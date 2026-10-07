package com.example.reservationservice.reservation.repository;

import com.example.reservationservice.reservation.model.Reservation;
import com.example.reservationservice.reservation.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByVehicleId(Long vehicleId);

    boolean existsByVehicleIdAndStatusIn(
            Long vehicleId,
            List<ReservationStatus> statuses
    );
}