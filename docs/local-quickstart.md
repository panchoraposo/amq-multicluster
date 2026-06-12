# Local quickstart (Podman)

This quickstart runs the demo **without OpenShift**, using local containers with Podman.

## Requirements

- Podman
- Java 21+ (Quarkus 3.33)
- Maven 3.9+

## Recommended local flow

- Start 3 local brokers (simulate `amq1`, `amq2`, `amq3`) exposing:
  - MQTT (dev): `1883` (no TLS) or `8883` (TLS if you provide certs)
  - AMQP: `5672`
  - Console/Jolokia: `8161`
- Run the device simulator pointing to each site via MQTT.
- Run the consumers (optional) and the visualizer.

## Visualizer (no clusters)

Run the visualizer in synthetic mode (no consumers required):

```bash
mvn -pl apps/visualizer quarkus:dev
```

Open `http://localhost:8080`.

> The exact compose/podman setup for three local brokers will be added later; the current focus is the multi-cluster OpenShift demo.

