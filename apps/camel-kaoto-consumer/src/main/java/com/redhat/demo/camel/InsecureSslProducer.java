package com.redhat.demo.camel;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.SSLContextClientParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

@ApplicationScoped
public class InsecureSslProducer {

  @ConfigMapping(prefix = "demo.mqtt")
  public interface MqttCfg {
    @WithDefault("true") boolean insecureTls();
  }

  @Produces
  @ApplicationScoped
  public SSLContextParameters insecureSsl(MqttCfg cfg) {
    SSLContextParameters p = new SSLContextParameters();
    if (!cfg.insecureTls()) {
      return p;
    }

    TrustManagersParameters tmp = new TrustManagersParameters();
    tmp.setTrustManager(new X509TrustManager() {
      @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
      @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
      @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
    });
    p.setTrustManagers(tmp);
    p.setKeyManagers(new KeyManagersParameters());
    p.setClientParameters(new SSLContextClientParameters());
    return p;
  }
}

