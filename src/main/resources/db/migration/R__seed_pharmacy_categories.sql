INSERT INTO vertical_profiles (id, display_name, enabled)
VALUES ('pharmacy-cl', 'Farmacia chilena', TRUE)
ON CONFLICT (id) DO UPDATE SET display_name = EXCLUDED.display_name, enabled = EXCLUDED.enabled;

INSERT INTO vertical_profile_categories (profile_id, category_key, display_name, direction, sort_order)
VALUES
    ('pharmacy-cl', 'sales', 'Ventas', 'INFLOW', 10),
    ('pharmacy-cl', 'acquirer-settlements', 'Abonos de adquirente', 'INFLOW', 20),
    ('pharmacy-cl', 'suppliers', 'Proveedores', 'OUTFLOW', 30),
    ('pharmacy-cl', 'rent', 'Arriendo', 'OUTFLOW', 40),
    ('pharmacy-cl', 'payroll', 'Remuneraciones', 'OUTFLOW', 50),
    ('pharmacy-cl', 'utilities', 'Servicios básicos', 'OUTFLOW', 60),
    ('pharmacy-cl', 'subscriptions', 'Suscripciones', 'OUTFLOW', 70),
    ('pharmacy-cl', 'bank-fees', 'Comisiones bancarias', 'OUTFLOW', 80),
    ('pharmacy-cl', 'recurring-obligations', 'Obligaciones recurrentes', 'OUTFLOW', 90)
ON CONFLICT (profile_id, category_key) DO UPDATE
SET display_name = EXCLUDED.display_name,
    direction = EXCLUDED.direction,
    sort_order = EXCLUDED.sort_order;

INSERT INTO vertical_profile_rules (profile_id, rule_key, condition_key, threshold, action_key)
VALUES
    ('pharmacy-cl', 'low-balance-warning', 'projected_balance_below_threshold', 250000.00, 'low-balance-warning'),
    ('pharmacy-cl', 'overdue-alert', 'obligations_due_before_cash_inflow', 0.00, 'overdue-alert'),
    ('pharmacy-cl', 'healthy-status', 'projected_balance_above_threshold', 750000.00, 'healthy-status')
ON CONFLICT (profile_id, rule_key) DO UPDATE
SET condition_key = EXCLUDED.condition_key,
    threshold = EXCLUDED.threshold,
    action_key = EXCLUDED.action_key;

INSERT INTO vertical_profile_obligation_templates (profile_id, obligation_key, display_name, estimated_amount, frequency, due_day_of_month)
VALUES
    ('pharmacy-cl', 'main-supplier', 'Pago a proveedor principal', 1200000.00, 'P1M', 10),
    ('pharmacy-cl', 'rent', 'Arriendo del local', 900000.00, 'P1M', 5),
    ('pharmacy-cl', 'payroll', 'Remuneraciones del equipo', 1800000.00, 'P1M', 30),
    ('pharmacy-cl', 'utilities', 'Servicios básicos', 250000.00, 'P1M', 12)
ON CONFLICT (profile_id, obligation_key) DO UPDATE
SET display_name = EXCLUDED.display_name,
    estimated_amount = EXCLUDED.estimated_amount,
    frequency = EXCLUDED.frequency,
    due_day_of_month = EXCLUDED.due_day_of_month;
