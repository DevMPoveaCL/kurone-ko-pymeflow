package com.kuroneko.pymeflow.infrastructure.persistence;

import com.kuroneko.pymeflow.application.port.out.SyncSessionPort;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class JdbcSyncSessionAdapter implements SyncSessionPort {
    private static final String SELECT_COLUMNS = """
            select sync_id, profile_id, provider_type, status, pages_fetched, entries_fetched, imported_entries,
                   has_more_pages, truncated, auth_aborted, cursor, last_sync_at, session_entry_count,
                   retry_after_seconds, errors_json
            from provider_sync_sessions
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ProviderErrorJsonMapper errorJsonMapper;

    public JdbcSyncSessionAdapter(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, new ProviderErrorJsonMapper());
    }

    JdbcSyncSessionAdapter(JdbcTemplate jdbcTemplate, ProviderErrorJsonMapper errorJsonMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.errorJsonMapper = errorJsonMapper;
    }

    @Override
    public String syncId(ProfileId profileId, String providerType) {
        var key = sessionKey(profileId, providerType);
        insertSessionIfMissing(key);
        return jdbcTemplate.queryForObject("""
                select sync_id
                from provider_sync_sessions
                where profile_id = ? and provider_type = ?
                """, String.class, key.profileId().value(), key.providerType());
    }

    private void insertSessionIfMissing(SessionKey key) {
        var now = Timestamp.from(Instant.now());
        if (isPostgreSQL()) {
            jdbcTemplate.update("""
                            insert into provider_sync_sessions
                            (sync_id, profile_id, provider_type, status, created_at, updated_at)
                            values (?, ?, ?, ?, ?, ?)
                            on conflict (profile_id, provider_type) do nothing
                            """,
                    newSyncId(),
                    key.profileId().value(),
                    key.providerType(),
                    SyncStatus.PARTIAL.name(),
                    now,
                    now
            );
            return;
        }
        try {
            jdbcTemplate.update("""
                            insert into provider_sync_sessions
                            (sync_id, profile_id, provider_type, status, created_at, updated_at)
                            values (?, ?, ?, ?, ?, ?)
                            """,
                    newSyncId(),
                    key.profileId().value(),
                    key.providerType(),
                    SyncStatus.PARTIAL.name(),
                    now,
                    now
            );
        } catch (DuplicateKeyException ignored) {
            // Non-PostgreSQL tests can keep the historical idempotent insert fallback.
        }
    }

    private boolean isPostgreSQL() {
        return Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) connection ->
                connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("PostgreSQL")));
    }

    @Override
    public Optional<String> findCursor(ProfileId profileId, String providerType) {
        var key = sessionKey(profileId, providerType);
        return jdbcTemplate.query("""
                        select cursor
                        from provider_sync_sessions
                        where profile_id = ? and provider_type = ?
                        """,
                (rs, rowNum) -> Optional.ofNullable(rs.getString("cursor")),
                key.profileId().value(),
                key.providerType()
        ).stream().findFirst().flatMap(value -> value);
    }

    @Override
    public void saveCursor(ProfileId profileId, String providerType, String cursor) {
        var key = sessionKey(profileId, providerType);
        syncId(key.profileId(), key.providerType());
        var now = Instant.now();
        jdbcTemplate.update("""
                        update provider_sync_sessions
                        set cursor = ?, last_sync_at = ?, updated_at = ?
                        where profile_id = ? and provider_type = ?
                        """,
                normalizedCursor(cursor).orElse(null),
                Timestamp.from(now),
                Timestamp.from(now),
                key.profileId().value(),
                key.providerType()
        );
    }

    @Override
    public Optional<Instant> lastSyncAt(ProfileId profileId, String providerType) {
        var key = sessionKey(profileId, providerType);
        return jdbcTemplate.query("""
                        select last_sync_at
                        from provider_sync_sessions
                        where profile_id = ? and provider_type = ?
                        """,
                (rs, rowNum) -> instantOrNull(rs, "last_sync_at"),
                key.profileId().value(),
                key.providerType()
        ).stream().findFirst();
    }

    @Override
    public void incrementEntryCount(ProfileId profileId, String providerType, int entryCount) {
        if (entryCount < 0) {
            throw new IllegalArgumentException("Entry count must not be negative");
        }
        var key = sessionKey(profileId, providerType);
        syncId(key.profileId(), key.providerType());
        jdbcTemplate.update("""
                        update provider_sync_sessions
                        set session_entry_count = session_entry_count + ?, updated_at = ?
                        where profile_id = ? and provider_type = ?
                        """,
                entryCount,
                Timestamp.from(Instant.now()),
                key.profileId().value(),
                key.providerType()
        );
    }

    @Override
    public int entryCount(ProfileId profileId, String providerType) {
        var key = sessionKey(profileId, providerType);
        var count = jdbcTemplate.query("""
                        select session_entry_count
                        from provider_sync_sessions
                        where profile_id = ? and provider_type = ?
                        """,
                (rs, rowNum) -> rs.getInt("session_entry_count"),
                key.profileId().value(),
                key.providerType()
        );
        return count.stream().findFirst().orElse(0);
    }

    @Override
    public void recordReport(SyncSessionSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("Sync session snapshot is required");
        }
        syncId(snapshot.profileId(), snapshot.providerType());
        jdbcTemplate.update("""
                        update provider_sync_sessions
                        set status = ?, pages_fetched = ?, entries_fetched = ?, imported_entries = ?,
                            has_more_pages = ?, truncated = ?, auth_aborted = ?, cursor = ?, last_sync_at = ?,
                            session_entry_count = ?, retry_after_seconds = ?, errors_json = ?, updated_at = ?
                        where sync_id = ?
                        """,
                snapshot.status().name(),
                snapshot.pagesFetched(),
                snapshot.entriesFetched(),
                snapshot.importedEntries(),
                snapshot.hasMorePages(),
                snapshot.truncated(),
                snapshot.authAborted(),
                snapshot.cursor().orElse(null),
                snapshot.lastSyncAt().map(Timestamp::from).orElse(null),
                snapshot.sessionEntryCount(),
                snapshot.retryAfterSeconds().orElse(null),
                errorJsonMapper.serialize(snapshot.errors()),
                Timestamp.from(Instant.now()),
                snapshot.syncId()
        );
    }

    @Override
    public Optional<SyncSessionSnapshot> findBySyncId(String syncId) {
        if (syncId == null || syncId.isBlank()) {
            return Optional.empty();
        }
        return jdbcTemplate.query(
                SELECT_COLUMNS + " where sync_id = ?",
                (rs, rowNum) -> mapRow(rs),
                syncId.trim()
        ).stream().findFirst();
    }

    private SyncSessionSnapshot mapRow(ResultSet rs) throws SQLException {
        return new SyncSessionSnapshot(
                rs.getString("sync_id"),
                new ProfileId(rs.getString("profile_id")),
                rs.getString("provider_type"),
                SyncStatus.valueOf(rs.getString("status")),
                rs.getInt("pages_fetched"),
                rs.getInt("entries_fetched"),
                rs.getInt("imported_entries"),
                rs.getBoolean("has_more_pages"),
                rs.getBoolean("truncated"),
                rs.getBoolean("auth_aborted"),
                Optional.ofNullable(rs.getString("cursor")),
                Optional.ofNullable(instantOrNull(rs, "last_sync_at")),
                rs.getInt("session_entry_count"),
                errorJsonMapper.deserialize(rs.getString("errors_json")),
                optionalInteger(rs, "retry_after_seconds"),
                Durability.DURABLE
        );
    }

    private static SessionKey sessionKey(ProfileId profileId, String providerType) {
        return new SessionKey(profileId, providerType);
    }

    private static Optional<String> normalizedCursor(String cursor) {
        return Optional.ofNullable(cursor).map(String::trim).filter(value -> !value.isBlank());
    }

    private static Instant instantOrNull(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Optional<Integer> optionalInteger(ResultSet rs, String column) throws SQLException {
        var value = rs.getObject(column, Integer.class);
        return Optional.ofNullable(value);
    }

    private static String newSyncId() {
        return "sync-" + UUID.randomUUID();
    }

    private record SessionKey(ProfileId profileId, String providerType) {
        private SessionKey {
            if (profileId == null) {
                throw new IllegalArgumentException("Profile id is required");
            }
            if (providerType == null || providerType.isBlank()) {
                throw new IllegalArgumentException("Provider type is required");
            }
            providerType = providerType.trim();
        }
    }
}
