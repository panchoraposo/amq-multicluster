# Demo walkthrough (3 clusters OpenShift)

Guía de alto nivel para correr la demo en 3 clusters (contexts `oc`: `amq1`, `amq2`, `amq3`):

- `amq1`: broker + ingest MQTT/TLS
- `amq2`: broker + ingest MQTT/TLS
- `amq3`: broker + ingest MQTT/TLS

La conectividad entre clusters se realiza con **Service Interconnect / Skupper** para exponer endpoints AMQP entre sitios.

## Pasos (resumen)

1. Instalar AMQ Broker Operator (7.14.x) en cada cluster.
2. Desplegar el `ActiveMQArtemis` en cada cluster.
3. Configurar Service Interconnect para que cada cluster pueda resolver/conectar los endpoints AMQP de los otros sitios.
4. Validar mirroring bidireccional (malla): publicar en un sitio y verificar que el evento aparece en los otros dos.
5. Correr simuladores Quarkus (por sitio) y abrir el visualizer UI.

## Visualizer con Jolokia

Para que el visualizer muestre `Uptime` por sitio, configura los base URLs de Jolokia (uno por cluster):

`demo.jolokia.amq1-base-url=http://<route-host>/console/jolokia`

Ejemplo para obtener el `route-host`:

```bash
oc --context amq1 -n amq-multicluster get route amq-amq1-wconsj-0-svc-rte -o jsonpath='{.spec.host}{"\n"}'
```

Ver detalles en:
- `manifests/openshift/README.md`
- `manifests/service-interconnect/README.md`
- Casos de prueba (HA + no duplicados): `docs/demo-test-cases.md`

