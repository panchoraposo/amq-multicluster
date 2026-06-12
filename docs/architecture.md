# Arquitectura: AMQ Broker multicluster (3 sitios)

## Objetivo funcional

- **IoT ingest por sitio** usando **MQTT sobre TLS** hacia el broker local.
- **Sincronización global**: los eventos publicados en cualquier sitio deben “aparecer” en los otros dos.
- **Operación demo**: sin ACM/Submariner al inicio; usar **Red Hat Service Interconnect (Skupper)** como conectividad entre clusters.

## Replicación / sincronización (AMQ Broker)

La demo usa **broker connections** con **AMQP mirroring**. En AMQ Broker 7.14 se configura típicamente vía `spec.brokerProperties` del `ActiveMQArtemis` en OpenShift.

- Doc producto: `https://docs.redhat.com/en/documentation/red_hat_amq_broker/7.14/html/configuring_amq_broker/configuring-fault-tolerant-system-broker-connections_configuring`
- Doc OpenShift `brokerProperties`: `https://docs.redhat.com/en/documentation/red_hat_amq_broker/7.14/html-single/deploying_amq_broker_on_openshift/index`

### Por qué malla (full mesh) para 3 sitios

En mirroring es común que los eventos recibidos por mirror **no se re-mirroreen** (loop prevention). Para lograr “global sync” con 3 sitios, la estrategia más simple y explícita es:

- Cada sitio crea **dos** `AMQPConnections.*` (mirrors) hacia los otros dos.

### Semántica a entender (y que la demo mostrará)

- Mirroring es **asíncrono** → consistencia **eventual**.
- Si hay consumidores activos en **más de un sitio** sobre la misma cola, puede existir una ventana de **at-least-once** (duplicados). Mitigaciones:
  - consumidores idempotentes + `message-id` estable,
  - deduplicación en app,
  - evitar “competencia global” y particionar por sitio cuando aplique.

## Conectividad multicluster

La demo propone **Red Hat Service Interconnect (Skupper)** para exponer el endpoint AMQP entre sitios sin requerir Submariner.

- Referencia: `https://developers.redhat.com/learn/openshift/link-distributed-services-across-openshift-clusters-using-red-hat-service`

