package com.example.telemetryservice.telemetry.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "telemetry")
public class Telemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long vehicleId;

    @Column(nullable = false)
    private OffsetDateTime timestamp;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Double speed;

    private Double gyroX;

    private Double gyroY;

    private Double gyroZ;

    @Column(nullable = false)
    private Boolean locked;

    // Datos opcionales que envía el simulador IoT / ESP32
    private Integer battery;

    private Double distanceKm;

    private Long reservationId;

    private Long userId;

    public Telemetry() {
    }

    public Long getId() {
        return id;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public Double getGyroX() {
        return gyroX;
    }

    public void setGyroX(Double gyroX) {
        this.gyroX = gyroX;
    }

    public Double getGyroY() {
        return gyroY;
    }

    public void setGyroY(Double gyroY) {
        this.gyroY = gyroY;
    }

    public Double getGyroZ() {
        return gyroZ;
    }

    public void setGyroZ(Double gyroZ) {
        this.gyroZ = gyroZ;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public Integer getBattery() {
        return battery;
    }

    public void setBattery(Integer battery) {
        this.battery = battery;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
