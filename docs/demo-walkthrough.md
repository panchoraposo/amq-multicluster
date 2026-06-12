# Demo walkthrough (3 clusters OpenShift)

Guía de alto nivel para correr la demo en 3 clusters (contexts `oc`: `amq1`, `amq2`, `amq3`):

- `amq1`: broker + ingest MQTT/TLS
- `amq2`: broker + ingest MQTT/TLS
- `amq3`: broker + ingest MQTT/TLS

La conectividad entre clusters se realiza con **Service Interconnect / Skupper** para exponer endpoints AMQP entre sitios.

## Pasos (resumen)

1. Instalar AMQ Broker Operator (7.14.x) en cada cluster.
2. Desplegar el `ActiveMQArtemis` en cada cluster (MQTT/TLS + AMQP mirroring).
3. Configurar Service Interconnect para que cada cluster pueda resolver/conectar los endpoints AMQP de los otros sitios.
4. Levantar **HAProxy on‑prem** (en `amq1`) y exponer `mqtts://<host>:443` con passthrough TLS.
5. Correr simuladores (Node‑RED) + consumidores y abrir el `visualizer`.

## URLs rápidas

```bash
./scripts/demo-urls.sh
```

## Reset de contadores (para “demo clean”)

El `event-consumer` expone un endpoint para reiniciar contadores y el buffer de “latest events”:

```bash
./scripts/demo-reset.sh
```

## Validar flujo (3 sitios)

```bash
./scripts/demo-verify-flow.sh 3
```

## Notas sobre topics/addresses

- **MQTT topic** usado por el simulador: `iot/events/v4`
- **Core address / queue** en Artemis: `iot.events.v4` (Artemis traduce `/` → `.`)

Para que el visualizer muestre `Uptime` por sitio, configura los base URLs de Jolokia (uno por cluster):

`demo.jolokia.amq1-base-url=http://<route-host>/console/jolokia`

Ejemplo para obtener el `route-host`:

```bash
oc --context amq1 -n amq-multicluster get route amq-amq1-wconsj-0-svc-rte -o jsonpath='{.spec.host}{"\n"}'
```

Ver detalles en:
- `manifests/openshift/README.md`
- `manifests/service-interconnect/README.md`
- Casos de prueba (HA + no duplicados): `docs/demo-test-cases.md`

