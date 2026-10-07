package com.example.ecomovilbackend.vehicle.controller;

import com.example.ecomovilbackend.vehicle.dto.VehicleRequest;
import com.example.ecomovilbackend.vehicle.model.Vehicle;
import com.example.ecomovilbackend.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    // GET /api/vehicles
    @GetMapping
    public ResponseEntity<List<Vehicle>> getAll() {
        return ResponseEntity.ok(vehicleService.findAll());
    }

    // GET /api/vehicles/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Vehicle> getById(@PathVariable Long id) {

        return vehicleService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/vehicles
    @PostMapping
    public ResponseEntity<Vehicle> create(
            @Valid @RequestBody VehicleRequest request) {

        return ResponseEntity.ok(vehicleService.create(request));
    }

    // PUT /api/vehicles/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Vehicle> update(
            @PathVariable Long id,
            @Valid @RequestBody VehicleRequest request) {

        return vehicleService.update(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/vehicles/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        if (!vehicleService.delete(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}