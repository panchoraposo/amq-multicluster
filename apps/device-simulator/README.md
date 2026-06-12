# device-simulator (Quarkus)

Simula dispositivos publicando eventos por MQTT.

## Run (local)

```bash
mvn -pl apps/device-simulator quarkus:dev \
  -Dquarkus.args=""
```

Config principal en `application.properties` (`demo.mqtt.*`).

