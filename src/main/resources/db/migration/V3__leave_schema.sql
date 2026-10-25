-- V3__leave_schema.sql
-- Leave types, per-employee yearly balances, and holidays

CREATE TABLE IF NOT EXISTS leave_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    yearly_quota INTEGER NOT NULL CHECK (yearly_quota >= 0),
    carry_forward BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_leave_types_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS leave_balances (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    leave_type_id BIGINT NOT NULL REFERENCES leave_types(id),
    balance_year INTEGER NOT NULL,
    allocated INTEGER NOT NULL CHECK (allocated >= 0),
    used INTEGER NOT NULL DEFAULT 0 CHECK (used >= 0),
    pending INTEGER NOT NULL DEFAULT 0 CHECK (pending >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_leave_balances_employee_type_year UNIQUE (employee_id, leave_type_id, balance_year)
);

CREATE INDEX IF NOT EXISTS idx_leave_balances_employee_year ON leave_balances(employee_id, balance_year);
CREATE INDEX IF NOT EXISTS idx_leave_balances_type_year ON leave_balances(leave_type_id, balance_year);

CREATE TABLE IF NOT EXISTS holidays (
    id BIGSERIAL PRIMARY KEY,
    holiday_date DATE NOT NULL,
    name VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_holidays_holiday_date UNIQUE (holiday_date)
);
