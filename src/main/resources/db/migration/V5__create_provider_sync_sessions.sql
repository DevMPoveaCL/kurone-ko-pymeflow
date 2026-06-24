CREATE TABLE provider_sync_sessions (
    sync_id VARCHAR(80) PRIMARY KEY,
    profile_id VARCHAR(63) NOT NULL,
    provider_type VARCHAR(80) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PARTIAL'
        CHECK (status IN ('COMPLETED', 'PARTIAL', 'FAILED')),
    pages_fetched INTEGER NOT NULL DEFAULT 0 CHECK (pages_fetched >= 0),
    entries_fetched INTEGER NOT NULL DEFAULT 0 CHECK (entries_fetched >= 0),
    imported_entries INTEGER NOT NULL DEFAULT 0 CHECK (imported_entries >= 0),
    has_more_pages BOOLEAN NOT NULL DEFAULT FALSE,
    truncated BOOLEAN NOT NULL DEFAULT FALSE,
    auth_aborted BOOLEAN NOT NULL DEFAULT FALSE,
    cursor VARCHAR(512),
    last_sync_at TIMESTAMP,
    session_entry_count INTEGER NOT NULL DEFAULT 0 CHECK (session_entry_count >= 0),
    retry_after_seconds INTEGER CHECK (retry_after_seconds IS NULL OR retry_after_seconds >= 0),
    errors_json TEXT NOT NULL DEFAULT '[]',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX provider_sync_sessions_profile_provider_uk
    ON provider_sync_sessions (profile_id, provider_type);

CREATE INDEX provider_sync_sessions_profile_provider_idx
    ON provider_sync_sessions (profile_id, provider_type);

CREATE INDEX provider_sync_sessions_updated_at_idx
    ON provider_sync_sessions (updated_at);
