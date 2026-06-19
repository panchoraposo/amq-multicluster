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

## Visualizer (local mode)

The `visualizer` frontend always renders **3 sites** (`amq1/amq2/amq3`). For local dev, you can run **3 `event-consumer` instances**
on different ports (all pointing to the same local broker) and wire the snapshot URLs into the visualizer config.

## Local broker (Podman)

This uses a public UBI-based Artemis image that exposes MQTT (`1883`), CORE (`61616`) and the console (`8161`).

```bash
podman run --rm -d --name amq-local \
  -e AMQ_USER=admin -e AMQ_PASSWORD=admin \
  -p 1883:1883 -p 61616:61616 -p 8161:8161 \
  quay.io/arkmq-org/activemq-artemis-broker:dev.latest
```

Open the console at `http://localhost:8161/console` (user/pass: `admin` / `admin`).

## event-consumer (Quarkus dev mode) ×3

In three terminals, start one consumer per site (ports `8081`, `8082`, `8083`).
Note: `sensors/#` must be URL-encoded as `sensors/%23` for some MQTT clients/libraries.

```bash
# terminal 1
DEMO_BROKER_URL='tcp://localhost:61616' DEMO_BROKER_USERNAME=admin DEMO_BROKER_PASSWORD=admin \
DEMO_BROKER_SITE=amq1 DEMO_MQTT_SITE=amq1 DEMO_MQTT_HOST=localhost DEMO_MQTT_PORT=1883 DEMO_MQTT_TOPIC='sensors/#' \
mvn -pl apps/event-consumer quarkus:dev -Dquarkus.http.port=8081

# terminal 2
DEMO_BROKER_URL='tcp://localhost:61616' DEMO_BROKER_USERNAME=admin DEMO_BROKER_PASSWORD=admin \
DEMO_BROKER_SITE=amq2 DEMO_MQTT_SITE=amq2 DEMO_MQTT_HOST=localhost DEMO_MQTT_PORT=1883 DEMO_MQTT_TOPIC='sensors/#' \
mvn -pl apps/event-consumer quarkus:dev -Dquarkus.http.port=8082

# terminal 3
DEMO_BROKER_URL='tcp://localhost:61616' DEMO_BROKER_USERNAME=admin DEMO_BROKER_PASSWORD=admin \
DEMO_BROKER_SITE=amq3 DEMO_MQTT_SITE=amq3 DEMO_MQTT_HOST=localhost DEMO_MQTT_PORT=1883 DEMO_MQTT_TOPIC='sensors/#' \
mvn -pl apps/event-consumer quarkus:dev -Dquarkus.http.port=8083
```

## visualizer (Quarkus dev mode)

In a 4th terminal:

```bash
DEMO_CONSUMERS_AMQ1_SNAPSHOT_URL='http://localhost:8081/api/snapshot' \
DEMO_CONSUMERS_AMQ2_SNAPSHOT_URL='http://localhost:8082/api/snapshot' \
DEMO_CONSUMERS_AMQ3_SNAPSHOT_URL='http://localhost:8083/api/snapshot' \
DEMO_JOLOKIA_AMQ1_BASE_URL='http://localhost:8161/console/jolokia' \
DEMO_JOLOKIA_AMQ2_BASE_URL='http://localhost:8161/console/jolokia' \
DEMO_JOLOKIA_AMQ3_BASE_URL='http://localhost:8161/console/jolokia' \
mvn -pl apps/visualizer quarkus:dev
```

Open `http://localhost:8080`.

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
DEMO_MQTT_TOPIC='sensors/%23' \
mvn -pl apps/camel-kaoto-consumer quarkus:dev
```

## Send test data (MQTT)

```bash
podman run --rm --network host docker.io/library/eclipse-mosquitto:2.0 \
  mosquitto_pub -h localhost -p 1883 -u admin -P admin \
  -t 'sensors/local/device/dev-1/temp/telemetry' \
  -m '{\"eventId\":\"local-1\",\"ts\":\"2026-06-19T00:00:00Z\",\"site\":\"local\",\"deviceId\":\"dev-1\",\"sensor\":{\"type\":\"temp\",\"reading\":{\"value\":42.0,\"unit\":\"C\"}},\"location\":{\"factory\":\"a\",\"line\":\"1\"}}'
```


