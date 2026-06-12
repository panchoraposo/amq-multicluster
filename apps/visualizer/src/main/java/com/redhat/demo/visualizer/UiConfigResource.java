package com.redhat.demo.visualizer;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/ui")
@Produces(MediaType.APPLICATION_JSON)
public class UiConfigResource {

  @ConfigMapping(prefix = "demo.ui")
  public interface UiConfig {
    @WithDefault("AMQ1") String amq1Label();
    @WithDefault("#2563eb") String amq1Color();
    @WithDefault("AMQ2") String amq2Label();
    @WithDefault("#16a34a") String amq2Color();
    @WithDefault("AMQ3") String amq3Label();
    @WithDefault("#a21caf") String amq3Color();
  }

  @ConfigMapping(prefix = "demo.links")
  public interface LinksConfig {
    @WithDefault("") String amq1BrokerConsoleUrl();
    @WithDefault("") String amq2BrokerConsoleUrl();
    @WithDefault("") String amq3BrokerConsoleUrl();
    @WithDefault("") String amq1ConsumerUrl();
    @WithDefault("") String amq2ConsumerUrl();
    @WithDefault("") String amq3ConsumerUrl();
    @WithDefault("") String amq1SimulatorUrl();
    @WithDefault("") String amq2SimulatorUrl();
    @WithDefault("") String amq3SimulatorUrl();
  }

  public record UiPayload(
    String amq1Label,
    String amq1Color,
    String amq2Label,
    String amq2Color,
    String amq3Label,
    String amq3Color,
    String amq1BrokerConsoleUrl,
    String amq2BrokerConsoleUrl,
    String amq3BrokerConsoleUrl,
    String amq1ConsumerUrl,
    String amq2ConsumerUrl,
    String amq3ConsumerUrl,
    String amq1SimulatorUrl,
    String amq2SimulatorUrl,
    String amq3SimulatorUrl
  ) {}

  @Inject UiConfig ui;
  @Inject LinksConfig links;

  @GET
  @Path("/config")
  public UiPayload config() {
    return new UiPayload(
      ui.amq1Label(),
      ui.amq1Color(),
      ui.amq2Label(),
      ui.amq2Color(),
      ui.amq3Label(),
      ui.amq3Color(),
      links.amq1BrokerConsoleUrl(),
      links.amq2BrokerConsoleUrl(),
      links.amq3BrokerConsoleUrl(),
      links.amq1ConsumerUrl(),
      links.amq2ConsumerUrl(),
      links.amq3ConsumerUrl(),
      links.amq1SimulatorUrl(),
      links.amq2SimulatorUrl(),
      links.amq3SimulatorUrl()
    );
  }
}

