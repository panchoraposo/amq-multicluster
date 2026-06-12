# Service Interconnect (Skupper) — multi-cluster connectivity

Goal: enable **AMQP (TCP)** connectivity between brokers deployed on different OpenShift clusters, without Submariner/ACM, during demo development.

## References

- Red Hat Developer tutorial: `https://developers.redhat.com/learn/openshift/link-distributed-services-across-openshift-clusters-using-red-hat-service`

## Suggested approach (practical)

This repo assumes a shared namespace (project) `amq-multicluster` on all clusters, and a **Service Interconnect site** per cluster (same namespace).

The goal is that, in every cluster, there are **local DNS/service names** like:

- `amq-amq1-mirror:5672`
- `amq-amq2-mirror:5672`
- `amq-amq3-mirror:5672`

Those are the endpoints referenced by the broker `brokerProperties` in `manifests/openshift/10-broker-*.yaml`.

## Steps (Skupper CLI v2)

> Note: if RHSI is not installed, the upstream Skupper CLI requires CRDs. If `skupper site create` fails due to missing CRDs, install them first:
>
> `oc apply -f https://skupper.io/v2/install.yaml`

### 1) Preparation (each cluster)

- Log in with `oc` to the target cluster.
- Create/select the project:

```bash
oc new-project amq-multicluster || oc project amq-multicluster
```

### 2) Create a site per cluster

On each cluster:

```bash
skupper -c <amq1|amq2|amq3> -n amq-multicluster site create <amq1|amq2|amq3> --enable-link-access
```

### 3) Connect the sites (mesh or star)

For simplicity, a star with `amq1` as hub is often enough (the network still routes between sites).

Example (star):

On **amq1**:

```bash
skupper -c amq1 -n amq-multicluster token issue amq1.token.yaml --redemptions-allowed 2
```

On **amq2** and **amq3**:

```bash
skupper -c amq2 -n amq-multicluster token redeem amq1.token.yaml
skupper -c amq3 -n amq-multicluster token redeem amq1.token.yaml
```

### 4) Publish each site AMQP as a “mirror endpoint”

Each `ActiveMQArtemis` defines an `amqp` acceptor (5672) and the Operator creates a Service per broker pod, for example:

```bash
amq-amq1-amqp-0-svc
amq-amq2-amqp-0-svc
amq-amq3-amqp-0-svc
```

In Skupper v2, model this with:

- 1 **connector** in the *remote* site (points to the real broker Service)
- 1 **listener** in the *local* site (creates the local DNS/service name `amq-<site>-mirror`)

#### 4.1 Create connectors (one per site, in its own cluster)

```bash
skupper -c amq1 -n amq-multicluster connector create amq-amq1-mirror 5672 --workload service/amq-amq1-amqp-0-svc --routing-key amq-amq1-mirror
skupper -c amq2 -n amq-multicluster connector create amq-amq2-mirror 5672 --workload service/amq-amq2-amqp-0-svc --routing-key amq-amq2-mirror
skupper -c amq3 -n amq-multicluster connector create amq-amq3-mirror 5672 --workload service/amq-amq3-amqp-0-svc --routing-key amq-amq3-mirror
```

#### 4.2 Create listeners (two per site, for the other two brokers)

On **amq1**:

```bash
skupper -c amq1 -n amq-multicluster listener create amq-amq2-mirror 5672 --routing-key amq-amq2-mirror
skupper -c amq1 -n amq-multicluster listener create amq-amq3-mirror 5672 --routing-key amq-amq3-mirror
```

On **amq2**:

```bash
skupper -c amq2 -n amq-multicluster listener create amq-amq1-mirror 5672 --routing-key amq-amq1-mirror
skupper -c amq2 -n amq-multicluster listener create amq-amq3-mirror 5672 --routing-key amq-amq3-mirror
```

On **amq3**:

```bash
skupper -c amq3 -n amq-multicluster listener create amq-amq1-mirror 5672 --routing-key amq-amq1-mirror
skupper -c amq3 -n amq-multicluster listener create amq-amq2-mirror 5672 --routing-key amq-amq2-mirror
```

Expected result: on **each** cluster, `oc get svc` lists `amq-amq<1|2|3>-mirror` Services (the “local endpoints” used by mirroring).

## Important notes

- **Security**: RHSI/Skupper already encrypts cross-site traffic (mTLS). For that reason, in this demo the AMQP acceptor for mirroring can stay with `sslEnabled: false`.
- **MQTT**: MQTT/TLS is exposed by the broker (acceptor `mqtts` with `expose: true`). For real IoT, consider a LoadBalancer/NLB or TCP Ingress depending on your platform; OpenShift Routes are not ideal for non-HTTP TCP in general.

> If your environment does not use the `skupper` CLI, this README can be adapted to the Operator CRDs. The contract remains the same: each site must have `amq-<site>-mirror:5672` available.

