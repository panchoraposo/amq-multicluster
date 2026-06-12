# Casos de prueba (HA + no duplicados)

Este documento reúne un par de casos de prueba “demo-friendly” para mostrar:

- **Sincronización multicluster** (mirroring) funcionando.
- **No duplicación** (cuando se consume desde un único sitio).
- **HA** (recuperación ante caída/reinicio de broker en OpenShift con persistencia).

> Requisitos: contexts `oc` configurados como `amq1`, `amq2`, `amq3`.

## Caso 1 — Multicluster mirroring sin duplicados (consumo en un solo sitio)

### Objetivo

1) Producir en AMQ1 y ver que el mensaje aparece en AMQ2/AMQ3.  
2) Consumir en AMQ2 y ver que el ACK se refleja y el mensaje desaparece también de AMQ1/AMQ3.  
3) Esto demuestra “sin duplicados” para el caso recomendado: **un consumidor activo** para esa cola (anycast).

### Pasos

Crear cola + publicar (en AMQ1):

```bash
oc --context amq1 -n amq-multicluster rsh amq-amq1-ss-0 bash -lc \
  "PODIP=\$(hostname -i | awk '{print \\$1}'); \
   /opt/amq/bin/artemis queue create --silent --user admin --password admin --protocol CORE --url tcp://\$PODIP:61616 \
     --name mirror-demo --address mirror-demo --anycast --durable --auto-create-address || true; \
   /opt/amq/bin/artemis producer --silent --user admin --password admin --protocol CORE --url tcp://\$PODIP:61616 \
     --destination queue://mirror-demo --message-count 10 --message from-amq1"
```

Verificar que aparece en los 3 (AMQ1/2/3):

```bash
for CTX in amq1 amq2 amq3; do
  echo "=== $CTX mirror-demo ==="
  oc --context "$CTX" -n amq-multicluster rsh "amq-$CTX-ss-0" bash -lc \
    "PODIP=\$(hostname -i | awk '{print \\$1}'); \
     /opt/amq/bin/artemis queue stat --silent --user admin --password admin --protocol CORE --url tcp://\$PODIP:61616 --queueName mirror-demo"
done
```

Consumir **solo** desde AMQ2:

```bash
oc --context amq2 -n amq-multicluster rsh amq-amq2-ss-0 bash -lc \
  "PODIP=\$(hostname -i | awk '{print \\$1}'); \
   /opt/amq/bin/artemis consumer --silent --user admin --password admin --protocol CORE --url tcp://\$PODIP:61616 \
     --destination queue://mirror-demo --message-count 10"
```

Confirmar que el mensaje desaparece en los 3 (ACK mirrored):

```bash
for CTX in amq1 amq2 amq3; do
  echo "=== $CTX mirror-demo post-consume ==="
  oc --context "$CTX" -n amq-multicluster rsh "amq-$CTX-ss-0" bash -lc \
    "PODIP=\$(hostname -i | awk '{print \\$1}'); \
     /opt/amq/bin/artemis queue stat --silent --user admin --password admin --protocol CORE --url tcp://\$PODIP:61616 --queueName mirror-demo"
done
```

## Caso 2 — HA en OpenShift (broker cae y vuelve sin pérdida)

### Objetivo

Demostrar que ante caída de un broker Pod:

- el Pod se re-crea,
- re-atacha el PV,
- los mensajes persistidos siguen disponibles,
- sin duplicación en consumo.

### Pasos

1) Publicar mensajes en una cola (por ejemplo `ha-demo`) en AMQ1.
2) Bajar el Pod del broker (`oc delete pod amq-amq1-ss-0`).
3) Esperar que vuelva a `Ready`.
4) Verificar que el `MESSAGE_COUNT` sigue igual.
5) Consumir y verificar `MESSAGE_COUNT=0`.

```bash
# Publica 20 mensajes
oc --context amq1 -n amq-multicluster rsh amq-amq1-ss-0 bash -lc \
  "PODIP=\$(hostname -i | awk '{print \\$1}'); \
   /opt/amq/bin/artemis queue create --silent --user admin --password admin --protocol CORE --url tcp://\$PODIP:61616 \
     --name ha-demo --address ha-demo --anycast --durable --auto-create-address || true; \
   /opt/amq/bin/artemis producer --silent --user admin --password admin --protocol CORE --url tcp://\$PODIP:61616 \
     --destination queue://ha-demo --message-count 20 --message before-restart"

# Mata el pod y espera
oc --context amq1 -n amq-multicluster delete pod amq-amq1-ss-0
oc --context amq1 -n amq-multicluster wait --for=condition=Ready pod -l statefulset.kubernetes.io/pod-name=amq-amq1-ss-0 --timeout=180s

# Verifica que siguen los mensajes
oc --context amq1 -n amq-multicluster rsh amq-amq1-ss-0 bash -lc \
  "PODIP=\$(hostname -i | awk '{print \\$1}'); \
   /opt/amq/bin/artemis queue stat --silent --user admin --password admin --protocol CORE --url tcp://\$PODIP:61616 --queueName ha-demo"

# Consume todo
oc --context amq1 -n amq-multicluster rsh amq-amq1-ss-0 bash -lc \
  "PODIP=\$(hostname -i | awk '{print \\$1}'); \
   /opt/amq/bin/artemis consumer --silent --user admin --password admin --protocol CORE --url tcp://\$PODIP:61616 \
     --destination queue://ha-demo --message-count 20"

# Queda en 0
oc --context amq1 -n amq-multicluster rsh amq-amq1-ss-0 bash -lc \
  "PODIP=\$(hostname -i | awk '{print \\$1}'); \
   /opt/amq/bin/artemis queue stat --silent --user admin --password admin --protocol CORE --url tcp://\$PODIP:61616 --queueName ha-demo"
```

