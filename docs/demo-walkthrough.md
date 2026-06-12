# Demo walkthrough (3 OpenShift clusters)

High-level guide to run the demo on three clusters (your `oc` contexts must be `amq1`, `amq2`, `amq3`).

## What you should see

- **Topic mode (multicast)**:
  - **On‑prem devices** publish to **HAProxy** (on `amq1`) and are load-balanced to **AMQ1 / AMQ2**
  - **Public Cloud‑only devices** publish directly to **AMQ3**
  - **AMQP mirroring** replicates across all brokers
- **Queue mode (anycast)**:
  - `queue-producer` sends events to an **anycast** queue (`queue.demo`)
  - each site consumes point‑to‑point (no fan‑out)

## Quick URLs

```bash
./scripts/demo-urls.sh
```

## Reset counters (clean demo)

```bash
./scripts/demo-reset.sh
```

## Verify live flow

- Topic mode:

```bash
./scripts/demo-verify-flow.sh 3 topic
```

- Queue mode:

```bash
./scripts/demo-verify-flow.sh 3 queue
```

## Topic tree notes

- The **Node‑RED** simulator publishes a topic tree under `sensors/#`, for example:
  - `sensors/onprem/factory/plant-a/line/line-1/device/device-24/temp/telemetry`
  - `sensors/public-cloud/factory/plant-b/line/line-2/device/device-7/pressure/telemetry`

## Network Observer (Skupper)

Each cluster runs **RHSI Network Observer** (Skupper console) to visualize service network connectivity.
Routes are created in the `amq-multicluster-skupper` namespace and are named like `networkobserver-network-observer`.

See:
- `manifests/service-interconnect/README.md`
- Test cases: `docs/demo-test-cases.md`

