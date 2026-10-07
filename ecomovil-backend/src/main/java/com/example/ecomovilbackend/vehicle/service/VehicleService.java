package com.example.ecomovilbackend.vehicle.service;

import com.example.ecomovilbackend.vehicle.dto.VehicleRequest;
import com.example.ecomovilbackend.vehicle.model.Vehicle;
import com.example.ecomovilbackend.vehicle.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public List<Vehicle> findAll() {
        return vehicleRepository.findAll();
    }

    public Optional<Vehicle> findById(Long id) {
        return vehicleRepository.findById(id);
    }

    public Vehicle create(VehicleRequest request) {

        Vehicle vehicle = new Vehicle(
                request.getType(),
                request.getBrand(),
                request.getModel(),
                request.getStatus(),
                request.getLatitude(),
                request.getLongitude()
        );

        return vehicleRepository.save(vehicle);
    }

    public Optional<Vehicle> update(Long id, VehicleRequest request) {

        return vehicleRepository.findById(id)
                .map(vehicle -> {

                    vehicle.setType(request.getType());
                    vehicle.setBrand(request.getBrand());
                    vehicle.setModel(request.getModel());
                    vehicle.setStatus(request.getStatus());
                    vehicle.setLatitude(request.getLatitude());
                    vehicle.setLongitude(request.getLongitude());

                    return vehicleRepository.save(vehicle);
                });
    }

    public boolean delete(Long id) {

        if (!vehicleRepository.existsById(id)) {
            return false;
        }

        vehicleRepository.deleteById(id);
        return true;
    }
}