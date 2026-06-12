# Architecture: AMQ Broker multi-cluster (3 sites)

## Functional goals

- **IoT ingest per site** using **MQTT over TLS** to the local broker.
- **Global replication**: events ingested in any site should converge to the other sites.
- **Demo connectivity**: use **Red Hat Service Interconnect (Skupper)** for inter-cluster connectivity (no ACM/Submariner required for the demo).

## Replication / synchronization (AMQ Broker)

This demo uses **broker connections** with **AMQP mirroring**. On OpenShift, the configuration is typically expressed via `spec.brokerProperties` on the `ActiveMQArtemis` CR.

- AMQ Broker documentation: `https://docs.redhat.com/en/documentation/red_hat_amq_broker/7.14/html/configuring_amq_broker/configuring-fault-tolerant-system-broker-connections_configuring`
- OpenShift `brokerProperties`: `https://docs.redhat.com/en/documentation/red_hat_amq_broker/7.14/html-single/deploying_amq_broker_on_openshift/index`

### Why full mesh for 3 sites

With mirroring, events received via mirror are typically **not mirrored again** (loop prevention). To achieve “global sync” across 3 sites, the simplest topology is:

- Each site creates **two** mirror connections (`AMQPConnections.*`) to the other two sites.

### Semantics (what the demo highlights)

- Mirroring is **asynchronous** → **eventual consistency**
- If consumers are active in **more than one site** on the same queue/address, there is an **at-least-once** window (duplicates can happen). Mitigations:
  - idempotent consumers with stable `message-id` / `eventId`
  - application-level deduplication
  - partitioning workloads per site when appropriate

## Multi-cluster connectivity

The demo uses **Red Hat Service Interconnect (Skupper)** to connect sites and expose cross-cluster endpoints without requiring Submariner.

- Reference: `https://developers.redhat.com/learn/openshift/link-distributed-services-across-openshift-clusters-using-red-hat-service`

