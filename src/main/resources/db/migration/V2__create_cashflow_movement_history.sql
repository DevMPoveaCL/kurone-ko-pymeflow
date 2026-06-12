CREATE TABLE IF NOT EXISTS cashflow_movement_history (
    id UUID PRIMARY KEY,
    profile_id VARCHAR(63) NOT NULL REFERENCES vertical_profiles(id),
    amount NUMERIC(18, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL CHECK (LENGTH(currency) = 3),
    movement_date DATE NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('MANUAL_REVIEW', 'PROJECTABLE', 'REJECTED')),
    category_key VARCHAR(80),
    safe_description VARCHAR(160),
    source_reference VARCHAR(80),
    rejection_reason_code VARCHAR(80),
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_cashflow_movement_history_category
        FOREIGN KEY (profile_id, category_key)
        REFERENCES vertical_profile_categories(profile_id, category_key),
    CONSTRAINT chk_cashflow_movement_history_category_status
        CHECK (
            (status = 'PROJECTABLE' AND category_key IS NOT NULL)
            OR (status IN ('MANUAL_REVIEW', 'REJECTED') AND category_key IS NULL)
        )
);

CREATE INDEX IF NOT EXISTS idx_cashflow_movement_history_profile_status_date
    ON cashflow_movement_history(profile_id, status, movement_date);

CREATE INDEX IF NOT EXISTS idx_cashflow_movement_history_profile_created
    ON cashflow_movement_history(profile_id, created_at);
