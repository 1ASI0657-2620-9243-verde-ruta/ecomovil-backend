"""
EcoMovil - Simulador IoT

Simula la unidad de telemetría (ESP32) de cada vehículo de la flota.

Solo transmite mientras el vehículo está ACTIVO, es decir, mientras tiene
una reserva vigente (el vehicle-service lo marca como RENTED al reservar y
lo devuelve a AVAILABLE al cancelar). Por cada vehículo activo publica cada
pocos segundos su ubicación, velocidad, giroscopio, batería, estado de la
cerradura y los datos del vehículo y de la reserva.

Cuando la reserva termina, el simulador deja de transmitir y guarda en el
vehicle-service la última ubicación, para que el vehículo quede en el mapa
donde lo dejó el usuario.

Uso:
    pip install -r requirements.txt
    python simulator.py                      # sigue las reservas reales
    python simulator.py --vehiculos 1,2      # fuerza vehículos sin reserva (demo)
    python simulator.py --eventos            # inyecta excesos de velocidad y caídas

Variables de entorno (opcionales):
    ECOMOVIL_API_URL    URL del API Gateway (por defecto http://localhost:8080)
    MQTT_BROKER         host del broker MQTT (por defecto localhost)
    MQTT_PORT           puerto del broker (por defecto 1883)
"""

import argparse
import json
import math
import os
import random
import threading
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone

import paho.mqtt.client as mqtt


# ==========================================
# CONFIGURACIÓN
# ==========================================

API_URL = os.getenv("ECOMOVIL_API_URL", "http://localhost:8080").rstrip("/")
MQTT_BROKER = os.getenv("MQTT_BROKER", "localhost")
MQTT_PORT = int(os.getenv("MQTT_PORT", "1883"))

TELEMETRY_TOPIC = "ecomovil/vehicles/{vehicle_id}/telemetry"
COMMAND_TOPIC = "ecomovil/vehicles/+/access/command"

# Punto de partida si el vehículo no tiene coordenadas (Plaza San Martín, Lima)
DEFAULT_LAT = -12.0517
DEFAULT_LON = -77.0346

# Velocidad de crucero (km/h) por tipo de vehículo. El límite del
# safety-service es 25 km/h, así que en condiciones normales no se supera.
CRUISE_SPEED = {
    "bicicleta": 15.0,
    "scooter": 20.0,
    "patin": 12.0,
    "patín": 12.0,
    "moto": 22.0,
}

# Estados de reserva que cuentan como reserva vigente
ACTIVE_RESERVATION_STATUSES = {"PENDING", "CONFIRMED", "ACTIVE"}


# ==========================================
# ESTADO DE CADA VEHÍCULO SIMULADO
# ==========================================

