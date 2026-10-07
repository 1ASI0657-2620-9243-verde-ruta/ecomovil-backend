package com.example.reservationservice.vehicle;

import com.example.reservationservice.vehicle.dto.VehicleResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class VehicleClient {

    private final RestClient restClient;

    private static final String VEHICLE_SERVICE_URL =
            "http://localhost:8081";

    public VehicleClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public VehicleResponse getVehicle(Long vehicleId) {

        return restClient.get()
                .uri(
                        VEHICLE_SERVICE_URL
                                + "/api/vehicles/{id}",
                        vehicleId
                )
                .retrieve()
                .body(VehicleResponse.class);
    }

    public VehicleResponse updateVehicle(
            Long vehicleId,
            VehicleResponse vehicle
    ) {

        return restClient.put()
                .uri(
                        VEHICLE_SERVICE_URL
                                + "/api/vehicles/{id}",
                        vehicleId
                )
                .body(vehicle)
                .retrieve()
                .body(VehicleResponse.class);
    }
}