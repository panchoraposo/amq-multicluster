# Test cases (HA + no duplicates)

These test cases are designed to be “demo-friendly” and show:

- **Multi-cluster replication** (AMQP mirroring) working
- **No duplication** (when consuming from a single place, and/or with idempotent consumer)
- **High availability** (broker pod restart with persistence)

> Prereqs: your `oc` contexts must be `amq1`, `amq2`, `amq3`.

## Case 0 — Preparation (URLs + reset)

```bash
./scripts/demo-urls.sh
./scripts/demo-reset.sh
```

## Case 1 — IoT topic flow: on‑prem → HAProxy → (AMQ1/AMQ2) and Public Cloud‑only → AMQ3

### Goal

- On‑prem devices publish to **HAProxy** (TCP ingress) and messages reach **AMQ1** or **AMQ2**
- Public Cloud‑only devices publish directly to the broker in **AMQ3**
- The `visualizer` shows flow and mirroring across 3 sites

### Steps

1) Reset counters:

```bash
./scripts/demo-reset.sh
```

2) Verify traffic is flowing (received increases):

```bash
./scripts/demo-verify-flow.sh 3 topic
```

## Case 2 — Queue (anycast) flow: point-to-point delivery

### Goal

Demonstrate **anycast** semantics using `queue.demo`:

- the `queue-producer` sends continuously
- each site consumes from the anycast queue
- the UI “Queue (anycast)” button switches the visualizer to `mode=queue`

### Steps

```bash
./scripts/demo-reset.sh
./scripts/demo-verify-flow.sh 3 queue
```

## Case 3 — Broker HA on OpenShift (pod restarts, messages persist)

### Goal

Demonstrate that when a broker pod is deleted:

- the pod is recreated
- the PVC is reattached
- persisted messages remain available
- consumption remains stable

### Steps

```bash
./scripts/demo-ha-failover.sh amq1 ha-demo 20
```