class SimulatedVehicle:
    """Estado físico de un vehículo mientras dura su viaje."""

    def __init__(self, data):
        self.vehicle_id = int(data["id"])
        self.update_info(data)

        self.latitude = data.get("latitude") or DEFAULT_LAT
        self.longitude = data.get("longitude") or DEFAULT_LON

        self.heading = random.uniform(0, 360)
        self.speed = 0.0
        self.locked = False
        self.battery = random.randint(70, 100)
        self.distance_km = 0.0

        self.reservation_id = None
        self.user_id = None

        # Evento puntual pendiente: "SPEEDING" o "FALL"
        self.pending_event = None

    def update_info(self, data):
        self.type = str(data.get("type") or "bicicleta")
        self.brand = str(data.get("brand") or "")
        self.model = str(data.get("model") or "")
        self.status = str(data.get("status") or "")

    def cruise_speed(self):
        return CRUISE_SPEED.get(self.type.strip().lower(), 15.0)

    def step(self, seconds):
        """Avanza la simulación `seconds` segundos y devuelve el giroscopio."""

        gyro = [
            random.uniform(-0.15, 0.15),
            random.uniform(-0.15, 0.15),
            random.uniform(-0.10, 0.10),
        ]

        if self.locked:
            # Bloqueado: no se mueve
            self.speed = 0.0
            self.pending_event = None
            return gyro

        # Velocidad: oscila alrededor de la de crucero,
        # con paradas ocasionales (semáforos, cruces)
        if random.random() < 0.08:
            self.speed = 0.0
        else:
            target = self.cruise_speed() + random.uniform(-3.0, 3.0)
            self.speed = max(0.0, self.speed + (target - self.speed) * 0.6)

        # Eventos inyectados para probar el safety-service
        if self.pending_event == "SPEEDING":
            self.speed = 32.0 + random.uniform(0, 6)
        elif self.pending_event == "FALL":
            self.speed = 0.0
            gyro = [
                random.uniform(2.5, 4.0),
                random.uniform(1.5, 3.0),
                random.uniform(0.5, 1.5),
            ]
        self.pending_event = None

        # Rumbo: giros suaves, como siguiendo calles
        self.heading = (self.heading + random.uniform(-25, 25)) % 360

        # Desplazamiento en metros
        meters = (self.speed / 3.6) * seconds
        rad = math.radians(self.heading)
        d_lat = (meters * math.cos(rad)) / 111_320
        d_lon = (meters * math.sin(rad)) / (
            111_320 * math.cos(math.radians(self.latitude))
        )
        self.latitude += d_lat
        self.longitude += d_lon
        self.distance_km += meters / 1000

        # Los eléctricos gastan batería al moverse
        if self.type.strip().lower() != "bicicleta" and meters > 0:
            self.battery = max(0, self.battery - random.choice([0, 0, 1]))

        return gyro

    def telemetry(self, gyro):
        """Mensaje MQTT. Conserva los campos que esperan telemetry-service y
        safety-service y agrega los datos del vehículo y de la reserva."""

        return {
            "vehicleId": self.vehicle_id,
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "latitude": round(self.latitude, 7),
            "longitude": round(self.longitude, 7),
            "speed": round(self.speed, 2),
            "gyroX": round(gyro[0], 3),
            "gyroY": round(gyro[1], 3),
            "gyroZ": round(gyro[2], 3),
            "locked": self.locked,
            "battery": self.battery,
            "heading": round(self.heading, 1),
            "distanceKm": round(self.distance_km, 3),
            "vehicle": {
                "type": self.type,
                "brand": self.brand,
                "model": self.model,
                "status": self.status,
            },
            "reservationId": self.reservation_id,
            "userId": self.user_id,
        }


# ==========================================
# ACCESO AL API GATEWAY
# ==========================================

