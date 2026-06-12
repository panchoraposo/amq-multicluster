package com.redhat.demo.consumer;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api")
public class ConsumerResource {

  @Inject EventConsumerMain.ConsumerState state;

  @GET
  @Path("/snapshot")
  @Produces(MediaType.APPLICATION_JSON)
  public EventConsumerMain.Snapshot snapshot() {
    return state.snapshot();
  }

  @POST
  @Path("/reset")
  @Produces(MediaType.APPLICATION_JSON)
  public EventConsumerMain.Snapshot reset() {
    state.reset();
    return state.snapshot();
  }
}

