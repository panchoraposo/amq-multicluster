FROM registry.access.redhat.com/ubi9/nodejs-20@sha256:6fc45a4b23fd5763dd2acc125bc235388dc7e95573ff88a835eec8f88a360af4

# Copy flow template + start script that substitutes env vars.
COPY apps/device-simulator-nodered/flow.json /opt/demo/flows.template.json
COPY apps/device-simulator-nodered/start.sh /usr/local/bin/start.sh

USER root
RUN mkdir -p /data /opt/node-red \
 && npm -g config set fund false \
 && npm -g config set update-notifier false \
 && npm -g config set audit false \
 && npm install -g --unsafe-perm node-red@4.0.9 \
 && chgrp -R 0 /data /opt/demo /opt/node-red /usr/local/bin/start.sh \
 && chmod -R g=u /data /opt/demo /opt/node-red \
 && chmod +x /usr/local/bin/start.sh
USER 1001

EXPOSE 1880
ENTRYPOINT ["/usr/local/bin/start.sh"]
