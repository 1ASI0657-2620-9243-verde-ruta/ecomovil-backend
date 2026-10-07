package com.example.ecomovilbackend.vehicle.repository;

import com.example.ecomovilbackend.vehicle.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
}