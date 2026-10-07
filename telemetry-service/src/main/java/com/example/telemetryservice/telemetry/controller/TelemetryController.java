package com.example.telemetryservice.telemetry.controller;

import com.example.telemetryservice.telemetry.model.Telemetry;
import com.example.telemetryservice.telemetry.repository.TelemetryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

    private final TelemetryRepository telemetryRepository;

    public TelemetryController(
            TelemetryRepository telemetryRepository
    ) {
        this.telemetryRepository = telemetryRepository;
    }

    @GetMapping
    public ResponseEntity<List<Telemetry>> getAll() {
        return ResponseEntity.ok(
                telemetryRepository.findAll()
        );
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<Telemetry>> getByVehicle(
            @PathVariable Long vehicleId
    ) {

        return ResponseEntity.ok(
                telemetryRepository
                        .findTop20ByVehicleIdOrderByTimestampDesc(vehicleId)
        );
    }
}