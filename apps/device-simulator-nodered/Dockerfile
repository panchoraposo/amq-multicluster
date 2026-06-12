FROM docker.io/nodered/node-red:4.0.9

# Copy flow template + start script that substitutes env vars.
COPY apps/device-simulator-nodered/flow.json /opt/demo/flows.template.json
COPY apps/device-simulator-nodered/start.sh /usr/local/bin/start.sh

USER root
RUN mkdir -p /data \
 && chgrp -R 0 /data /opt/demo /usr/local/bin/start.sh \
 && chmod -R g=u /data /opt/demo \
 && chmod +x /usr/local/bin/start.sh
USER 1001

ENTRYPOINT ["/usr/local/bin/start.sh"]
