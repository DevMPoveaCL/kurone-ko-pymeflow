CREATE UNIQUE INDEX IF NOT EXISTS idx_cashflow_movement_history_profile_source
    ON cashflow_movement_history(profile_id, source_reference)
    WHERE source_reference IS NOT NULL;
