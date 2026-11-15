package com.example.reservationservice.vehicle;

import com.example.reservationservice.vehicle.dto.VehicleResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class VehicleClient {

    private final RestClient restClient;

    private final String vehicleServiceUrl;

    public VehicleClient(
            RestClient restClient,
            @Value("${vehicle-service.url:http://localhost:8081}")
            String vehicleServiceUrl
    ) {
        this.restClient = restClient;
        this.vehicleServiceUrl = vehicleServiceUrl;
    }

    public VehicleResponse getVehicle(Long vehicleId) {

        return restClient.get()
                .uri(
                        vehicleServiceUrl
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
                        vehicleServiceUrl
                                + "/api/vehicles/{id}",
                        vehicleId
                )
                .body(vehicle)
                .retrieve()
                .body(VehicleResponse.class);
    }
}