package com.example.safetyservice.safety.controller;

import com.example.safetyservice.safety.model.SafetyAlert;
import com.example.safetyservice.safety.repository.SafetyAlertRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/safety")
public class SafetyController {

    private final SafetyAlertRepository safetyAlertRepository;

    public SafetyController(
            SafetyAlertRepository safetyAlertRepository
    ) {
        this.safetyAlertRepository = safetyAlertRepository;
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<SafetyAlert>> getAllAlerts() {

        return ResponseEntity.ok(
                safetyAlertRepository.findAll()
        );
    }

    @GetMapping("/alerts/vehicle/{vehicleId}")
    public ResponseEntity<List<SafetyAlert>> getAlertsByVehicle(
            @PathVariable Long vehicleId
    ) {

        return ResponseEntity.ok(
                safetyAlertRepository
                        .findTop20ByVehicleIdOrderByTimestampDesc(
                                vehicleId
                        )
        );
    }
}