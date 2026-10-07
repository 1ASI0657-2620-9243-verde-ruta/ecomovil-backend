package com.example.telemetryservice.telemetry.service;

import com.example.telemetryservice.telemetry.model.Telemetry;
import com.example.telemetryservice.telemetry.repository.TelemetryRepository;
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
public class MqttTelemetryListener implements MqttCallback {

    private final TelemetryRepository telemetryRepository;
    private final ObjectMapper objectMapper;

    @Value("${mqtt.broker}")
    private String broker;

    @Value("${mqtt.topic}")
    private String topic;

    @Value("${mqtt.client-id}")
    private String clientId;

    private MqttClient mqttClient;

    public MqttTelemetryListener(
            TelemetryRepository telemetryRepository,
            ObjectMapper objectMapper
    ) {
        this.telemetryRepository = telemetryRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void start() {

        try {

            mqttClient = new MqttClient(
                    broker,
                    clientId
            );

            // Registramos el callback antes de conectarnos
            mqttClient.setCallback(this);

            MqttConnectionOptions options =
                    new MqttConnectionOptions();

            options.setCleanStart(true);
            options.setAutomaticReconnect(true);

            // Conectar al broker MQTT
            mqttClient.connect(options);

            // Suscribirse al topic de todos los vehículos
            mqttClient.subscribe(topic, 1);

            System.out.println("======================================");
            System.out.println("✅ Telemetry Service conectado a MQTT");
            System.out.println("📡 Broker: " + broker);
            System.out.println("📨 Topic: " + topic);
            System.out.println("======================================");

        } catch (MqttException e) {

            System.err.println(
                    "❌ Error conectando a MQTT: "
                            + e.getMessage()
            );

        } catch (Exception e) {

            System.err.println(
                    "❌ Error iniciando Telemetry Service: "
                            + e.getMessage()
            );
        }
    }

    /**
     * Se ejecuta cuando MQTT pierde la conexión.
     */
    @Override
    public void disconnected(
            MqttDisconnectResponse disconnectResponse
    ) {

        System.out.println(
                "⚠️ MQTT desconectado: "
                        + disconnectResponse.getReasonString()
        );
    }

    /**
     * Se ejecuta cuando ocurre un error MQTT.
     */
    @Override
    public void mqttErrorOccurred(
            MqttException exception
    ) {

        System.err.println(
                "❌ Error MQTT: "
                        + exception.getMessage()
        );
    }

    /**
     * Se ejecuta cuando llega un mensaje MQTT.
     */
    @Override
    public void messageArrived(
            String receivedTopic,
            MqttMessage message
    ) {

        try {

            String payload = new String(
                    message.getPayload(),
                    StandardCharsets.UTF_8
            );

            System.out.println();
            System.out.println("📡 Telemetría recibida");
            System.out.println("Topic: " + receivedTopic);
            System.out.println("Payload: " + payload);

            // Convertir JSON a Map
            Map<String, Object> data =
                    objectMapper.readValue(
                            payload,
                            Map.class
                    );

            // Crear entidad Telemetry
            Telemetry telemetry = new Telemetry();

            telemetry.setVehicleId(
                    ((Number) data.get("vehicleId"))
                            .longValue()
            );

            telemetry.setTimestamp(
                    OffsetDateTime.parse(
                            (String) data.get("timestamp")
                    )
            );

            telemetry.setLatitude(
                    ((Number) data.get("latitude"))
                            .doubleValue()
            );

            telemetry.setLongitude(
                    ((Number) data.get("longitude"))
                            .doubleValue()
            );

            telemetry.setSpeed(
                    ((Number) data.get("speed"))
                            .doubleValue()
            );

            telemetry.setGyroX(
                    ((Number) data.get("gyroX"))
                            .doubleValue()
            );

            telemetry.setGyroY(
                    ((Number) data.get("gyroY"))
                            .doubleValue()
            );

            telemetry.setGyroZ(
                    ((Number) data.get("gyroZ"))
                            .doubleValue()
            );

            telemetry.setLocked(
                    (Boolean) data.get("locked")
            );

            // Guardar en PostgreSQL
            telemetryRepository.save(telemetry);

            System.out.println(
                    "💾 Telemetría guardada en PostgreSQL"
            );

        } catch (Exception e) {

            System.err.println(
                    "❌ Error procesando telemetría: "
                            + e.getMessage()
            );
        }
    }

    /**
     * Se ejecuta cuando termina una publicación MQTT.
     * Telemetry Service no publica mensajes,
     * por eso no necesitamos hacer nada aquí.
     */
    @Override
    public void deliveryComplete(
            IMqttToken token
    ) {

        // No hacemos nada.
    }

    /**
     * Se ejecuta cuando MQTT se conecta
     * o se reconecta automáticamente.
     */
    @Override
    public void connectComplete(
            boolean reconnect,
            String serverURI
    ) {

        System.out.println(
                "🔗 MQTT conectado: "
                        + serverURI
        );
    }

    /**
     * Se ejecuta cuando llega un paquete
     * de autenticación MQTT.
     *
     * Actualmente EcoMovil no utiliza
     * autenticación MQTT.
     */
    @Override
    public void authPacketArrived(
            int reasonCode,
            MqttProperties properties
    ) {

        // No usamos autenticación MQTT por ahora.
    }
}