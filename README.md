# AMQ Broker multicluster demo (3 clusters)

Demostración de una arquitectura IoT/mensajería basada en **Red Hat AMQ Broker 7.14** (a futuro 8.x), desplegada en **3 clusters OpenShift** (`azure`, `dc1`, `dc2`) con:

- **Ingest MQTT/TLS por sitio**: dispositivos publican en el broker de su sitio (algunos solo llegan a Azure, otros a DC1/DC2).
- **Sincronización global**: lo que llega a cualquier sitio se replica hacia los otros dos mediante **broker connections / AMQP mirroring** (bidireccional en malla).
- **Simuladores y apps** en **Quarkus 3.33** (privilegiando Red Hat Build of Quarkus) y un **visualizer UI** para “ver” el flujo.
- **Conectividad multicluster** para la demo usando **Red Hat Service Interconnect (Skupper)** (y luego Submariner en la demo final).

> Nota: la sincronización es **eventual (async)**. Si consumes activamente en más de un sitio sobre la misma cola, existe una ventana de **at-least-once** con potenciales duplicados; la demo lo documenta y ofrece mitigaciones (idempotencia/dedup).

## Estructura del repo

```
docs/
  architecture.md
  local-quickstart.md
  demo-walkthrough.md
manifests/
  openshift/
    README.md
  service-interconnect/
    README.md
apps/
  device-simulator/      # Quarkus (MQTT/TLS producer)
  event-consumer/        # Quarkus (AMQP/JMS consumer + API para UI)
  visualizer/            # UI + backend (modo demo-data)
```

## Documentación clave

- Producto AMQ Broker 7.14: `https://docs.redhat.com/en/documentation/red_hat_amq_broker/7.14/`
- Broker connections / mirroring (multisite): `https://docs.redhat.com/en/documentation/red_hat_amq_broker/7.14/html/configuring_amq_broker/configuring-fault-tolerant-system-broker-connections_configuring`
- `brokerProperties` en OpenShift (para configurar mirrors y más): `https://docs.redhat.com/en/documentation/red_hat_amq_broker/7.14/html-single/deploying_amq_broker_on_openshift/index`
- Service Interconnect (Skupper): `https://developers.redhat.com/learn/openshift/link-distributed-services-across-openshift-clusters-using-red-hat-service`

## Arranque rápido (local)

Ver [`docs/local-quickstart.md`](docs/local-quickstart.md).

## Demo multicluster (OpenShift)

Ver [`docs/demo-walkthrough.md`](docs/demo-walkthrough.md) y los manifests en [`manifests/`](manifests/).