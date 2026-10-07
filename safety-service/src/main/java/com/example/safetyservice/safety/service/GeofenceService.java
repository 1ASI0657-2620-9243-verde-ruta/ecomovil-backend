package com.example.safetyservice.safety.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class GeofenceService {

    private final JdbcTemplate jdbcTemplate;

    public GeofenceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isInsideActiveGeofence(
            double latitude,
            double longitude
    ) {

        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM geofences
                    WHERE active = TRUE
                    AND ST_Contains(
                        area,
                        ST_SetSRID(
                            ST_Point(?, ?),
                            4326
                        )
                    )
                )
                """;

        Boolean result = jdbcTemplate.queryForObject(
                sql,
                Boolean.class,
                longitude,
                latitude
        );

        return Boolean.TRUE.equals(result);
    }
}