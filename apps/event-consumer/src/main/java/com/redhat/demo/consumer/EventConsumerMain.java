package com.redhat.demo.consumer;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.jms.BytesMessage;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@QuarkusMain
public class EventConsumerMain implements QuarkusApplication {

  @ConfigMapping(prefix = "demo.mqtt")
  public interface MqttConfig {
    @WithDefault("") String host();
    @WithDefault("8883") int port();
    @WithDefault("iot.events.v3") String topic();
    @WithDefault("true") boolean insecureTls();
    @WithDefault("amq1") String site();
  }

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
  @Inject MqttConfig mqtt;
  @Inject ConsumerState state;

  @Override
  public int run(String... args) throws Exception {
    if (mqtt.host() != null && !mqtt.host().isBlank()) {
      System.out.println("Consumer starting MQTT site=" + mqtt.site() + " topic=" + mqtt.topic() + " host=" + mqtt.host() + ":" + mqtt.port());
      startMqtt();
      Thread.currentThread().join();
      return 0;
    }

    // Legacy JMS mode (kept as fallback)
    String coreUrl = cfg.url();
    System.out.println("Consumer starting JMS site=" + cfg.site() + " queue=" + cfg.queue() + " url=" + coreUrl);

    vertx.executeBlocking(promise -> {
      long backoffMs = 1000;
      while (true) {
        try {
          // NOTE: JMS mode currently disabled for multicluster MQTT demos.
          Thread.sleep(30000);
        } catch (Exception e) {
          System.err.println("Consume error (will reconnect): " + e.getMessage());
          try {
            Thread.sleep(backoffMs);
          } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
          }
          backoffMs = Math.min(backoffMs * 2, 30000);
        }
      }
    }, false);

    Thread.currentThread().join();
    return 0;
  }

  private void startMqtt() throws Exception {
    String brokerUrl = "ssl://" + mqtt.host() + ":" + mqtt.port();
    String clientId = "event-consumer-" + mqtt.site() + "-" + System.getenv().getOrDefault("HOSTNAME", "pod");

    MqttConnectOptions opts = new MqttConnectOptions();
    opts.setAutomaticReconnect(true);
    opts.setCleanSession(true);
    opts.setConnectionTimeout(10);
    opts.setKeepAliveInterval(30);
    if (mqtt.insecureTls()) {
      opts.setSocketFactory(trustAllSocketFactory());
      opts.setHttpsHostnameVerificationEnabled(false);
    }

    MqttClient client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
    client.setCallback(new MqttCallbackExtended() {
      @Override
      public void connectComplete(boolean reconnect, String serverURI) {
        System.out.println("MQTT connected reconnect=" + reconnect + " uri=" + serverURI + " topic=" + mqtt.topic());
        try {
          client.subscribe(mqtt.topic(), 0);
        } catch (Exception e) {
          System.err.println("MQTT subscribe error: " + e.getMessage());
        }
      }

      @Override
      public void connectionLost(Throwable cause) {
        System.err.println("MQTT connection lost: " + (cause != null ? cause.getMessage() : "null"));
      }

      @Override
      public void messageArrived(String topic, MqttMessage message) throws Exception {
        String body = new String(message.getPayload(), StandardCharsets.UTF_8);
        state.onPayload(body);
      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken token) {
        // consumer only
      }
    });

    client.connect(opts);
  }

  private static SSLSocketFactory trustAllSocketFactory() throws Exception {
    TrustManager[] trustAll = new TrustManager[] {
      new X509TrustManager() {
        @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
        @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
        @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
      }
    };
    SSLContext ctx = SSLContext.getInstance("TLS");
    ctx.init(null, trustAll, new SecureRandom());
    SSLSocketFactory delegate = ctx.getSocketFactory();
    // Disable hostname verification by clearing endpoint identification algorithm.
    return new SSLSocketFactory() {
      @Override public String[] getDefaultCipherSuites() { return delegate.getDefaultCipherSuites(); }
      @Override public String[] getSupportedCipherSuites() { return delegate.getSupportedCipherSuites(); }

      private java.net.Socket tweak(java.net.Socket s) {
        if (s instanceof SSLSocket ssl) {
          SSLParameters p = ssl.getSSLParameters();
          p.setEndpointIdentificationAlgorithm(null);
          ssl.setSSLParameters(p);
        }
        return s;
      }

      @Override public java.net.Socket createSocket() throws java.io.IOException {
        return tweak(delegate.createSocket());
      }
      @Override public java.net.Socket createSocket(java.net.Socket s, String host, int port, boolean autoClose) throws java.io.IOException {
        return tweak(delegate.createSocket(s, host, port, autoClose));
      }
      @Override public java.net.Socket createSocket(String host, int port) throws java.io.IOException {
        return tweak(delegate.createSocket(host, port));
      }
      @Override public java.net.Socket createSocket(String host, int port, java.net.InetAddress localHost, int localPort) throws java.io.IOException {
        return tweak(delegate.createSocket(host, port, localHost, localPort));
      }
      @Override public java.net.Socket createSocket(java.net.InetAddress host, int port) throws java.io.IOException {
        return tweak(delegate.createSocket(host, port));
      }
      @Override public java.net.Socket createSocket(java.net.InetAddress address, int port, java.net.InetAddress localAddress, int localPort) throws java.io.IOException {
        return tweak(delegate.createSocket(address, port, localAddress, localPort));
      }
    };
  }

  @ApplicationScoped
  public static class ConsumerState {
    private final AtomicLong received = new AtomicLong();
    private final AtomicLong duplicates = new AtomicLong();
    private final Map<String, Boolean> seen = new ConcurrentHashMap<>();
    private final Deque<EventSample> last = new ArrayDeque<>();

    public void onPayload(String body) throws Exception {
      received.incrementAndGet();

      String key = null;
      if (body != null) {
        int i = body.indexOf("\"eventId\":\"");
        if (i >= 0) {
          int start = i + "\"eventId\":\"".length();
          int end = body.indexOf('"', start);
          if (end > start) {
            key = body.substring(start, end);
          }
        }
      }
      if (key == null) {
        key = body;
      }
      if (key != null) {
        if (seen.putIfAbsent(key, Boolean.TRUE) != null) {
          duplicates.incrementAndGet();
        }
      }

      synchronized (last) {
        last.addFirst(new EventSample(Instant.now().toString(), null, body));
        while (last.size() > 20) {
          last.removeLast();
        }
      }
    }

    public void onMessage(Message msg) throws Exception {
      received.incrementAndGet();

      String body = null;
      if (msg instanceof TextMessage tm) {
        body = tm.getText();
      } else if (msg instanceof BytesMessage bm) {
        long len = bm.getBodyLength();
        if (len > 0 && len < Integer.MAX_VALUE) {
          byte[] data = new byte[(int) len];
          bm.readBytes(data);
          body = new String(data, StandardCharsets.UTF_8);
        }
      }

      String key = msg.getJMSMessageID();
      if (body != null) {
        int i = body.indexOf("\"eventId\":\"");
        if (i >= 0) {
          int start = i + "\"eventId\":\"".length();
          int end = body.indexOf('"', start);
          if (end > start) {
            key = body.substring(start, end);
          }
        }
      }
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