def http_json(method, path, body=None, timeout=5):
    url = f"{API_URL}{path}"
    data = json.dumps(body).encode("utf-8") if body is not None else None
    request = urllib.request.Request(
        url,
        data=data,
        method=method,
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(request, timeout=timeout) as response:
        raw = response.read().decode("utf-8")
        return json.loads(raw) if raw else None


def fetch_vehicles():
    return http_json("GET", "/api/vehicles") or []


def fetch_active_reservations():
    """Devuelve {vehicleId: reserva vigente}. Si falla, devuelve {}."""
    try:
        reservations = http_json("GET", "/api/reservations") or []
    except (urllib.error.URLError, OSError, ValueError):
        return {}

    active = {}
    for reservation in reservations:
        if reservation.get("status") in ACTIVE_RESERVATION_STATUSES:
            active[int(reservation["vehicleId"])] = reservation
    return active


def save_final_position(sim):
    """Al terminar el viaje guarda la última ubicación en el vehicle-service."""
    try:
        current = http_json("GET", f"/api/vehicles/{sim.vehicle_id}")
        if not current:
            return
        http_json(
            "PUT",
            f"/api/vehicles/{sim.vehicle_id}",
            {
                "type": current["type"],
                "brand": current["brand"],
                "model": current["model"],
                "status": current["status"],
                "latitude": round(sim.latitude, 7),
                "longitude": round(sim.longitude, 7),
            },
        )
        print(
            f"📍 Vehículo {sim.vehicle_id}: última ubicación guardada "
            f"({sim.latitude:.5f}, {sim.longitude:.5f})"
        )
    except (urllib.error.URLError, OSError, ValueError, KeyError) as e:
        print(f"⚠️ No se pudo guardar la ubicación final de {sim.vehicle_id}: {e}")


# ==========================================
# SIMULADOR
# ==========================================

class FleetSimulator:

    def __init__(self, interval, poll, forced_ids, events):
        self.interval = interval
        self.poll = poll
        self.forced_ids = set(forced_ids)
        self.events = events

        self.active = {}  # vehicleId -> SimulatedVehicle
        self.lock = threading.Lock()
        self.last_poll = 0.0
        self.api_ok = None
        self.idle_notice = False

        self.client = mqtt.Client(
            mqtt.CallbackAPIVersion.VERSION2,
            client_id=f"ecomovil-simulator-{os.getpid()}",
        )
        self.client.on_connect = self.on_connect
        self.client.on_message = self.on_message
        self.client.on_disconnect = self.on_disconnect

    # ---------- MQTT ----------

    def on_connect(self, client, userdata, flags, reason_code, properties):
        if reason_code == 0:
            print(f"✅ Conectado al broker MQTT {MQTT_BROKER}:{MQTT_PORT}")
            client.subscribe(COMMAND_TOPIC, qos=1)
            print(f"👂 Escuchando comandos LOCK / UNLOCK en {COMMAND_TOPIC}")
        else:
            print(f"❌ Error de conexión MQTT. Código: {reason_code}")

    def on_disconnect(self, client, userdata, flags, reason_code, properties):
        print(f"⚠️ Desconectado del broker MQTT. Código: {reason_code}")

    def on_message(self, client, userdata, message):
        try:
            data = json.loads(message.payload.decode("utf-8"))
            vehicle_id = int(data.get("vehicleId"))
            command = data.get("command")
        except (ValueError, TypeError) as e:
            print(f"❌ Comando MQTT inválido: {e}")
            return

        with self.lock:
            sim = self.active.get(vehicle_id)
            if sim is None:
                print(
                    f"⚠️ Comando {command} para el vehículo {vehicle_id} "
                    f"ignorado: no está en uso"
                )
                return

            if command == "LOCK":
                sim.locked = True
                print(f"🔒 Vehículo {vehicle_id} BLOQUEADO")
            elif command == "UNLOCK":
                sim.locked = False
                print(f"🔓 Vehículo {vehicle_id} DESBLOQUEADO")
            else:
                print(f"⚠️ Comando desconocido: {command}")

    # ---------- Flota ----------

    def sync_fleet(self):
        """Consulta el backend y arranca o detiene la simulación de cada
        vehículo según tenga o no una reserva vigente."""
        try:
            vehicles = fetch_vehicles()
            reservations = fetch_active_reservations()
            if self.api_ok is not True:
                print(f"🌐 API Gateway disponible en {API_URL}")
            self.api_ok = True
        except (urllib.error.URLError, OSError, ValueError) as e:
            if self.api_ok is not False:
                print(
                    f"⚠️ No se pudo consultar {API_URL}/api/vehicles ({e}). "
                    f"Reintentando cada {self.poll:g} s..."
                )
            self.api_ok = False
            return

        in_use = {}
        for vehicle in vehicles:
            vehicle_id = int(vehicle["id"])
            rented = vehicle.get("status") == "RENTED"
            if rented or vehicle_id in self.forced_ids:
                in_use[vehicle_id] = vehicle

        with self.lock:
            # Viajes que empiezan
            for vehicle_id, vehicle in in_use.items():
                sim = self.active.get(vehicle_id)
                if sim is None:
                    sim = SimulatedVehicle(vehicle)
                    self.active[vehicle_id] = sim
                    print()
                    print(
                        f"🚲 Viaje iniciado | vehículo {vehicle_id} "
                        f"({sim.type} {sim.brand} {sim.model}) "
                        f"desde ({sim.latitude:.5f}, {sim.longitude:.5f})"
                    )
                else:
                    sim.update_info(vehicle)

                reservation = reservations.get(vehicle_id)
                sim.reservation_id = reservation.get("id") if reservation else None
                sim.user_id = reservation.get("userId") if reservation else None

            # Viajes que terminan
            finished = [vid for vid in self.active if vid not in in_use]
            ended = [self.active.pop(vid) for vid in finished]

        for sim in ended:
            print()
            print(
                f"🏁 Viaje terminado | vehículo {sim.vehicle_id} | "
                f"{sim.distance_km:.2f} km recorridos"
            )
            save_final_position(sim)

        if self.active:
            self.idle_notice = False
        elif not self.idle_notice:
            print("💤 Ningún vehículo en uso. Esperando reservas...")
            self.idle_notice = True

    def tick(self):
        with self.lock:
            sims = list(self.active.values())

        for sim in sims:
            if self.events and random.random() < 0.05:
                sim.pending_event = random.choice(["SPEEDING", "FALL"])

            gyro = sim.step(self.interval)
            payload = sim.telemetry(gyro)
            topic = TELEMETRY_TOPIC.format(vehicle_id=sim.vehicle_id)
            result = self.client.publish(topic, json.dumps(payload), qos=1)

            if result.rc == mqtt.MQTT_ERR_SUCCESS:
                print(
                    f"📡 Vehículo {sim.vehicle_id} | "
                    f"({payload['latitude']:.5f}, {payload['longitude']:.5f}) | "
                    f"{payload['speed']:>5.1f} km/h | "
                    f"batería {sim.battery}% | "
                    f"{'🔒' if sim.locked else '🔓'} | "
                    f"reserva {sim.reservation_id or '-'}"
                )
            else:
                print(f"❌ Error enviando telemetría de {sim.vehicle_id}: {result.rc}")

    # ---------- Ciclo principal ----------

    def run(self):
        print()
        print("🚲 EcoMovil - Simulador IoT")
        print("-----------------------------")
        print(f"🌐 API Gateway: {API_URL}")
        print(f"⏱️  Telemetría cada {self.interval:g} s, flota cada {self.poll:g} s")
        if self.forced_ids:
            print(f"🧪 Vehículos forzados: {sorted(self.forced_ids)}")
        if self.events:
            print("🧪 Eventos de seguridad aleatorios: ACTIVADOS")

        try:
            self.client.connect(MQTT_BROKER, MQTT_PORT, 60)
        except OSError as e:
            print(f"❌ No se pudo conectar al broker MQTT: {e}")
            raise SystemExit(1)

        self.client.loop_start()

        try:
            while True:
                if time.time() - self.last_poll >= self.poll:
                    self.sync_fleet()
                    self.last_poll = time.time()
                self.tick()
                time.sleep(self.interval)
        except KeyboardInterrupt:
            print("\n🛑 Simulador detenido por el usuario.")
        finally:
            self.client.loop_stop()
            self.client.disconnect()
            print("🔌 Desconectado del broker MQTT.")


def parse_args():
    parser = argparse.ArgumentParser(description="EcoMovil - Simulador IoT")
    parser.add_argument(
        "--intervalo", type=float, default=5,
        help="segundos entre mensajes de telemetría (por defecto 5)",
    )
    parser.add_argument(
        "--sync", type=float, default=10,
        help="segundos entre consultas de la flota al backend (por defecto 10)",
    )
    parser.add_argument(
        "--vehiculos", default="",
        help="ids separados por coma que se simulan aunque no estén rentados",
    )
    parser.add_argument(
        "--eventos", action="store_true",
        help="inyecta de vez en cuando excesos de velocidad y caídas",
    )
    args = parser.parse_args()
    forced = [int(x) for x in args.vehiculos.split(",") if x.strip()]
    return args.intervalo, args.sync, forced, args.eventos


if __name__ == "__main__":
    interval, poll, forced_ids, events = parse_args()
    FleetSimulator(interval, poll, forced_ids, events).run()
