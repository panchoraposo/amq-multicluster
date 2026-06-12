# Apps (Quarkus + Visualizer)

## `device-simulator/`

Simula dispositivos IoT publicando eventos por **MQTT** (idealmente sobre TLS) hacia el broker del sitio.

## `event-consumer/`

Consume eventos desde AMQ Broker (AMQP/JMS) y expone un API simple para que el visualizer muestre volumen y últimas muestras.

## `visualizer/`

UI para visualizar el flujo. Puede correr en modo `DEMO_DATA=1` (sin clusters) o apuntando a endpoints reales.

