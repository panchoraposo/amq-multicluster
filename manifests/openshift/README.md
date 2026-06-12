# Manifests OpenShift (AMQ Broker 7.14) — 3 sitios

## Qué hay acá

Manifests para desplegar AMQ Broker en OpenShift (Operator + `ActiveMQArtemis`) en tres sitios lógicos (que corresponden a tus kubeconfig contexts):

- `amq1`
- `amq2`
- `amq3`

Cada sitio:

- expone **MQTT/TLS** para ingest local,
- expone **AMQP** para mirroring inter-sitio,
- expone **console/Jolokia** para observabilidad/visualizer.

La sincronización global se implementa con **broker connections / AMQP mirroring** en malla (cada broker con mirrors hacia los otros 2).

## Nota: API version y `brokerProperties`

En AMQ Broker Operator 7.14, el CRD de `ActiveMQArtemis` está en `broker.amq.io/v1beta1`.  
Los manifests de este repo usan esa API version para que `spec.brokerProperties` se aplique correctamente.

Para que el mirroring se conecte a un broker remoto con `requireLogin: true`, se deben configurar `AMQPConnections.<name>.user` y `AMQPConnections.<name>.password` (ver docs).

## Cómo aplicar (por cluster)

En **cada** cluster (`azure`, `dc1`, `dc2`):

1) Instala el Operator (una sola vez):
- Aplica `00-operator/00-namespace.yaml`, `00-operator/10-operatorgroup.yaml`, `00-operator/20-subscription.yaml`
- Espera a que el CSV quede `Succeeded` en `amq-multicluster`.

2) Crea (o adapta) el secret TLS para MQTT:
- Usa `30-tls-secret-example.yaml` como referencia y crea `amq-tls-mqtt`.

3) Aplica el broker del sitio:
- En AMQ1: `10-broker-amq1.yaml`
- En AMQ2: `10-broker-amq2.yaml`
- En AMQ3: `10-broker-amq3.yaml`

4) Crea la cola de demo (opcional pero recomendado):
- `20-addresses-queues.yaml`

## Convención de endpoints para mirroring (Service Interconnect)

Los `brokerProperties` usan URIs como `tcp://amq-amq2-mirror:5672`. La idea es que **Service Interconnect** exponga el acceptor AMQP del broker remoto con ese **nombre de servicio** en cada cluster consumidor.

Esto se completa en `manifests/service-interconnect/README.md`.

## Referencias

- Broker connections / mirrors: `https://docs.redhat.com/en/documentation/red_hat_amq_broker/7.14/html/configuring_amq_broker/configuring-fault-tolerant-system-broker-connections_configuring`
- `brokerProperties` en OpenShift: `https://docs.redhat.com/en/documentation/red_hat_amq_broker/7.14/html-single/deploying_amq_broker_on_openshift/index`

