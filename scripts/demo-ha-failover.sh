#!/usr/bin/env bash
set -euo pipefail

CTX="${1:-amq1}"
QUEUE="${2:-ha-demo}"
COUNT="${3:-20}"

NS_AMQ="amq-multicluster-amq"

echo "=== publish ${COUNT} to ${QUEUE} on ${CTX} ==="
oc --context "$CTX" -n "$NS_AMQ" rsh "amq-${CTX}-ss-0" bash -lc \
  "PODIP=\$(hostname -i | awk '{print \\$1}'); \
   /opt/amq/bin/artemis queue create --silent --user admin --password admin --protocol CORE --url tcp://\$PODIP:61616 \
     --name ${QUEUE} --address ${QUEUE} --anycast --durable --auto-create-address || true; \
   /opt/amq/bin/artemis producer --silent --user admin --password admin --protocol CORE --url tcp://\$PODIP:61616 \
     --destination queue://${QUEUE} --message-count ${COUNT} --message before-failover"

echo "=== kill broker pod ${CTX}-ss-0 ==="
oc --context "$CTX" -n "$NS_AMQ" delete pod "amq-${CTX}-ss-0"
oc --context "$CTX" -n "$NS_AMQ" wait --for=condition=Ready pod -l statefulset.kubernetes.io/pod-name="amq-${CTX}-ss-0" --timeout=300s

echo "=== verify message count persists ==="
oc --context "$CTX" -n "$NS_AMQ" rsh "amq-${CTX}-ss-0" bash -lc \
  "PODIP=\$(hostname -i | awk '{print \\$1}'); \
   /opt/amq/bin/artemis queue stat --silent --user admin --password admin --protocol CORE --url tcp://\$PODIP:61616 --queueName ${QUEUE}"

echo "=== consume all and verify 0 ==="
oc --context "$CTX" -n "$NS_AMQ" rsh "amq-${CTX}-ss-0" bash -lc \
  "PODIP=\$(hostname -i | awk '{print \\$1}'); \
   /opt/amq/bin/artemis consumer --silent --user admin --password admin --protocol CORE --url tcp://\$PODIP:61616 \
     --destination queue://${QUEUE} --message-count ${COUNT}"
oc --context "$CTX" -n "$NS_AMQ" rsh "amq-${CTX}-ss-0" bash -lc \
  "PODIP=\$(hostname -i | awk '{print \\$1}'); \
   /opt/amq/bin/artemis queue stat --silent --user admin --password admin --protocol CORE --url tcp://\$PODIP:61616 --queueName ${QUEUE}"

