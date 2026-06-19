package com.redhat.demo.visualizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
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
import java.util.Optional;

@Path("/api/broker")
@Produces(MediaType.APPLICATION_JSON)
public class BrokerStatsResource {

  public record QueueStats(
    long messageCount,
    long consumerCount,
    long messagesAdded,
    long messagesAcknowledged,
    long deliveringCount,
    long routed
  ) {}

  public record BrokerStats(
    boolean online,
    boolean active,
    QueueStats queue
  ) {}

  @ConfigMapping(prefix = "demo.jolokia")
  public interface JolokiaCfg {
    Optional<String> amq1BaseUrl();
    Optional<String> amq2BaseUrl();
    Optional<String> amq3BaseUrl();
    @WithDefault("admin") String username();
    @WithDefault("admin") String password();
  }

  @ConfigMapping(prefix = "demo.queue")
  public interface QueueCfg {
    @WithDefault("queue.demo") String name();
  }

  @Inject JolokiaCfg jolokia;
  @Inject QueueCfg queueCfg;
  @Inject ObjectMapper mapper;

  private final HttpClient http = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(4))
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build();

  @GET
  @Path("/{site}/queue")
  public BrokerStats queue(@jakarta.ws.rs.PathParam("site") String site) throws Exception {
    String base = switch (site) {
      case "amq1" -> jolokia.amq1BaseUrl().orElse("");
      case "amq2" -> jolokia.amq2BaseUrl().orElse("");
      case "amq3" -> jolokia.amq3BaseUrl().orElse("");
      default -> throw new NotFoundException("Unknown site: " + site);
    };
    if (base == null || base.isBlank()) {
      throw new NotFoundException("No Jolokia base URL configured for " + site);
    }

    String url = base.replaceAll("/+$", "") + "/";
    String auth = Base64.getEncoder().encodeToString((jolokia.username() + ":" + jolokia.password()).getBytes(StandardCharsets.UTF_8));

    String q = queueCfg.name();
    String qmb = qMbean(q, "anycast");
    String qAddr = addrMbean(q);

    List<Map<String, Object>> batch = List.of(
      Map.of("type", "read", "mbean", "org.apache.activemq.artemis:broker=\"amq-broker\"", "attribute", "Active"),
      Map.of("type", "read", "mbean", qmb, "attribute", List.of("MessageCount", "ConsumerCount", "MessagesAdded", "MessagesAcknowledged", "DeliveringCount")),
      Map.of("type", "read", "mbean", qAddr, "attribute", List.of("RoutedMessageCount"))
    );

    HttpRequest req = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .timeout(Duration.ofSeconds(5))
      .header("Authorization", "Basic " + auth)
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(batch), StandardCharsets.UTF_8))
      .build();

    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (resp.statusCode() >= 400) {
      return new BrokerStats(false, false, null);
    }

    JsonNode arr = mapper.readTree(resp.body());
    if (!arr.isArray() || arr.size() < 2) {
      return new BrokerStats(false, false, null);
    }

    boolean active = arr.get(0).path("status").asInt() == 200 && arr.get(0).path("value").asBoolean(false);

    QueueStats qs = null;
    if (arr.get(1).path("status").asInt() == 200 && !arr.get(1).path("value").isMissingNode()) {
      JsonNode v = arr.get(1).path("value");
      long routed = 0;
      if (arr.size() > 2 && arr.get(2).path("status").asInt() == 200) {
        routed = arr.get(2).path("value").path("RoutedMessageCount").asLong(0);
      }
      qs = new QueueStats(
        v.path("MessageCount").asLong(0),
        v.path("ConsumerCount").asLong(0),
        v.path("MessagesAdded").asLong(0),
        v.path("MessagesAcknowledged").asLong(0),
        v.path("DeliveringCount").asLong(0),
        routed
      );
    }
    return new BrokerStats(true, active, qs);
  }

  @GET
  @Path("/{site}/health")
  public BrokerStats health(@jakarta.ws.rs.PathParam("site") String site) throws Exception {
    String base = switch (site) {
      case "amq1" -> jolokia.amq1BaseUrl().orElse("");
      case "amq2" -> jolokia.amq2BaseUrl().orElse("");
      case "amq3" -> jolokia.amq3BaseUrl().orElse("");
      default -> throw new NotFoundException("Unknown site: " + site);
    };
    if (base == null || base.isBlank()) {
      throw new NotFoundException("No Jolokia base URL configured for " + site);
    }

    String url = base.replaceAll("/+$", "") + "/";
    String auth = Base64.getEncoder().encodeToString((jolokia.username() + ":" + jolokia.password()).getBytes(StandardCharsets.UTF_8));

    List<Map<String, Object>> batch = List.of(
      Map.of("type", "read", "mbean", "org.apache.activemq.artemis:broker=\"amq-broker\"", "attribute", "Active")
    );

    HttpRequest req = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .timeout(Duration.ofSeconds(5))
      .header("Authorization", "Basic " + auth)
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(batch), StandardCharsets.UTF_8))
      .build();

    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (resp.statusCode() >= 400) {
      return new BrokerStats(false, false, null);
    }

    JsonNode arr = mapper.readTree(resp.body());
    if (!arr.isArray() || arr.isEmpty()) {
      return new BrokerStats(false, false, null);
    }
    boolean active = arr.get(0).path("status").asInt() == 200 && arr.get(0).path("value").asBoolean(false);
    return new BrokerStats(true, active, null);
  }

  private static String qMbean(String name, String routingType) {
    return "org.apache.activemq.artemis:address=\"" + name + "\",broker=\"amq-broker\",component=addresses,queue=\"" + name + "\",routing-type=\"" + routingType + "\",subcomponent=queues";
  }

  private static String addrMbean(String addr) {
    return "org.apache.activemq.artemis:address=\"" + addr + "\",broker=\"amq-broker\",component=addresses";
  }
}

