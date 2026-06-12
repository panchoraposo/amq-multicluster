#!/usr/bin/env bash
set -euo pipefail

umask 0002

MQTT_HOST="${MQTT_HOST:-amq-amq1-mqtts-lb.amq-multicluster-amq.svc}"
MQTT_PORT="${MQTT_PORT:-8883}"

TEMPLATE="/opt/demo/flows.template.json"
OUT="/data/flows.json"

mkdir -p /data

TEMPLATE="${TEMPLATE}" OUT="${OUT}" node -e '
  const fs = require("fs");
  const tpl = process.env.TEMPLATE;
  const out = process.env.OUT;
  const host = process.env.MQTT_HOST || "amq-amq1-mqtts-lb.amq-multicluster-amq.svc";
  const port = process.env.MQTT_PORT || "8883";
  let s = fs.readFileSync(tpl, "utf8");
  s = s.replace(/\$\{MQTT_HOST\}/g, host);
  s = s.replace(/\$\{MQTT_PORT\}/g, port);
  fs.writeFileSync(out, s);
'

PORT="${PORT:-1880}"
exec node-red --userDir /data --port "${PORT}"

