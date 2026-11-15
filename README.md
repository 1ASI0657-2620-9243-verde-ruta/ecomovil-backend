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
- `ecomovil-iot-simulator`: simulador IoT de la flota.
- `ecomovil-infra`: infraestructura local con Docker Compose y Mosquitto.
- `ecomovil-web`: frontend web (HTML, CSS y JavaScript) que consume el API Gateway.
- `ecomovil-mobile`: app Android que empaqueta `ecomovil-web` para el emulador o un celular.

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

4. (Opcional) Iniciar el simulador IoT:

```bash
cd ecomovil-iot-simulator
pip install -r requirements.txt
python simulator.py
```

## Simulador IoT

El simulador consulta el API Gateway cada 10 segundos y solo transmite por los
vehículos **en uso**, es decir, los que están en estado `RENTED` porque tienen una
reserva vigente. Por cada uno publica cada 5 segundos en
`ecomovil/vehicles/{id}/telemetry`:

- ubicación (`latitude`, `longitude`), rumbo y distancia recorrida;
- velocidad según el tipo de vehículo, con paradas ocasionales;
- giroscopio (`gyroX`, `gyroY`, `gyroZ`), batería y estado de la cerradura;
- datos del vehículo (`type`, `brand`, `model`, `status`), `reservationId` y `userId`.

Responde a los comandos `LOCK` y `UNLOCK` del access-service: bloqueado, el
vehículo se detiene. Cuando la reserva se cancela, deja de transmitir y guarda en
el vehicle-service la última ubicación del vehículo.

Opciones útiles para demos:

```bash
python simulator.py --vehiculos 1,2   # simula vehículos aunque no estén rentados
python simulator.py --eventos         # inyecta excesos de velocidad y caídas
python simulator.py --intervalo 2     # telemetría cada 2 segundos
```

## Frontend y app móvil

- Web: `cd ecomovil-web && python -m http.server 8795` y abrir http://localhost:8795.
- Android: abrir `ecomovil-mobile` en Android Studio y ejecutarlo en un emulador.
  La app usa `http://10.0.2.2:8080`, que es el `localhost` de la PC visto desde el
  emulador. Ver `ecomovil-mobile/README.md`.
- El APK de depuración se compila en GitHub Actions en cada cambio
  (pestaña Actions, artefacto `ecomovil-debug-apk`).

## Notas

- El repositorio excluye `target/`, `.idea/` y entornos virtuales.
- Las credenciales y URLs de servicios deben migrarse a variables de entorno antes de un despliegue productivo.
- `ecomovil-backend` se conserva por compatibilidad con el material original; revisar su posible duplicidad con `vehicle-service` antes de la entrega final.
