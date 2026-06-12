package com.redhat.demo.visualizer;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/")
public class IndexResource {

  @ConfigMapping(prefix = "demo.ui")
  public interface UiConfig {
    @WithDefault("AMQ1")
    String amq1Label();

    @WithDefault("#2563eb")
    String amq1Color();

    @WithDefault("AMQ2")
    String amq2Label();

    @WithDefault("#16a34a")
    String amq2Color();

    @WithDefault("AMQ3")
    String amq3Label();

    @WithDefault("#a21caf")
    String amq3Color();
  }

  @ConfigMapping(prefix = "demo.consumers")
  public interface ConsumersConfig {
    /**
     * URL to the event-consumer `/api/snapshot` endpoint for each site.
     * If empty, the UI can run in DEMO_DATA mode.
     */
    String amq1SnapshotUrl();
    String amq2SnapshotUrl();
    String amq3SnapshotUrl();
  }

  @Inject Template index;
  @Inject UiConfig cfg;
  @Inject ConsumersConfig consumers;

  @GET
  public TemplateInstance home() {
    return index
      .data("amq1Label", cfg.amq1Label())
      .data("amq1Color", cfg.amq1Color())
      .data("amq2Label", cfg.amq2Label())
      .data("amq2Color", cfg.amq2Color())
      .data("amq3Label", cfg.amq3Label())
      .data("amq3Color", cfg.amq3Color())
      .data("amq1SnapshotUrl", consumers.amq1SnapshotUrl())
      .data("amq2SnapshotUrl", consumers.amq2SnapshotUrl())
      .data("amq3SnapshotUrl", consumers.amq3SnapshotUrl());
  }
}

