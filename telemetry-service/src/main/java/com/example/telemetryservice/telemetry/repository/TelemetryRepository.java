package com.example.telemetryservice.telemetry.repository;

import com.example.telemetryservice.telemetry.model.Telemetry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TelemetryRepository extends JpaRepository<Telemetry, Long> {

    List<Telemetry> findTop20ByVehicleIdOrderByTimestampDesc(Long vehicleId);
}