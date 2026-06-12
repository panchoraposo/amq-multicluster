package com.redhat.demo.consumer;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@QuarkusMain
public class EventConsumerMain implements QuarkusApplication {

  @ConfigMapping(prefix = "demo.broker")
  public interface BrokerConfig {
    String url();

    @WithDefault("admin")
    String username();

    @WithDefault("admin")
    String password();

    @WithDefault("iot.events")
    String queue();

    @WithDefault("amq1")
    String site();
  }

  @Inject Vertx vertx;
  @Inject BrokerConfig cfg;
  @Inject ConsumerState state;

  @Override
  public int run(String... args) throws Exception {
    String coreUrl = "tcp://" + cfg.url();
    ConnectionFactory cf = new ActiveMQJMSConnectionFactory(coreUrl, cfg.username(), cfg.password());
    Connection conn = cf.createConnection();
    conn.start();

    Session session = conn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
    MessageConsumer consumer = session.createConsumer(session.createQueue(cfg.queue()));

    System.out.println("Consumer site=" + cfg.site() + " queue=" + cfg.queue() + " url=" + coreUrl);

    vertx.executeBlocking(promise -> {
      while (true) {
        try {
          Message msg = consumer.receive(1000);
          if (msg == null) {
            continue;
          }
          state.onMessage(msg);
          msg.acknowledge();
        } catch (Exception e) {
          System.err.println("Consume error: " + e.getMessage());
        }
      }
    }, false);

    Thread.currentThread().join();
    return 0;
  }

  @ApplicationScoped
  public static class ConsumerState {
    private final AtomicLong received = new AtomicLong();
    private final AtomicLong duplicates = new AtomicLong();
    private final Map<String, Boolean> seen = new ConcurrentHashMap<>();
    private final Deque<EventSample> last = new ArrayDeque<>();

    public void onMessage(Message msg) throws Exception {
      received.incrementAndGet();

      String body = null;
      if (msg instanceof TextMessage tm) {
        body = tm.getText();
      }

      String key = msg.getJMSMessageID();
      if (key == null && body != null) {
        key = body;
      }
      if (key != null) {
        if (seen.putIfAbsent(key, Boolean.TRUE) != null) {
          duplicates.incrementAndGet();
        }
      }

      synchronized (last) {
        last.addFirst(new EventSample(Instant.now().toString(), msg.getJMSMessageID(), body));
        while (last.size() > 20) {
          last.removeLast();
        }
      }
    }

    public Snapshot snapshot() {
      synchronized (last) {
        return new Snapshot(received.get(), duplicates.get(), last.toArray(EventSample[]::new));
      }
    }
  }

  public record EventSample(String receivedAt, String jmsMessageId, String body) {}

  public record Snapshot(long received, long duplicates, EventSample[] last) {}
}

