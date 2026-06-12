#!/usr/bin/env bash
set -euo pipefail

for CTX in amq1 amq2 amq3; do
  H="$(oc --context "$CTX" -n amq-multicluster-apps get route event-consumer -o jsonpath='{.spec.host}')"
  echo "=== ${CTX} reset ==="
  curl -sk -X POST "https://${H}/api/reset" | jq -r '"received="+(.received|tostring)+" dup="+(.duplicates|tostring)'
done

