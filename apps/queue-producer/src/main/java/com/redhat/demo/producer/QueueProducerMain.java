package com.redhat.demo.producer;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Random;

@QuarkusMain
public class QueueProducerMain implements QuarkusApplication {

  @ConfigMapping(prefix = "demo.broker")
  public interface BrokerConfig {
    String url();
    @WithDefault("admin") String username();
    @WithDefault("admin") String password();
  }

  @ConfigMapping(prefix = "demo.queue")
  public interface QueueConfig {
    @WithDefault("queue.demo") String name();
    @WithDefault("1000") long intervalMs();
    @WithDefault("public-cloud") String site();
  }

  @jakarta.inject.Inject BrokerConfig broker;
  @jakarta.inject.Inject QueueConfig queue;

  private final Random rnd = new Random();

  @Override
  public int run(String... args) throws Exception {
    String pod = System.getenv().getOrDefault("HOSTNAME", "queue-producer");
    long backoffMs = 1000;

    while (true) {
      ServerLocator locator = null;
      ClientSessionFactory sf = null;
      ClientSession session = null;
      ClientProducer producer = null;
      try {
        locator = ActiveMQClient.createServerLocator(broker.url());
        sf = locator.createSessionFactory();
        session = sf.createSession(broker.username(), broker.password(), false, true, true, locator.isHA(), 1);
        producer = session.createProducer(queue.name());
        session.start();

        long seq = 0;
        while (true) {
          seq++;
          String deviceId = "qdev-" + (1 + rnd.nextInt(20));
          String sensorType = switch (rnd.nextInt(4)) {
            case 0 -> "temp";
            case 1 -> "humidity";
            case 2 -> "vibration";
            default -> "pressure";
          };
          double value = switch (sensorType) {
            case "temp" -> 18 + rnd.nextDouble() * 17;
            case "humidity" -> 25 + rnd.nextDouble() * 60;
            case "vibration" -> rnd.nextDouble() * 2.5;
            default -> 0.8 + rnd.nextDouble() * 0.6;
          };
          String unit = switch (sensorType) {
            case "temp" -> "C";
            case "humidity" -> "%";
            case "vibration" -> "g";
            default -> "bar";
          };

          String eventId = Long.toString(System.currentTimeMillis(), 36) + "-" + pod + "-" + Long.toString(seq, 36);
          String json = """
            {"schemaVersion":1,"mode":"queue","site":"%s","producer":"%s","eventId":"%s","deviceId":"%s","seq":%d,"ts":"%s","sensor":{"type":"%s","reading":{"value":%.2f,"unit":"%s"},"batteryPct":%d,"status":"OK"}}
            """.formatted(queue.site(), pod, eventId, deviceId, seq, Instant.now().toString(), sensorType, value, unit, 60 + rnd.nextInt(40)).trim();

          ClientMessage msg = session.createMessage(true);
          msg.getBodyBuffer().writeBytes(json.getBytes(StandardCharsets.UTF_8));
          producer.send(msg);

          Thread.sleep(Math.max(50, queue.intervalMs()));
        }
      } catch (Exception e) {
        System.err.println("Queue-producer error (will reconnect): " + e.getMessage());
        Thread.sleep(backoffMs);
        backoffMs = Math.min(backoffMs * 2, 30000);
      } finally {
        try { if (producer != null) producer.close(); } catch (Exception ignored) {}
        try { if (session != null) session.close(); } catch (Exception ignored) {}
        try { if (sf != null) sf.close(); } catch (Exception ignored) {}
        try { if (locator != null) locator.close(); } catch (Exception ignored) {}
      }
    }
  }
}

