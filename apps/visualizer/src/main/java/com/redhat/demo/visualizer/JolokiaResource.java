package com.redhat.demo.visualizer;

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
import java.util.Optional;

@Path("/api/jolokia")
@Produces(MediaType.APPLICATION_JSON)
public class JolokiaResource {

  @ConfigMapping(prefix = "demo.jolokia")
  public interface JolokiaConfig {
    /**
     * Base URL must include `/console/jolokia`, e.g.:
     * http://<route-host>/console/jolokia
     */
    Optional<String> amq1BaseUrl();
    Optional<String> amq2BaseUrl();
    Optional<String> amq3BaseUrl();

    @WithDefault("admin")
    String username();

    @WithDefault("admin")
    String password();
  }

  private final HttpClient http = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(5))
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build();

  @Inject JolokiaConfig cfg;

  @GET
  @Path("/{site}/version")
  public String version(@jakarta.ws.rs.PathParam("site") String site) throws Exception {
    return fetch(site, "/version");
  }

  @GET
  @Path("/{site}/uptime")
  public String uptime(@jakarta.ws.rs.PathParam("site") String site) throws Exception {
    // Generic JVM MBean. This is stable and doesn't depend on Artemis internals.
    return fetch(site, "/read/java.lang:type=Runtime/Uptime");
  }

  private String fetch(String site, String path) throws Exception {
    String base = switch (site) {
      case "amq1" -> cfg.amq1BaseUrl().orElse("");
      case "amq2" -> cfg.amq2BaseUrl().orElse("");
      case "amq3" -> cfg.amq3BaseUrl().orElse("");
      default -> throw new NotFoundException("Unknown site: " + site);
    };
    if (base == null || base.isBlank()) {
      throw new NotFoundException("No Jolokia baseUrl configured for " + site);
    }

    String basic = Base64.getEncoder().encodeToString((cfg.username() + ":" + cfg.password()).getBytes(StandardCharsets.UTF_8));
    HttpRequest req = HttpRequest.newBuilder()
      .uri(URI.create(base + path))
      .timeout(Duration.ofSeconds(8))
      .header("Authorization", "Basic " + basic)
      .header("Accept", "application/json")
      .GET()
      .build();
    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (resp.statusCode() >= 400) {
      throw new RuntimeException("Jolokia " + site + " HTTP " + resp.statusCode());
    }
    return resp.body();
  }
}

