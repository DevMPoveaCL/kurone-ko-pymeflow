CREATE TABLE IF NOT EXISTS vertical_profiles (
    id VARCHAR(63) PRIMARY KEY,
    display_name VARCHAR(120) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS vertical_profile_categories (
    profile_id VARCHAR(63) NOT NULL REFERENCES vertical_profiles(id),
    category_key VARCHAR(80) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    sort_order INTEGER NOT NULL,
    PRIMARY KEY (profile_id, category_key)
);

CREATE TABLE IF NOT EXISTS vertical_profile_rules (
    profile_id VARCHAR(63) NOT NULL REFERENCES vertical_profiles(id),
    rule_key VARCHAR(100) NOT NULL,
    condition_key VARCHAR(120) NOT NULL,
    threshold NUMERIC(18, 2) NOT NULL,
    action_key VARCHAR(100) NOT NULL,
    PRIMARY KEY (profile_id, rule_key)
);

CREATE TABLE IF NOT EXISTS vertical_profile_obligation_templates (
    profile_id VARCHAR(63) NOT NULL REFERENCES vertical_profiles(id),
    obligation_key VARCHAR(100) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    estimated_amount NUMERIC(18, 2) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    due_day_of_month INTEGER NOT NULL CHECK (due_day_of_month BETWEEN 1 AND 31),
    PRIMARY KEY (profile_id, obligation_key)
);
