package me.pectics.securetransfer.core;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

/**
 * 基于 SQLite 的防重放存储。
 * <p>采用 {@code journal_mode=WAL} + {@code synchronous=FULL}:每次 COMMIT 都会 fsync,
 * 因此 {@code recordIfAbsent} 返回 {@code true} 时记录已落盘 —— 满足"落盘才算验签成功"。
 * 唯一主键({@code jti})保证了并发下的原子去重。</p>
 */
public final class SqliteReplayStore implements ReplayStore {

    private final Connection connection;
    private final Object writeLock = new Object();

    public SqliteReplayStore(final Path databaseFile, final int busyTimeoutMillis) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.toAbsolutePath());
        try (Statement statement = this.connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA synchronous=FULL");
            statement.execute("PRAGMA busy_timeout=" + busyTimeoutMillis);
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS used_tokens (" + "jti TEXT PRIMARY KEY, exp INTEGER NOT NULL)");
        }
        this.connection.setAutoCommit(true);
    }

    @Override
    public boolean recordIfAbsent(final String jwtId, final Instant expiresAt) {
        synchronized (this.writeLock) {
            try (PreparedStatement statement =
                    this.connection.prepareStatement("INSERT OR IGNORE INTO used_tokens(jti, exp) VALUES(?, ?)")) {
                statement.setString(1, jwtId);
                statement.setLong(2, expiresAt.getEpochSecond());
                return statement.executeUpdate() == 1;
            } catch (final SQLException e) {
                throw new IllegalStateException("记录 jti 失败", e);
            }
        }
    }

    @Override
    public void prune(final Instant now) {
        synchronized (this.writeLock) {
            try (PreparedStatement statement =
                    this.connection.prepareStatement("DELETE FROM used_tokens WHERE exp < ?")) {
                statement.setLong(1, now.getEpochSecond());
                statement.executeUpdate();
            } catch (final SQLException e) {
                throw new IllegalStateException("清理过期 jti 失败", e);
            }
        }
    }

    @Override
    public void close() {
        try {
            this.connection.close();
        } catch (final SQLException ignored) {
            // 关闭失败无需上抛
        }
    }
}
