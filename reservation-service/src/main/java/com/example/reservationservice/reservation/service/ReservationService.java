package com.example.reservationservice.reservation.service;

import com.example.reservationservice.reservation.dto.ReservationRequest;
import com.example.reservationservice.reservation.model.Reservation;
import com.example.reservationservice.reservation.model.ReservationStatus;
import com.example.reservationservice.reservation.repository.ReservationRepository;
import com.example.reservationservice.vehicle.VehicleClient;
import com.example.reservationservice.vehicle.dto.VehicleResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final VehicleClient vehicleClient;

    public ReservationService(
            ReservationRepository reservationRepository,
            VehicleClient vehicleClient
    ) {
        this.reservationRepository = reservationRepository;
        this.vehicleClient = vehicleClient;
    }

    // ==========================================
    // LISTAR TODAS LAS RESERVAS
    // ==========================================

    public List<Reservation> findAll() {
        return reservationRepository.findAll();
    }

    // ==========================================
    // BUSCAR RESERVA POR ID
    // ==========================================

    public Optional<Reservation> findById(Long id) {
        return reservationRepository.findById(id);
    }

    // ==========================================
    // BUSCAR RESERVAS POR USUARIO
    // ==========================================

    public List<Reservation> findByUser(Long userId) {
        return reservationRepository.findByUserId(userId);
    }

    // ==========================================
    // BUSCAR RESERVAS POR VEHÍCULO
    // ==========================================

    public List<Reservation> findByVehicle(Long vehicleId) {
        return reservationRepository.findByVehicleId(vehicleId);
    }

    // ==========================================
    // CREAR RESERVA
    // ==========================================

    public Reservation create(
            ReservationRequest request
    ) {

        // ------------------------------------------
        // 1. Validar fechas
        // ------------------------------------------

        if (!request.getEndTime()
                .isAfter(request.getStartTime())) {

            throw new IllegalArgumentException(
                    "La fecha de fin debe ser posterior "
                            + "a la fecha de inicio"
            );
        }

        // ------------------------------------------
        // 2. Consultar vehículo
        // ------------------------------------------

        VehicleResponse vehicle =
                vehicleClient.getVehicle(
                        request.getVehicleId()
                );

        // ------------------------------------------
        // 3. Verificar que exista
        // ------------------------------------------

        if (vehicle == null) {

            throw new IllegalStateException(
                    "El vehículo no existe"
            );
        }

        // ------------------------------------------
        // 4. Verificar disponibilidad
        // ------------------------------------------

        if (!"AVAILABLE".equals(vehicle.getStatus())) {

            throw new IllegalStateException(
                    "El vehículo no está disponible. "
                            + "Estado actual: "
                            + vehicle.getStatus()
            );
        }

        // ------------------------------------------
        // 5. Verificar reservas activas
        // ------------------------------------------

        boolean vehicleBusy =
                reservationRepository
                        .existsByVehicleIdAndStatusIn(
                                request.getVehicleId(),
                                List.of(
                                        ReservationStatus.PENDING,
                                        ReservationStatus.CONFIRMED,
                                        ReservationStatus.ACTIVE
                                )
                        );

        if (vehicleBusy) {

            throw new IllegalStateException(
                    "El vehículo ya tiene una reserva activa"
            );
        }

        // ------------------------------------------
        // 6. Crear reserva
        // ------------------------------------------

        Reservation reservation =
                new Reservation();

        reservation.setVehicleId(
                request.getVehicleId()
        );

        reservation.setUserId(
                request.getUserId()
        );

        reservation.setStartTime(
                request.getStartTime()
        );

        reservation.setEndTime(
                request.getEndTime()
        );

        reservation.setStatus(
                ReservationStatus.CONFIRMED
        );

        // ------------------------------------------
        // 7. Guardar reserva
        // ------------------------------------------

        Reservation savedReservation =
                reservationRepository.save(
                        reservation
                );

        // ------------------------------------------
        // 8. Cambiar vehículo a RENTED
        // ------------------------------------------

        vehicle.setStatus("RENTED");

        vehicleClient.updateVehicle(
                request.getVehicleId(),
                vehicle
        );

        System.out.println(
                "======================================"
        );

        System.out.println(
                "✅ RESERVA CREADA"
        );

        System.out.println(
                "📅 Reserva: "
                        + savedReservation.getId()
        );

        System.out.println(
                "🚲 Vehículo: "
                        + savedReservation.getVehicleId()
        );

        System.out.println(
                "👤 Usuario: "
                        + savedReservation.getUserId()
        );

        System.out.println(
                "📌 Estado: "
                        + savedReservation.getStatus()
        );

        System.out.println(
                "🚲 Vehículo cambiado a RENTED"
        );

        System.out.println(
                "======================================"
        );

        return savedReservation;
    }

    // ==========================================
    // CANCELAR RESERVA
    // ==========================================

    public Optional<Reservation> cancel(
            Long id
    ) {

        return reservationRepository
                .findById(id)
                .map(reservation -> {

                    // ----------------------------------
                    // 1. Cambiar reserva a CANCELLED
                    // ----------------------------------

                    reservation.setStatus(
                            ReservationStatus.CANCELLED
                    );

                    Reservation savedReservation =
                            reservationRepository.save(
                                    reservation
                            );

                    // ----------------------------------
                    // 2. Buscar vehículo
                    // ----------------------------------

                    VehicleResponse vehicle =
                            vehicleClient.getVehicle(
                                    reservation.getVehicleId()
                            );

                    // ----------------------------------
                    // 3. Liberar vehículo
                    // ----------------------------------

                    if (vehicle != null) {

                        vehicle.setStatus(
                                "AVAILABLE"
                        );

                        vehicleClient.updateVehicle(
                                reservation.getVehicleId(),
                                vehicle
                        );

                        System.out.println(
                                "🚲 Vehículo "
                                        + reservation.getVehicleId()
                                        + " cambiado a AVAILABLE"
                        );
                    }

                    System.out.println(
                            "======================================"
                    );

                    System.out.println(
                            "❌ RESERVA CANCELADA"
                    );

                    System.out.println(
                            "📅 Reserva: "
                                    + savedReservation.getId()
                    );

                    System.out.println(
                            "🚲 Vehículo: "
                                    + savedReservation.getVehicleId()
                    );

                    System.out.println(
                            "📌 Estado: "
                                    + savedReservation.getStatus()
                    );

                    System.out.println(
                            "======================================"
                    );

                    return savedReservation;
                });
    }
}