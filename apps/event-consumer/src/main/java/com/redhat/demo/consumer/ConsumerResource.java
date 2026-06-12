package com.redhat.demo.consumer;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/api")
public class ConsumerResource {

  @Inject EventConsumerMain.ConsumerStates states;

  @GET
  @Path("/snapshot")
  @Produces(MediaType.APPLICATION_JSON)
  public EventConsumerMain.Snapshot snapshot(@QueryParam("mode") String mode) {
    return states.byMode(mode).snapshot();
  }

  @POST
  @Path("/reset")
  @Produces(MediaType.APPLICATION_JSON)
  public EventConsumerMain.Snapshot reset(@QueryParam("mode") String mode) {
    states.reset(mode);
    return states.byMode(mode).snapshot();
  }
}

