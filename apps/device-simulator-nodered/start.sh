#!/usr/bin/env bash
set -euo pipefail

umask 0002

MQTT_HOST="${MQTT_HOST:-amq-amq1-mqtts-lb.amq-multicluster-amq.svc}"
MQTT_PORT="${MQTT_PORT:-8883}"

TEMPLATE="/opt/demo/flows.template.json"
OUT="/data/flows.json"

mkdir -p /data

sed \
  -e "s|\\\\\\${MQTT_HOST}|${MQTT_HOST}|g" \
  -e "s|\\\\\\${MQTT_PORT}|${MQTT_PORT}|g" \
  "${TEMPLATE}" > "${OUT}"

exec /usr/src/node-red/entrypoint.sh

