package com.redhat.demo.visualizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * API compatible with hodrigohamalho/amq-broker-demo webapp:
 * - GET /api/config
 * - GET /api/state
 *
 * We keep the same JSON shape so the frontend can be ported with minimal diffs.
 */
@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class DemoStateResource {

  @ConfigMapping(prefix = "demo.ha")
  public interface HaConfig {
    /**
     * Local site (amq1/amq2/amq3). Used to derive the broker console services.
     */
    @WithDefault("amq1")
    String site();

    @WithDefault("amq-multicluster-amq")
    String namespace();

    /**
     * ActiveMQArtemis CR name, e.g. amq-amq1.
     * If not set, we derive it from site (amq-amq1, amq-amq2, amq-amq3).
     */
    @WithDefault("AUTO")
    String brokerCr();

    @WithDefault("admin")
    String username();

    @WithDefault("admin")
    String password();

    /**
     * Anycast queue name shown in Queue mode.
     * NOTE: In our demo, MQTT publishes to multicast addresses by default; Queue mode
     * is still useful to visualize point-to-point counters if you wire a real queue producer.
     */
    @WithDefault("iot.events.v3")
    String queue();

    /**
     * Artemis routing-type for the queue MBean. For MQTT-published addresses,
     * routing is multicast by default unless you use anycast prefixes.
     */
    @WithDefault("multicast")
    String queueRoutingType();

    /**
     * Multicast address/topic name shown in Topic mode.
     */
    @WithDefault("iot.events.v3")
    String topic();
  }

  public record BrokerCfg(int id, String name, String pod, String console) {}

  public record ConfigPayload(
    String queue,
    String topic,
    String namespace,
    String cr,
    List<BrokerCfg> brokers
  ) {}

  public record QueueStats(
    long messageCount,
    long consumerCount,
    long messagesAdded,
    long messagesAcknowledged,
    long deliveringCount,
    long routed
  ) {}

  public record TopicStats(
    long messageCount,
    long routedMessageCount,
    long subscriptions
  ) {}

  public record BrokerState(
    int id,
    String name,
    String pod,
    boolean online,
    boolean active,
    QueueStats queue,
    TopicStats topic
  ) {}

  public record StatePayload(
    String queue,
    String topic,
    boolean topicLive,
    List<BrokerState> brokers
  ) {}

  @Inject HaConfig cfg;
  @Inject ObjectMapper mapper;

  private final HttpClient http = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(4))
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build();

  @GET
  @Path("/config")
  public ConfigPayload config() {
    String site = cfg.site();
    String ns = cfg.namespace();
    String cr = (cfg.brokerCr() == null || cfg.brokerCr().isBlank() || "AUTO".equalsIgnoreCase(cfg.brokerCr()))
      ? ("amq-" + site)
      : cfg.brokerCr();

    // Internal per-pod console services exist for Operator-deployed brokers:
    // <cr>-wconsj-0-svc and <cr>-wconsj-1-svc (port 8161).
    String c0 = "http://" + cr + "-wconsj-0-svc." + ns + ".svc";
    String c1 = "http://" + cr + "-wconsj-1-svc." + ns + ".svc";

    return new ConfigPayload(
      cfg.queue(),
      cfg.topic(),
      ns,
      cr,
      List.of(
        new BrokerCfg(0, "Broker-0", cr + "-ss-0", c0),
        new BrokerCfg(1, "Broker-1", cr + "-ss-1", c1)
      )
    );
  }

  @GET
  @Path("/state")
  public StatePayload state() throws Exception {
    ConfigPayload c = config();
    List<BrokerState> brokers = c.brokers().stream().map(b -> {
      try {
        return readBroker(b, c.queue(), c.topic());
      } catch (Exception e) {
        return new BrokerState(b.id(), b.name(), b.pod(), false, false, null, null);
      }
    }).toList();

    boolean topicLive = brokers.stream().anyMatch(b -> b.topic() != null);
    return new StatePayload(c.queue(), c.topic(), topicLive, brokers);
  }

  private BrokerState readBroker(BrokerCfg b, String queue, String topic) throws Exception {
    String base = b.console().replaceAll("/+$", "");
    String url = base + ":8161/console/jolokia/";

    String auth = Base64.getEncoder().encodeToString((cfg.username() + ":" + cfg.password()).getBytes(StandardCharsets.UTF_8));

    String qmb = qMbean(queue, cfg.queueRoutingType());
    String tmb = addrMbean(topic);
    String qAddr = addrMbean(queue);

    // Jolokia batched POST (same approach as amq-broker-demo).
    List<Map<String, Object>> batch = List.of(
      Map.of("type", "read", "mbean", "org.apache.activemq.artemis:broker=\"amq-broker\"", "attribute", "Active"),
      Map.of("type", "read", "mbean", qmb, "attribute", List.of("MessageCount", "ConsumerCount", "MessagesAdded", "MessagesAcknowledged", "DeliveringCount")),
      Map.of("type", "read", "mbean", tmb, "attribute", List.of("MessageCount", "RoutedMessageCount", "QueueNames")),
      Map.of("type", "read", "mbean", qAddr, "attribute", List.of("RoutedMessageCount"))
    );

    HttpRequest req = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .timeout(Duration.ofSeconds(4))
      .header("Authorization", "Basic " + auth)
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(batch), StandardCharsets.UTF_8))
      .build();

    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (resp.statusCode() >= 400) {
      return new BrokerState(b.id(), b.name(), b.pod(), false, false, null, null);
    }

    JsonNode arr = mapper.readTree(resp.body());
    if (!arr.isArray() || arr.size() < 2) {
      return new BrokerState(b.id(), b.name(), b.pod(), false, false, null, null);
    }

    boolean active = arr.get(0).path("status").asInt() == 200 && arr.get(0).path("value").asBoolean(false);

    QueueStats q = null;
    if (arr.get(1).path("status").asInt() == 200 && !arr.get(1).path("value").isMissingNode()) {
      JsonNode v = arr.get(1).path("value");
      long routed = 0;
      if (arr.size() > 3 && arr.get(3).path("status").asInt() == 200) {
        routed = arr.get(3).path("value").path("RoutedMessageCount").asLong(0);
      }
      q = new QueueStats(
        v.path("MessageCount").asLong(0),
        v.path("ConsumerCount").asLong(0),
        v.path("MessagesAdded").asLong(0),
        v.path("MessagesAcknowledged").asLong(0),
        v.path("DeliveringCount").asLong(0),
        routed
      );
    }

    TopicStats t = null;
    if (arr.size() > 2 && arr.get(2).path("status").asInt() == 200 && !arr.get(2).path("value").isMissingNode()) {
      JsonNode v = arr.get(2).path("value");
      JsonNode qn = v.path("QueueNames");
      long subs = qn.isArray() ? qn.size() : 0;
      t = new TopicStats(
        v.path("MessageCount").asLong(0),
        v.path("RoutedMessageCount").asLong(0),
        subs
      );
    }

    return new BrokerState(b.id(), b.name(), b.pod(), true, active, q, t);
  }

  private static String qMbean(String name, String routingType) {
    // Mirror of amq-broker-demo: assumes broker name "amq-broker"
    return "org.apache.activemq.artemis:address=\"" + name + "\",broker=\"amq-broker\",component=addresses,queue=\"" + name + "\",routing-type=\"" + routingType + "\",subcomponent=queues";
  }

  private static String addrMbean(String addr) {
    return "org.apache.activemq.artemis:address=\"" + addr + "\",broker=\"amq-broker\",component=addresses";
  }
}

