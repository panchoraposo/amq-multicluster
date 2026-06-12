FROM docker.io/nodered/node-red:4.0.9

# Copy flow template + start script that substitutes env vars.
COPY apps/device-simulator-nodered/flow.json /opt/demo/flows.template.json
COPY apps/device-simulator-nodered/start.sh /usr/local/bin/start.sh

USER root
RUN chmod +x /usr/local/bin/start.sh && chown -R node-red:root /opt/demo /usr/local/bin/start.sh
USER node-red

ENTRYPOINT ["/usr/local/bin/start.sh"]
