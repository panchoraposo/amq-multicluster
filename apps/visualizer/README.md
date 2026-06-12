# visualizer (Quarkus)

UI mínima para la demo multicluster.

## Banners por cluster

La pantalla muestra 3 tarjetas con banner de color (AMQ1/AMQ2/AMQ3). Configurable con:

- `demo.ui.amq1-label`, `demo.ui.amq1-color`
- `demo.ui.amq2-label`, `demo.ui.amq2-color`
- `demo.ui.amq3-label`, `demo.ui.amq3-color`

## Integración con `event-consumer`

Si configuras URLs a `event-consumer` (uno por cluster), la UI refresca contadores:

- `demo.consumers.amq1-snapshot-url`
- `demo.consumers.amq2-snapshot-url`
- `demo.consumers.amq3-snapshot-url`

Si los URLs están vacíos, corre con **DEMO_DATA** (sintético).

