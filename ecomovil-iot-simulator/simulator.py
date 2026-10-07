import json
import time
from datetime import datetime, timezone

import paho.mqtt.client as mqtt


# ==========================================
# CONFIGURACIÓN
# ==========================================

MQTT_BROKER = "localhost"
MQTT_PORT = 1883

VEHICLE_ID = 1

TELEMETRY_TOPIC = (
    f"ecomovil/vehicles/{VEHICLE_ID}/telemetry"
)

COMMAND_TOPIC = (
    f"ecomovil/vehicles/{VEHICLE_ID}/access/command"
)


# ==========================================
# ESTADO DEL VEHÍCULO
# ==========================================

latitude = -12.0464
longitude = -77.0428

speed = 18.5

locked = False


# ==========================================
# MQTT - CONEXIÓN
# ==========================================

def on_connect(
    client,
    userdata,
    flags,
    reason_code,
    properties
):

    if reason_code == 0:

        print("✅ Conectado al broker MQTT")
        print(
            f"📡 Broker: {MQTT_BROKER}:{MQTT_PORT}"
        )

        print(
            f"📨 Telemetría: {TELEMETRY_TOPIC}"
        )

        print(
            f"🔐 Comandos: {COMMAND_TOPIC}"
        )

        # Suscribirse a comandos
        client.subscribe(
            COMMAND_TOPIC,
            qos=1
        )

        print(
            "👂 Esperando comandos LOCK / UNLOCK..."
        )

    else:

        print(
            f"❌ Error de conexión MQTT. "
            f"Código: {reason_code}"
        )


# ==========================================
# MQTT - COMANDOS
# ==========================================

def on_message(
    client,
    userdata,
    message
):

    global locked

    try:

        payload = message.payload.decode(
            "utf-8"
        )

        print()
        print(
            "📥 Comando MQTT recibido"
        )

        print(
            f"📨 Topic: {message.topic}"
        )

        print(
            f"📦 Payload: {payload}"
        )

        data = json.loads(payload)

        command = data.get("command")

        vehicle_id = data.get("vehicleId")

        if vehicle_id != VEHICLE_ID:

            print(
                "⚠️ Comando ignorado: "
                "vehículo incorrecto"
            )

            return

        if command == "LOCK":

            locked = True

            print()
            print(
                "🔒 ================================"
            )

            print(
                "🔒 VEHÍCULO BLOQUEADO"
            )

            print(
                f"🔒 Vehículo: {VEHICLE_ID}"
            )

            print(
                "🔒 Estado: LOCKED"
            )

            print(
                "🔒 ================================"
            )

        elif command == "UNLOCK":

            locked = False

            print()
            print(
                "🔓 ================================"
            )

            print(
                "🔓 VEHÍCULO DESBLOQUEADO"
            )

            print(
                f"🔓 Vehículo: {VEHICLE_ID}"
            )

            print(
                "🔓 Estado: UNLOCKED"
            )

            print(
                "🔓 ================================"
            )

        else:

            print(
                f"⚠️ Comando desconocido: {command}"
            )

    except Exception as e:

        print(
            f"❌ Error procesando comando MQTT: {e}"
        )


# ==========================================
# MQTT - DESCONEXIÓN
# ==========================================

def on_disconnect(
    client,
    userdata,
    disconnect_flags,
    reason_code,
    properties
):

    print(
        f"⚠️ Desconectado del broker MQTT. "
        f"Código: {reason_code}"
    )


# ==========================================
# CLIENTE MQTT
# ==========================================

client = mqtt.Client(
    mqtt.CallbackAPIVersion.VERSION2,
    client_id=f"ecomovil-simulator-{VEHICLE_ID}"
)

client.on_connect = on_connect
client.on_message = on_message
client.on_disconnect = on_disconnect


# ==========================================
# CONEXIÓN
# ==========================================

print()
print("🚲 EcoMovil - Simulador IoT")
print("-----------------------------")

try:

    client.connect(
        MQTT_BROKER,
        MQTT_PORT,
        60
    )

except Exception as e:

    print(
        f"❌ No se pudo conectar al broker MQTT: {e}"
    )

    exit(1)


client.loop_start()


# ==========================================
# TELEMETRÍA
# ==========================================

try:

    while True:

        telemetry = {

            "vehicleId": VEHICLE_ID,

            "timestamp":
                datetime.now(
                    timezone.utc
                ).isoformat(),

            "latitude": latitude,

            "longitude": longitude,

            "speed": speed,

            "gyroX": 0.12,

            "gyroY": 0.08,

            "gyroZ": 0.03,

            "locked": locked
        }

        payload = json.dumps(
            telemetry
        )

        result = client.publish(
            TELEMETRY_TOPIC,
            payload,
            qos=1
        )

        if result.rc == mqtt.MQTT_ERR_SUCCESS:

            print(
                f"📡 Telemetría enviada | "
                f"🔐 locked={locked}"
            )

        else:

            print(
                f"❌ Error enviando telemetría: "
                f"{result.rc}"
            )

        latitude += 0.00001
        longitude += 0.00001

        time.sleep(5)


except KeyboardInterrupt:

    print(
        "\n🛑 Simulador detenido por el usuario."
    )


finally:

    client.loop_stop()

    client.disconnect()

    print(
        "🔌 Desconectado del broker MQTT."
    )