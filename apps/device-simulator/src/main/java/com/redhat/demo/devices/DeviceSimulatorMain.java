package com.redhat.demo.devices;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.vertx.core.Vertx;
import jakarta.inject.Inject;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

@QuarkusMain
public class DeviceSimulatorMain implements QuarkusApplication {

  @ConfigMapping(prefix = "demo.mqtt")
  public interface MqttConfig {
    String host();

    @WithDefault("8883")
    int port();

    @WithDefault("true")
    boolean tls();

    @WithDefault("admin")
    String username();

    @WithDefault("admin")
    String password();

    @WithDefault("amqdemo")
    String clientIdPrefix();

    @WithDefault("amq1")
    String site();

    @WithDefault("10")
    int deviceCount();

    @WithDefault("250")
    long intervalMs();

    @WithDefault("0")
    int qos();

    /**
     * Optional when using TLS: path to JKS truststore containing broker cert.
     */
    String trustStorePath();

    String trustStorePassword();

    /**
     * Optional when using mTLS: path to JKS keystore with client cert.
     */
    String keyStorePath();

    String keyStorePassword();

    @WithDefault("false")
    boolean trustAll();
  }

  @Inject Vertx vertx;
  @Inject MqttConfig cfg;

  @Override
  public int run(String... args) throws Exception {
    var brokerUrl = (cfg.tls() ? "ssl://" : "tcp://") + cfg.host() + ":" + cfg.port();
    var clientId = cfg.clientIdPrefix() + "-" + cfg.site() + "-" + UUID.randomUUID();

    MqttClient client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
    MqttConnectOptions opts = new MqttConnectOptions();
    opts.setUserName(cfg.username());
    opts.setPassword(cfg.password().toCharArray());
    opts.setAutomaticReconnect(true);
    opts.setCleanSession(true);
    opts.setConnectionTimeout(10);
    if (cfg.tls()) {
      opts.setSocketFactory(buildSslContext(cfg).getSocketFactory());
    }

    connect(client, opts);

    long[] seq = new long[cfg.deviceCount()];

    vertx.setPeriodic(cfg.intervalMs(), id -> {
      int deviceIdx = ThreadLocalRandom.current().nextInt(cfg.deviceCount());
      long next = ++seq[deviceIdx];
      String deviceId = "device-" + (deviceIdx + 1);
      String topic = "iot/events/v2";
      String eventId = UUID.randomUUID().toString();
      String payload = "{\"site\":\"" + cfg.site() + "\"," +
        "\"eventId\":\"" + eventId + "\"," +
        "\"deviceId\":\"" + deviceId + "\"," +
        "\"seq\":" + next + "," +
        "\"ts\":\"" + Instant.now() + "\"}";

      MqttMessage msg = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
      msg.setQos(cfg.qos());
      try {
        client.publish(topic, msg);
      } catch (Exception e) {
        System.err.println("Publish failed: " + e.getMessage());
      }
    });

    // keep running
    Thread.currentThread().join();
    return 0;
  }

  private static void connect(MqttClient client, MqttConnectOptions opts) throws MqttException {
    client.connect(opts);
    System.out.println("Connected MQTT clientId=" + client.getClientId() + " to " + client.getServerURI());
  }

  private static SSLContext buildSslContext(MqttConfig cfg) throws Exception {
    if (cfg.trustAll()) {
      TrustManager[] trustAll = new TrustManager[] {
        new X509TrustManager() {
          @Override
          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[0];
          }

          @Override
          public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
          }

          @Override
          public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
          }
        }
      };

      SSLContext ctx = SSLContext.getInstance("TLS");
      ctx.init(null, trustAll, new SecureRandom());
      return ctx;
    }

    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    KeyStore trustStore = KeyStore.getInstance("JKS");
    try (FileInputStream in = new FileInputStream(cfg.trustStorePath())) {
      trustStore.load(in, cfg.trustStorePassword().toCharArray());
    }
    tmf.init(trustStore);

    KeyManagerFactory kmf = null;
    if (cfg.keyStorePath() != null && !cfg.keyStorePath().isBlank()) {
      kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      KeyStore keyStore = KeyStore.getInstance("JKS");
      try (FileInputStream in = new FileInputStream(cfg.keyStorePath())) {
        keyStore.load(in, cfg.keyStorePassword().toCharArray());
      }
      kmf.init(keyStore, cfg.keyStorePassword().toCharArray());
    }

    SSLContext ctx = SSLContext.getInstance("TLS");
    ctx.init(kmf == null ? null : kmf.getKeyManagers(), tmf.getTrustManagers(), null);
    return ctx;
  }
}

