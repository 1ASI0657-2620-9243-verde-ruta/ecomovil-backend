package com.example.safetyservice.safety.service;

import com.example.safetyservice.safety.model.SafetyAlert;
import com.example.safetyservice.safety.repository.SafetyAlertRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class MqttSafetyListener implements MqttCallback {

    private final SafetyAlertRepository safetyAlertRepository;
    private final ObjectMapper objectMapper;
    private final GeofenceService geofenceService;

    @Value("${mqtt.broker}")
    private String broker;

    @Value("${mqtt.topic}")
    private String topic;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${safety.speed-limit}")
    private Double speedLimit;

    @Value("${safety.gyro-threshold}")
    private Double gyroThreshold;

    private MqttClient mqttClient;

    public MqttSafetyListener(
            SafetyAlertRepository safetyAlertRepository,
            ObjectMapper objectMapper,
            GeofenceService geofenceService
    ) {
        this.safetyAlertRepository = safetyAlertRepository;
        this.objectMapper = objectMapper;
        this.geofenceService = geofenceService;
    }

    @PostConstruct
    public void start() {

        try {

            mqttClient = new MqttClient(
                    broker,
                    clientId
            );

            mqttClient.setCallback(this);

            MqttConnectionOptions options =
                    new MqttConnectionOptions();

            options.setCleanStart(true);
            options.setAutomaticReconnect(true);

            mqttClient.connect(options);

            mqttClient.subscribe(topic, 1);

            System.out.println("======================================");
            System.out.println("✅ Safety Service conectado a MQTT");
            System.out.println("📡 Broker: " + broker);
            System.out.println("📨 Topic: " + topic);
            System.out.println("🚦 Límite velocidad: " + speedLimit);
            System.out.println("🚨 Umbral giroscopio: " + gyroThreshold);
            System.out.println("📍 Geofencing: ACTIVO");
            System.out.println("======================================");

        } catch (MqttException e) {

            System.err.println(
                    "❌ Error conectando Safety Service a MQTT: "
                            + e.getMessage()
            );

        } catch (Exception e) {

            System.err.println(
                    "❌ Error iniciando Safety Service: "
                            + e.getMessage()
            );
        }
    }

    @Override
    public void disconnected(
            MqttDisconnectResponse disconnectResponse
    ) {

        System.out.println(
                "⚠️ MQTT desconectado: "
                        + disconnectResponse.getReasonString()
        );
    }

    @Override
    public void mqttErrorOccurred(
            MqttException exception
    ) {

        System.err.println(
                "❌ Error MQTT: "
                        + exception.getMessage()
        );
    }

    @Override
    public void messageArrived(
            String receivedTopic,
            MqttMessage message
    ) {

        try {

            // ==========================================
            // RECIBIR PAYLOAD
            // ==========================================

            String payload = new String(
                    message.getPayload(),
                    StandardCharsets.UTF_8
            );

            System.out.println();
            System.out.println(
                    "🛡️ Safety Service recibió telemetría"
            );
            System.out.println(
                    "Topic: " + receivedTopic
            );
            System.out.println(
                    "Payload: " + payload
            );

            // ==========================================
            // CONVERTIR JSON
            // ==========================================

            Map<String, Object> data =
                    objectMapper.readValue(
                            payload,
                            Map.class
                    );

            // ==========================================
            // DATOS DEL VEHÍCULO
            // ==========================================

            Long vehicleId =
                    ((Number) data.get("vehicleId"))
                            .longValue();

            OffsetDateTime timestamp =
                    OffsetDateTime.parse(
                            (String) data.get("timestamp")
                    );

            Double latitude =
                    ((Number) data.get("latitude"))
                            .doubleValue();

            Double longitude =
                    ((Number) data.get("longitude"))
                            .doubleValue();

            Double speed =
                    ((Number) data.get("speed"))
                            .doubleValue();

            Double gyroX =
                    ((Number) data.get("gyroX"))
                            .doubleValue();

            Double gyroY =
                    ((Number) data.get("gyroY"))
                            .doubleValue();

            Double gyroZ =
                    ((Number) data.get("gyroZ"))
                            .doubleValue();

            // ==========================================
            // 1. DETECCIÓN DE EXCESO DE VELOCIDAD
            // ==========================================

            if (speed > speedLimit) {

                createAlert(
                        vehicleId,
                        timestamp,
                        "SPEED_LIMIT_EXCEEDED",
                        "HIGH",
                        "El vehículo supera el límite de velocidad: "
                                + speed
                                + " km/h",
                        latitude,
                        longitude
                );
            }

            // ==========================================
            // 2. DETECCIÓN DE POSIBLE CAÍDA
            // ==========================================

            double gyroMagnitude =
                    Math.sqrt(
                            Math.pow(gyroX, 2)
                                    + Math.pow(gyroY, 2)
                                    + Math.pow(gyroZ, 2)
                    );

            if (gyroMagnitude > gyroThreshold) {

                createAlert(
                        vehicleId,
                        timestamp,
                        "POSSIBLE_FALL",
                        "CRITICAL",
                        "Se detectó una variación anormal "
                                + "del giroscopio. Magnitud: "
                                + gyroMagnitude,
                        latitude,
                        longitude
                );
            }

            // ==========================================
            // 3. DETECCIÓN DE GEOCERCA
            // ==========================================

            boolean insideGeofence =
                    geofenceService.isInsideActiveGeofence(
                            latitude,
                            longitude
                    );

            if (insideGeofence) {

                System.out.println(
                        "📍 Geocerca: DENTRO ✅"
                );

            } else {

                System.out.println(
                        "📍 Geocerca: FUERA 🚨"
                );

                createAlert(
                        vehicleId,
                        timestamp,
                        "GEOFENCE_VIOLATION",
                        "HIGH",
                        "El vehículo salió de la zona permitida",
                        latitude,
                        longitude
                );
            }

        } catch (Exception e) {

            System.err.println(
                    "❌ Error procesando telemetría de seguridad: "
                            + e.getMessage()
            );
        }
    }

    // ==========================================
    // CREAR ALERTA
    // ==========================================

    private void createAlert(
            Long vehicleId,
            OffsetDateTime timestamp,
            String type,
            String severity,
            String message,
            Double latitude,
            Double longitude
    ) {

        SafetyAlert alert = new SafetyAlert();

        alert.setVehicleId(vehicleId);
        alert.setTimestamp(timestamp);
        alert.setType(type);
        alert.setSeverity(severity);
        alert.setMessage(message);
        alert.setLatitude(latitude);
        alert.setLongitude(longitude);

        safetyAlertRepository.save(alert);

        System.out.println();
        System.out.println(
                "🚨 ================================"
        );
        System.out.println(
                "🚨 ALERTA DE SEGURIDAD"
        );
        System.out.println(
                "🚨 Tipo: " + type
        );
        System.out.println(
                "🚨 Severidad: " + severity
        );
        System.out.println(
                "🚨 Vehículo: " + vehicleId
        );
        System.out.println(
                "🚨 Mensaje: " + message
        );
        System.out.println(
                "🚨 Ubicación: "
                        + latitude
                        + ", "
                        + longitude
        );
        System.out.println(
                "🚨 ================================"
        );
    }

    // ==========================================
    // MQTT CALLBACKS
    // ==========================================

    @Override
    public void deliveryComplete(
            IMqttToken token
    ) {

        // Safety Service no publica MQTT por ahora.
    }

    @Override
    public void connectComplete(
            boolean reconnect,
            String serverURI
    ) {

        System.out.println(
                "🔗 MQTT conectado a Safety Service: "
                        + serverURI
        );
    }

    @Override
    public void authPacketArrived(
            int reasonCode,
            MqttProperties properties
    ) {

        // No usamos autenticación MQTT por ahora.
    }
}