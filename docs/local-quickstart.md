# Quickstart local (Podman)

Este quickstart permite correr la demo **sin OpenShift**, usando contenedores locales con Podman.

## Requisitos

- Podman
- Java 21+ (para Quarkus 3.33)
- Maven 3.9+

## Flujo local recomendado

- Levantar 3 brokers locales (simulan `amq1`, `amq2`, `amq3`) con listeners:
  - MQTT (dev): `1883` (sin TLS) o `8883` (TLS si habilitas secrets/certs)
  - AMQP: `5672`
  - Console/Jolokia: `8161`
- Correr el simulador de dispositivos apuntando a cada sitio por MQTT.
- Correr consumidores (opcional) y el visualizer.

## Visualizer (sin clusters)

El visualizer puede correr en modo sintético (sin consumers):

```bash
mvn -pl apps/visualizer quarkus:dev
```

Abrir `http://localhost:8080`.

> La configuración exacta de compose/podman para levantar 3 brokers locales se completará en un siguiente paso; por ahora el foco es la demo OpenShift multicluster.

