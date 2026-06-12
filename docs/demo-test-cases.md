# Casos de prueba (HA + no duplicados)

Este documento reúne un par de casos de prueba “demo-friendly” para mostrar:

- **Sincronización multicluster** (mirroring) funcionando.
- **No duplicación** (cuando se consume desde un único sitio).
- **HA** (recuperación ante caída/reinicio de broker en OpenShift con persistencia).

> Requisitos: contexts `oc` configurados como `amq1`, `amq2`, `amq3`.

## Caso 0 — Preparación (URLs + reset)

```bash
./scripts/demo-urls.sh
./scripts/demo-reset.sh
```

## Caso 1 — Flujo IoT: on‑prem → HAProxy → (AMQ1/AMQ2) y Azure‑only → AMQ3

### Objetivo

- Dispositivos on‑prem publican a **HAProxy** (ingress TCP) y los mensajes llegan a **AMQ1** o **AMQ2**.
- Otros dispositivos (Azure‑only) publican directo al broker en **AMQ3**.
- El `visualizer` muestra el flujo en 3 sitios.

### Pasos

1) Reset de contadores:

```bash
./scripts/demo-reset.sh
```

2) Verificar que el tráfico fluye (incremento de `received`):

```bash
./scripts/demo-verify-flow.sh 3
```

### Objetivo

Demostrar que el demo puede ejecutarse con **contadores limpios** y que el consumo es **idempotente** (se contabilizan solo eventos únicos). El campo `dup` refleja los duplicados detectados y descartados por la app (si ocurren).

### Pasos

Reset + validación rápida:

```bash
./scripts/demo-reset.sh
./scripts/demo-verify-flow.sh 3
```

## Caso 2 — HA en OpenShift (broker cae y vuelve sin pérdida)

### Objetivo

Demostrar que ante caída de un broker Pod:

- el Pod se re-crea,
- re-atacha el PV,
- los mensajes persistidos siguen disponibles,
- sin duplicación en consumo.

### Pasos

```bash
./scripts/demo-ha-failover.sh amq1 ha-demo 20
```

