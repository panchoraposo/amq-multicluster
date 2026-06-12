# AMQ Broker multi-cluster demo (3 clusters)

Demo architecture for IoT messaging using **Red Hat AMQ Broker 7.14** (future-proof for 8.x), deployed across **3 OpenShift clusters** (`amq1`, `amq2`, `amq3`):

- **MQTT/TLS ingest on every site**
- **Global replication** via **broker connections / AMQP mirroring** (bidirectional full mesh)
- **Device simulation** using **Node‑RED**
- **Visualizer UI** (inspired by `hodrigohamalho/amq-broker-demo`)
- **Inter-cluster connectivity** using **Red Hat Service Interconnect (Skupper)**
- **Network Observer** (Skupper console) to visualize the service network between clusters

> Note: mirroring is **asynchronous** → **eventual consistency**. If you consume concurrently in more than one site on the same queue/address, there is an **at-least-once** window where duplicates can happen; the demo documents mitigations (idempotency/dedup).

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
  device-simulator-nodered/ # Node-RED (MQTTS producer)
  event-consumer/           # Quarkus consumer + snapshot API for the UI
  queue-producer/           # Quarkus core producer for anycast queue demo
  visualizer/               # UI + backend proxy
```

## Key documentation

- Producto AMQ Broker 7.14: `https://docs.redhat.com/en/documentation/red_hat_amq_broker/7.14/`
- Broker connections / mirroring (multisite): `https://docs.redhat.com/en/documentation/red_hat_amq_broker/7.14/html/configuring_amq_broker/configuring-fault-tolerant-system-broker-connections_configuring`
- `brokerProperties` on OpenShift (mirrors and more): `https://docs.redhat.com/en/documentation/red_hat_amq_broker/7.14/html-single/deploying_amq_broker_on_openshift/index`
- Service Interconnect (Skupper): `https://developers.redhat.com/learn/openshift/link-distributed-services-across-openshift-clusters-using-red-hat-service`
- RHSI Network Observer installation: `https://docs.redhat.com/en/documentation/red_hat_service_interconnect/2.1/html/installation/installing-operator`

## Local quickstart

See [`docs/local-quickstart.md`](docs/local-quickstart.md).

## Demo multicluster (OpenShift)

See [`docs/demo-walkthrough.md`](docs/demo-walkthrough.md) and manifests under [`manifests/`](manifests/).