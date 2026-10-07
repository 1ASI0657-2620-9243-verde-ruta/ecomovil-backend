package com.example.reservationservice.reservation.controller;

import com.example.reservationservice.reservation.dto.ReservationRequest;
import com.example.reservationservice.reservation.model.Reservation;
import com.example.reservationservice.reservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(
            ReservationService reservationService
    ) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public ResponseEntity<List<Reservation>> getAll() {
        return ResponseEntity.ok(
                reservationService.findAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reservation> getById(
            @PathVariable Long id
    ) {

        return reservationService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(
                        ResponseEntity.notFound().build()
                );
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Reservation>> getByUser(
            @PathVariable Long userId
    ) {

        return ResponseEntity.ok(
                reservationService.findByUser(userId)
        );
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<Reservation>> getByVehicle(
            @PathVariable Long vehicleId
    ) {

        return ResponseEntity.ok(
                reservationService.findByVehicle(vehicleId)
        );
    }

    @PostMapping
    public ResponseEntity<Reservation> create(
            @Valid @RequestBody ReservationRequest request
    ) {

        return ResponseEntity.ok(
                reservationService.create(request)
        );
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Reservation> cancel(
            @PathVariable Long id
    ) {

        return reservationService.cancel(id)
                .map(ResponseEntity::ok)
                .orElse(
                        ResponseEntity.notFound().build()
                );
    }
}