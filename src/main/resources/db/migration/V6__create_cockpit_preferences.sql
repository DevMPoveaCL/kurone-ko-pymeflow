CREATE TABLE IF NOT EXISTS cockpit_preferences (
    profile_id VARCHAR(63) PRIMARY KEY REFERENCES vertical_profiles(id),
    opening_balance NUMERIC(18, 2) NOT NULL,
    preferred_horizon_days INTEGER NOT NULL CHECK (preferred_horizon_days IN (7, 30)),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
