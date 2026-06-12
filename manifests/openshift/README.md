# OpenShift manifests (AMQ Broker 7.14) — 3 sites

## What’s here

Manifests to deploy AMQ Broker on OpenShift (Operator + `ActiveMQArtemis`) in three logical sites (matching your kubeconfig contexts):

- `amq1`
- `amq2`
- `amq3`

Each site:

- exposes **MQTT/TLS** for local ingest
- exposes **AMQP** for inter-site mirroring
- exposes **console/Jolokia** for observability (and optional UI integrations)

Global synchronization is implemented using **broker connections / AMQP mirroring** in a full mesh (each broker mirrors to the other two).

## Note: API version and `brokerProperties`

With AMQ Broker Operator 7.14, the `ActiveMQArtemis` CRD is `broker.amq.io/v1beta1`.
These manifests use that API version so `spec.brokerProperties` is applied correctly.

If a remote mirror endpoint uses `requireLogin: true`, configure `AMQPConnections.<name>.user` and `AMQPConnections.<name>.password` (see docs).

## How to apply (per cluster)

On **each** cluster (`amq1`, `amq2`, `amq3`):

1) Install the Operator (once):
- Apply `00-operator/00-namespace.yaml`, `00-operator/10-operatorgroup.yaml`, `00-operator/20-subscription.yaml`
- Wait for the CSV to become `Succeeded`

2) Create (or adapt) the MQTT TLS secret:
- Use `30-tls-secret-example.yaml` as a reference and create `amq-tls-mqtt`

3) Apply the broker manifest for the site:
- AMQ1: `10-broker-amq1.yaml`
- AMQ2: `10-broker-amq2.yaml`
- AMQ3: `10-broker-amq3.yaml`

4) Create demo addresses/queues (recommended):
- `20-addresses-queues.yaml`

## Mirroring endpoint convention (Service Interconnect)

The `brokerProperties` use URIs like `tcp://amq-amq2-mirror:5672`. The idea is that **Service Interconnect** exposes the remote broker AMQP acceptor using that **service name** in each consumer cluster.

See `manifests/service-interconnect/README.md`.

## References

- Broker connections / mirrors: `https://docs.redhat.com/en/documentation/red_hat_amq_broker/7.14/html/configuring_amq_broker/configuring-fault-tolerant-system-broker-connections_configuring`
- `brokerProperties` on OpenShift: `https://docs.redhat.com/en/documentation/red_hat_amq_broker/7.14/html-single/deploying_amq_broker_on_openshift/index`

