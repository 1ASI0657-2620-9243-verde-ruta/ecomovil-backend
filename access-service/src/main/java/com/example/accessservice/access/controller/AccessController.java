package com.example.accessservice.access.controller;

import com.example.accessservice.access.service.MqttAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/access")
public class AccessController {

    private final MqttAccessService mqttAccessService;

    public AccessController(
            MqttAccessService mqttAccessService
    ) {
        this.mqttAccessService = mqttAccessService;
    }

    @PostMapping("/vehicles/{vehicleId}/lock")
    public ResponseEntity<Map<String, Object>> lock(
            @PathVariable Long vehicleId
    ) {

        mqttAccessService.lockVehicle(vehicleId);

        return ResponseEntity.ok(
                Map.of(
                        "vehicleId", vehicleId,
                        "locked", true,
                        "message",
                        "Comando de bloqueo enviado correctamente"
                )
        );
    }

    @PostMapping("/vehicles/{vehicleId}/unlock")
    public ResponseEntity<Map<String, Object>> unlock(
            @PathVariable Long vehicleId
    ) {

        mqttAccessService.unlockVehicle(vehicleId);

        return ResponseEntity.ok(
                Map.of(
                        "vehicleId", vehicleId,
                        "locked", false,
                        "message",
                        "Comando de desbloqueo enviado correctamente"
                )
        );
    }
}