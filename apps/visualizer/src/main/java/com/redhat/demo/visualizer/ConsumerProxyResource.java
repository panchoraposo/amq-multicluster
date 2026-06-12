package com.redhat.demo.visualizer;

import io.smallrye.config.ConfigMapping;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Path("/api/consumer")
@Produces(MediaType.APPLICATION_JSON)
public class ConsumerProxyResource {

  @ConfigMapping(prefix = "demo.consumers")
  public interface ConsumersConfig {
    String amq1SnapshotUrl();
    String amq2SnapshotUrl();
    String amq3SnapshotUrl();
  }

  private final HttpClient http = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(5))
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build();

  @Inject ConsumersConfig cfg;

  @GET
  @Path("/{site}/snapshot")
  public String snapshot(@jakarta.ws.rs.PathParam("site") String site, @QueryParam("mode") String mode) throws Exception {
    String url = switch (site) {
      case "amq1" -> cfg.amq1SnapshotUrl();
      case "amq2" -> cfg.amq2SnapshotUrl();
      case "amq3" -> cfg.amq3SnapshotUrl();
      default -> throw new NotFoundException("Unknown site: " + site);
    };
    if (url == null || url.isBlank()) {
      throw new NotFoundException("No consumer snapshot URL configured for " + site);
    }
    if (mode != null && !mode.isBlank()) {
      url = url.contains("?") ? (url + "&mode=" + mode) : (url + "?mode=" + mode);
    }

    HttpRequest req = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .timeout(Duration.ofSeconds(8))
      .header("Accept", "application/json")
      .GET()
      .build();
    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (resp.statusCode() >= 400) {
      throw new RuntimeException("consumer " + site + " HTTP " + resp.statusCode());
    }
    return resp.body();
  }
}

