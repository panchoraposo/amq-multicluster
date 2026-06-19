package com.redhat.demo.visualizer;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Optional;

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
    Optional<String> amq1BrokerConsoleUrl();
    Optional<String> amq2BrokerConsoleUrl();
    Optional<String> amq3BrokerConsoleUrl();
    Optional<String> amq1ConsumerUrl();
    Optional<String> amq2ConsumerUrl();
    Optional<String> amq3ConsumerUrl();
    Optional<String> amq1SimulatorUrl();
    Optional<String> amq2SimulatorUrl();
    Optional<String> amq3SimulatorUrl();
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
      links.amq1BrokerConsoleUrl().orElse(""),
      links.amq2BrokerConsoleUrl().orElse(""),
      links.amq3BrokerConsoleUrl().orElse(""),
      links.amq1ConsumerUrl().orElse(""),
      links.amq2ConsumerUrl().orElse(""),
      links.amq3ConsumerUrl().orElse(""),
      links.amq1SimulatorUrl().orElse(""),
      links.amq2SimulatorUrl().orElse(""),
      links.amq3SimulatorUrl().orElse("")
    );
  }
}

