package za.co.mawa.bes.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Opt-in integration checks against a disposable MySQL 8 tenant/test schema.
 *
 * Requires MAWA_SYNC_IT_JDBC_URL, MAWA_SYNC_IT_DB_USER and
 * MAWA_SYNC_IT_DB_PASSWORD. These tests write only temporary rows identified
 * by a unique "it-sync-" key and remove them in finally blocks.
 *
 * WARNING: Never point at production. They intentionally acquire database
 * row locks and trigger a duplicate-key error.
 */
@EnabledIfEnvironmentVariable(named = "MAWA_SYNC_IT_JDBC_URL", matches = ".+")
class DeviceSyncSubmissionMySqlIT {

    private Connection connection() throws SQLException {
        String url = System.getenv("MAWA_SYNC_IT_JDBC_URL");
        assertNotNull(url);
        return DriverManager.getConnection(url,
                System.getenv("MAWA_SYNC_IT_DB_USER"),
                System.getenv("MAWA_SYNC_IT_DB_PASSWORD"));
    }

    private static void insert(Connection c, String submissionId, String key)
            throws SQLException {
        String sql = "INSERT INTO device_sync_submission "
                + "(submission_id, idempotency_key, device_id, sync_time, "
                + "http_method, target_path, request_payload, status, "
                + "attempt_count, created_at, updated_at, version) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, submissionId);
            ps.setString(2, key);
            ps.setString(3, "integration-test-device");
            ps.setTimestamp(4, Timestamp.valueOf(now));
            ps.setString(5, "POST");
            ps.setString(6, "/v2/sync/payment-batches/test");
            ps.setString(7, "{}");
            ps.setString(8, "RECEIVED");
            ps.setInt(9, 0);
            ps.setTimestamp(10, Timestamp.valueOf(now));
            ps.setTimestamp(11, Timestamp.valueOf(now));
            ps.setLong(12, 0L);
            assertEquals(1, ps.executeUpdate());
        }
    }

    private static void delete(Connection c, String key) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "DELETE FROM device_sync_submission WHERE idempotency_key = ?")) {
            ps.setString(1, key);
            ps.executeUpdate();
        }
    }

    @Test
    void uniqueIdempotencyKeyRejectsAnotherSubmission() throws Exception {
        String key = "it-sync-" + UUID.randomUUID();
        try (Connection first = connection(); Connection second = connection()) {
            try {
                insert(first, UUID.randomUUID().toString(), key);
                SQLException collision = assertThrows(SQLException.class,
                        () -> insert(second, UUID.randomUUID().toString(), key));
                assertEquals(1062, collision.getErrorCode(),
                        "MySQL should enforce the unique idempotency-key index");
                try (PreparedStatement ps = first.prepareStatement(
                        "SELECT COUNT(*) FROM device_sync_submission WHERE idempotency_key = ?")) {
                    ps.setString(1, key);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals(1, rs.getInt(1));
                    }
                }
            } finally {
                delete(first, key);
            }
        }
    }

    @Test
    void forUpdateLocksSubmissionUntilOwnerCommits() throws Exception {
        String key = "it-sync-" + UUID.randomUUID();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (Connection owner = connection()) {
            insert(owner, UUID.randomUUID().toString(), key);
            owner.setAutoCommit(false);
            CountDownLatch waiting = new CountDownLatch(1);
            try {
                try (PreparedStatement lock = owner.prepareStatement(
                        "SELECT id FROM device_sync_submission WHERE idempotency_key = ? FOR UPDATE")) {
                    lock.setString(1, key);
                    try (ResultSet rs = lock.executeQuery()) {
                        assertTrue(rs.next());
                    }
                }
                Future<Integer> otherResult = executor.submit(() -> {
                    try (Connection other = connection()) {
                        other.setAutoCommit(false);
                        try (Statement setting = other.createStatement()) {
                            setting.execute("SET SESSION innodb_lock_wait_timeout = 1");
                        }
                        waiting.countDown();
                        try (PreparedStatement lock = other.prepareStatement(
                                "SELECT id FROM device_sync_submission WHERE idempotency_key = ? FOR UPDATE")) {
                            lock.setString(1, key);
                            lock.executeQuery();
                            return 0; // Unexpected: the row should still be locked.
                        } catch (SQLException expected) {
                            return expected.getErrorCode();
                        } finally {
                            other.rollback();
                        }
                    }
                });
                assertTrue(waiting.await(5, TimeUnit.SECONDS));
                assertEquals(1205, otherResult.get(8, TimeUnit.SECONDS),
                        "A second FOR UPDATE must wait for the row owner");
            } finally {
                owner.rollback();
                owner.setAutoCommit(true);
                delete(owner, key);
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
