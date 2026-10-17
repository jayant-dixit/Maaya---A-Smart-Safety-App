-- 001_initial_schema.sql: Maaya Emergency Database Schema

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(32) NOT NULL UNIQUE,
    safety_pin_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS trusted_contacts (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(64) REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(32) NOT NULL,
    relationship VARCHAR(64) NOT NULL,
    is_emergency_priority BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS emergency_incidents (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64),
    trigger_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    initial_latitude DOUBLE PRECISION NOT NULL,
    initial_longitude DOUBLE PRECISION NOT NULL,
    current_latitude DOUBLE PRECISION NOT NULL,
    current_longitude DOUBLE PRECISION NOT NULL,
    accuracy_meters REAL NOT NULL,
    heart_rate_bpm INTEGER,
    battery_percentage INTEGER,
    stream_session_id VARCHAR(128),
    dashboard_access_token TEXT,
    notes TEXT,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS incident_locations (
    id BIGSERIAL PRIMARY KEY,
    incident_id VARCHAR(64) REFERENCES emergency_incidents(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    accuracy_meters REAL NOT NULL,
    speed REAL,
    altitude DOUBLE PRECISION,
    heart_rate_bpm INTEGER,
    battery_percentage INTEGER,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS safe_ride_sessions (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64),
    vehicle_plate VARCHAR(64) NOT NULL,
    driver_name VARCHAR(128) NOT NULL,
    destination_name VARCHAR(255) NOT NULL,
    destination_latitude DOUBLE PRECISION,
    destination_longitude DOUBLE PRECISION,
    expected_arrival_minutes INTEGER NOT NULL,
    check_in_interval_minutes INTEGER NOT NULL,
    is_completed BOOLEAN DEFAULT FALSE,
    is_escalated_to_sos BOOLEAN DEFAULT FALSE,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_check_in_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_incident_locations_incident_id ON incident_locations(incident_id);
CREATE INDEX IF NOT EXISTS idx_emergency_incidents_status ON emergency_incidents(status);
