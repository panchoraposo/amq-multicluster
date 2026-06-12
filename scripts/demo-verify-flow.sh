#!/usr/bin/env bash
set -euo pipefail

SLEEP_SECS="${1:-3}"

get_received() {
  local ctx="$1"
  local host
  host="$(oc --context "$ctx" -n amq-multicluster-apps get route event-consumer -o jsonpath='{.spec.host}')"
  curl -sk "https://${host}/api/snapshot" | jq -r .received
}

echo "-- received now --"
A1="$(get_received amq1)"; A2="$(get_received amq2)"; A3="$(get_received amq3)"
echo "amq1 ${A1}"
echo "amq2 ${A2}"
echo "amq3 ${A3}"

sleep "${SLEEP_SECS}"

echo "-- received after ${SLEEP_SECS}s --"
B1="$(get_received amq1)"; B2="$(get_received amq2)"; B3="$(get_received amq3)"
echo "amq1 ${A1} -> ${B1}"
echo "amq2 ${A2} -> ${B2}"
echo "amq3 ${A3} -> ${B3}"

