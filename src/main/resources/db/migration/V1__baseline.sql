-- V1__baseline.sql
-- Baseline schema initialization for StaffSheet

CREATE TABLE IF NOT EXISTS schema_baseline_marker (
    id SERIAL PRIMARY KEY,
    initialized_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(255) NOT NULL
);

INSERT INTO schema_baseline_marker (description)
VALUES ('StaffSheet schema baseline initialized');

