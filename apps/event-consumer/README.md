# event-consumer (Quarkus)

Consume de una cola (por defecto `iot.events`) y expone un snapshot para el visualizer.

## API

- `GET /api/snapshot` → `{ received, duplicates, last[] }`

## Run (local)

```bash
mvn -pl apps/event-consumer quarkus:dev
```

Config principal en `application.properties` (`demo.broker.*`).

