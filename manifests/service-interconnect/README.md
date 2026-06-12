# Service Interconnect (Skupper) — conectividad multicluster

Objetivo: habilitar conectividad **AMQP (TCP)** entre brokers desplegados en distintos clusters OpenShift sin Submariner/ACM durante el desarrollo de la demo.

## Referencias

- Tutorial Red Hat Developer: `https://developers.redhat.com/learn/openshift/link-distributed-services-across-openshift-clusters-using-red-hat-service`

## Enfoque sugerido (práctico)

Este repo asume un namespace común `amq-multicluster` en los 3 clusters, y un **site** de Service Interconnect por cluster (mismo namespace).

La meta es que en cada cluster existan **DNS locales** con estos nombres:

- `amq-amq1-mirror:5672`
- `amq-amq2-mirror:5672`
- `amq-amq3-mirror:5672`

Eso es exactamente lo que usan los `brokerProperties` en los manifests de `manifests/openshift/10-broker-*.yaml`.

## Pasos (Skupper CLI v2)

> Nota: en clusters donde RHSI no está instalado, Skupper OSS requiere CRDs. Si `skupper site create` falla indicando CRDs faltantes, instala primero:
>
> `oc apply -f https://skupper.io/v2/install.yaml`

### 1) Preparación (en cada cluster)

- Loguearte con `oc` al cluster correspondiente.
- Crear/seleccionar el proyecto:

```bash
oc new-project amq-multicluster || oc project amq-multicluster
```

### 2) Inicializar un site por cluster

En cada cluster:

```bash
skupper -c <amq1|amq2|amq3> -n amq-multicluster site create <amq1|amq2|amq3> --enable-link-access
```

### 3) Conectar los sites (malla o estrella)

Para simplicidad, una estrella con `amq1` como hub suele alcanzar (la red enruta igual entre sites).

Ejemplo (estrella):

En **amq1**:

```bash
skupper -c amq1 -n amq-multicluster token issue amq1.token.yaml --redemptions-allowed 2
```

En **amq2** y **amq3**:

```bash
skupper -c amq2 -n amq-multicluster token redeem amq1.token.yaml
skupper -c amq3 -n amq-multicluster token redeem amq1.token.yaml
```

### 4) Publicar el AMQP de cada sitio como “mirror endpoint”

Cada `ActiveMQArtemis` define un acceptor `amqp` (5672) y el Operator crea un `Service` por Pod, por ejemplo:

```bash
amq-amq1-amqp-0-svc
amq-amq2-amqp-0-svc
amq-amq3-amqp-0-svc
```

En Skupper v2, esto se modela con:

- 1 **connector** en el sitio *remoto* (apunta al Service real del broker).
- 1 **listener** en el sitio *local* (crea el DNS/Service local con el nombre `amq-<sitio>-mirror`).

#### 4.1 Crear connectors (uno por sitio, en su propio cluster)

```bash
skupper -c amq1 -n amq-multicluster connector create amq-amq1-mirror 5672 --workload service/amq-amq1-amqp-0-svc --routing-key amq-amq1-mirror
skupper -c amq2 -n amq-multicluster connector create amq-amq2-mirror 5672 --workload service/amq-amq2-amqp-0-svc --routing-key amq-amq2-mirror
skupper -c amq3 -n amq-multicluster connector create amq-amq3-mirror 5672 --workload service/amq-amq3-amqp-0-svc --routing-key amq-amq3-mirror
```

#### 4.2 Crear listeners (dos por sitio, para los otros 2 brokers)

En **amq1**:

```bash
skupper -c amq1 -n amq-multicluster listener create amq-amq2-mirror 5672 --routing-key amq-amq2-mirror
skupper -c amq1 -n amq-multicluster listener create amq-amq3-mirror 5672 --routing-key amq-amq3-mirror
```

En **amq2**:

```bash
skupper -c amq2 -n amq-multicluster listener create amq-amq1-mirror 5672 --routing-key amq-amq1-mirror
skupper -c amq2 -n amq-multicluster listener create amq-amq3-mirror 5672 --routing-key amq-amq3-mirror
```

En **amq3**:

```bash
skupper -c amq3 -n amq-multicluster listener create amq-amq1-mirror 5672 --routing-key amq-amq1-mirror
skupper -c amq3 -n amq-multicluster listener create amq-amq2-mirror 5672 --routing-key amq-amq2-mirror
```

Resultado esperado: en **cada** cluster, `oc get svc` debe listar los Services `amq-amq<1|2|3>-mirror` (los “local endpoints” que usa el mirroring).

### 5) Validar reachability desde cada broker

Una validación rápida (desde un pod cualquiera en el namespace) es resolver DNS y conectar a `:5672`.

## Notas importantes

- **Seguridad**: RHSI/Skupper ya cifra el tráfico (mTLS) entre sites. Por eso, en los manifests el acceptor AMQP para mirroring está como `sslEnabled: false` por defecto.
- **MQTT**: MQTT/TLS se expone por el broker (acceptor `mqtts` con `expose: true`). Para IoT real, considera un `LoadBalancer`/NLB o un Ingress TCP según tu plataforma; Route puede no ser ideal para TCP no-HTTP.

> Si tu instalación RHSI no usa `skupper` CLI, ajustamos este README a los CRDs reales del Operator (pero el contrato se mantiene: terminar con `amq-<site>-mirror:5672` disponible en todos los sites).

