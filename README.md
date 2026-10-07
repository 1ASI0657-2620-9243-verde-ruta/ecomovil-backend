# EcoMovil Backend

Backend de EcoMovil basado en microservicios con Spring Boot, PostgreSQL/PostGIS y MQTT (Mosquitto).

## Servicios

- `api-gateway`: puerta de entrada a los microservicios.
- `iam-service`: usuarios y autenticación.
- `vehicle-service`: gestión de vehículos.
- `reservation-service`: gestión de reservas.
- `telemetry-service`: recepción y persistencia de telemetría.
- `safety-service`: reglas y alertas de seguridad/geocercas.
- `access-service`: bloqueo y desbloqueo remoto mediante MQTT.
- `ecomovil-iot-simulator`: simulador IoT.
- `ecomovil-infra`: infraestructura local con Docker Compose y Mosquitto.

## Ejecución local

1. Levantar la infraestructura:

```bash
cd ecomovil-infra
docker compose up -d
```

2. Ejecutar cada microservicio desde IntelliJ IDEA o con Maven Wrapper:

```bash
./mvnw spring-boot:run
```

En Windows PowerShell/CMD:

```bat
mvnw.cmd spring-boot:run
```

3. Ejecutar finalmente `api-gateway`.

## Notas

- El repositorio excluye `target/`, `.idea/` y entornos virtuales.
- Las credenciales y URLs de servicios deben migrarse a variables de entorno antes de un despliegue productivo.
- `ecomovil-backend` se conserva por compatibilidad con el material original; revisar su posible duplicidad con `vehicle-service` antes de la entrega final.
