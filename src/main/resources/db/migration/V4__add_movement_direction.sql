ALTER TABLE cashflow_movement_history
    ADD COLUMN movement_direction VARCHAR(6) NOT NULL DEFAULT 'CREDIT'
        CHECK (movement_direction IN ('DEBIT', 'CREDIT'));
