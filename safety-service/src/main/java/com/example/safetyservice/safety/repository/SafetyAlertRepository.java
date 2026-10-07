package com.example.safetyservice.safety.repository;

import com.example.safetyservice.safety.model.SafetyAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SafetyAlertRepository
        extends JpaRepository<SafetyAlert, Long> {

    List<SafetyAlert> findTop20ByVehicleIdOrderByTimestampDesc(
            Long vehicleId
    );
}