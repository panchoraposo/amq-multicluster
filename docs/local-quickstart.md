# Local quickstart (Podman)

This quickstart runs the demo **without OpenShift**, using local containers with Podman.

## Requirements

- Podman
- Java 21+ (Quarkus 3.33)
- Maven 3.9+

## Recommended local flow (Quarkus dev + Podman)

This quickstart is designed to let you run **the apps in Quarkus dev mode** while Podman provides
the supporting containers.

- Run a local Artemis broker container (MQTT + CORE + console).
- Run `event-consumer` and `visualizer` via `mvn quarkus:dev`.
- Optionally run `camel-kaoto-consumer` with **Dev Services Postgres**.

## Visualizer (no clusters)

Run the visualizer in synthetic mode (no consumers required):

```bash
mvn -pl apps/visualizer quarkus:dev
```

Open `http://localhost:8080`.

## Local broker (Podman)

This uses a public UBI-based Artemis image that exposes MQTT (`1883`), CORE (`61616`) and the console (`8161`).

```bash
podman run --rm -d --name amq-local \
  -e AMQ_USER=admin -e AMQ_PASSWORD=admin \
  -p 1883:1883 -p 61616:61616 -p 8161:8161 \
  quay.io/arkmq-org/activemq-artemis-broker:dev.latest
```

Open the console at `http://localhost:8161/console` (user/pass: `admin` / `admin`).

## event-consumer (Quarkus dev mode)

```bash
DEMO_BROKER_URL='tcp://localhost:61616' \
DEMO_BROKER_USERNAME=admin \
DEMO_BROKER_PASSWORD=admin \
DEMO_BROKER_SITE=local \
DEMO_MQTT_HOST=localhost \
DEMO_MQTT_PORT=1883 \
DEMO_MQTT_TOPIC='sensors/#' \
mvn -pl apps/event-consumer quarkus:dev
```

## camel-kaoto-consumer + Dev Services Postgres (optional)

If you want the Analytics pipeline locally, run the Camel app in dev mode and let Quarkus start Postgres via Dev Services
(works with Podman as the container runtime):

```bash
QUARKUS_DATASOURCE_JDBC_URL= \
QUARKUS_DATASOURCE_USERNAME= \
QUARKUS_DATASOURCE_PASSWORD= \
QUARKUS_DATASOURCE_DEVSERVICES_ENABLED=true \
QUARKUS_DATASOURCE_DEVSERVICES_IMAGE_NAME='registry.access.redhat.com/rhel9/postgresql-15:latest' \
DEMO_MQTT_HOST=localhost \
DEMO_MQTT_PORT=1883 \
DEMO_MQTT_TOPIC='sensors/#' \
mvn -pl apps/camel-kaoto-consumer quarkus:dev
```


