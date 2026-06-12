#!/usr/bin/env bash
set -euo pipefail

for CTX in amq1 amq2 amq3; do
  echo "=== ${CTX} ==="
  echo -n "visualizer:     https://"
  oc --context "$CTX" -n amq-multicluster-apps get route visualizer -o jsonpath='{.spec.host}{"\n"}'
  echo -n "event-consumer: https://"
  oc --context "$CTX" -n amq-multicluster-apps get route event-consumer -o jsonpath='{.spec.host}{"\n"}'
  echo -n "device-sim UI:  https://"
  oc --context "$CTX" -n amq-multicluster-apps get route device-simulator -o jsonpath='{.spec.host}{"\n"}'
  echo
done

echo "=== on-prem HAProxy (amq1) ==="
echo -n "onprem mqtts:   "
echo "mqtts://$(oc --context amq1 -n amq-multicluster-edge get route onprem-mqtt -o jsonpath='{.spec.host}'):443"

