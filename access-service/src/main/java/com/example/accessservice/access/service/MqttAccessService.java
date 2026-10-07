package com.example.accessservice.access.service;

import jakarta.annotation.PostConstruct;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class MqttAccessService {

    @Value("${mqtt.broker}")
    private String broker;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.command-topic}")
    private String commandTopic;

    private MqttClient mqttClient;

    @PostConstruct
    public void start() {

        try {

            mqttClient = new MqttClient(
                    broker,
                    clientId
            );

            MqttConnectionOptions options =
                    new MqttConnectionOptions();

            options.setCleanStart(true);
            options.setAutomaticReconnect(true);

            mqttClient.connect(options);

            System.out.println("======================================");
            System.out.println("✅ Access Service conectado a MQTT");
            System.out.println("📡 Broker: " + broker);
            System.out.println("📨 Topic: " + commandTopic);
            System.out.println("======================================");

        } catch (Exception e) {

            System.err.println(
                    "❌ Error conectando Access Service a MQTT: "
                            + e.getMessage()
            );
        }
    }

    public void lockVehicle(Long vehicleId) {

        publishCommand(
                vehicleId,
                "LOCK"
        );
    }

    public void unlockVehicle(Long vehicleId) {

        publishCommand(
                vehicleId,
                "UNLOCK"
        );
    }

    private void publishCommand(
            Long vehicleId,
            String command
    ) {

        try {

            String topic = String.format(
                    commandTopic,
                    vehicleId
            );

            String payload = """
                    {
                      "vehicleId": %d,
                      "command": "%s"
                    }
                    """.formatted(
                    vehicleId,
                    command
            );

            MqttMessage message =
                    new MqttMessage(
                            payload.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            message.setQos(1);

            mqttClient.publish(
                    topic,
                    message
            );

            System.out.println();
            System.out.println(
                    "📡 Comando MQTT enviado"
            );
            System.out.println(
                    "🚲 Vehículo: " + vehicleId
            );
            System.out.println(
                    "🔐 Comando: " + command
            );
            System.out.println(
                    "📨 Topic: " + topic
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error enviando comando MQTT: "
                            + e.getMessage(),
                    e
            );
        }
    }
}