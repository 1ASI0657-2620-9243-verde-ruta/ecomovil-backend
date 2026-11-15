-- Se ejecuta solo al crear la base de datos por primera vez.
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS geofences (
    id      SERIAL PRIMARY KEY,
    name    VARCHAR(120) NOT NULL,
    active  BOOLEAN      NOT NULL DEFAULT TRUE,
    area    GEOMETRY(Polygon, 4326) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_geofences_area ON geofences USING GIST (area);

-- Zona piloto: Lima Centro, Jesús María, San Isidro, Miraflores, San Borja y Surco
INSERT INTO geofences (name, active, area)
SELECT 'Zona piloto Lima', TRUE,
       ST_GeomFromText('POLYGON((-77.0750 -12.0300, -76.9600 -12.0300, -76.9600 -12.1700, -77.0750 -12.1700, -77.0750 -12.0300))', 4326)
WHERE NOT EXISTS (SELECT 1 FROM geofences);
