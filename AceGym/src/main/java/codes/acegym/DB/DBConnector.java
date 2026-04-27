package codes.acegym.DB;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * DBConnector — with a lightweight connection pool.
 *
 * WHY:  Every DriverManager.getConnection() call opens a new TCP socket to
 *       MySQL (~100–200 ms). With POOL_SIZE pre-warmed connections the first
 *       pool.poll() is instant (< 1 ms).
 *
 * HOW:  Callers use the same try-with-resources pattern as before.
 *       connect() returns a Proxy whose close() returns the raw connection
 *       back to the pool instead of destroying it.
 */
public class DBConnector {

    // ── Keep JDBC URL params that let MySQL cache prepared statements ────────
    private static final String URL =
            "jdbc:mysql://localhost:3306/acefitnessgymdb" +
                    "?useSSL=false" +
                    "&allowPublicKeyRetrieval=true" +
                    "&cachePrepStmts=true" +
                    "&prepStmtCacheSize=250" +
                    "&prepStmtCacheSqlLimit=2048" +
                    "&autoReconnect=true" +
                    "&failOverReadOnly=false" +
                    "&serverTimezone=UTC";

    private static final String USER      = "root";
    private static final String PASS      = "";
    private static final int    POOL_SIZE = 6;   // tune as needed

    // ── The pool ─────────────────────────────────────────────────────────────
    private static final LinkedBlockingQueue<Connection> POOL =
            new LinkedBlockingQueue<>(POOL_SIZE);

    // Pre-warm all connections at class-load time (happens once at startup)
    static {
        for (int i = 0; i < POOL_SIZE; i++) {
            try {
                POOL.offer(rawConnect());
            } catch (SQLException e) {
                System.err.println("[DBConnector] pool warm-up failed: " + e.getMessage());
            }
        }
        System.out.println("[DBConnector] Pool ready — " + POOL.size() + " connections.");
    }

    // ── Raw (real) connection — used by pool only ────────────────────────────
    private static Connection rawConnect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // ── Public API — unchanged signature ────────────────────────────────────
    /**
     * Borrow a connection from the pool (instant if one is available).
     * Use inside try-with-resources; close() returns it to the pool.
     */
    public static Connection connect() throws SQLException {

        // 1. Try to grab a pooled connection
        Connection raw = POOL.poll();

        // 2. If pool was empty, create a fresh one on demand
        if (raw == null) {
            raw = rawConnect();
        } else {
            // 3. Validate — stale connections happen after long idle periods
            try {
                if (raw.isClosed() || !raw.isValid(1)) {
                    raw = rawConnect();
                }
            } catch (SQLException e) {
                raw = rawConnect();
            }
        }

        final Connection finalRaw = raw;

        // 4. Wrap in a Proxy so close() returns to pool instead of destroying
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        // Return to pool rather than closing
                        try {
                            if (!finalRaw.isClosed()) {
                                POOL.offer(finalRaw);
                            }
                        } catch (SQLException ignored) {}
                        return null;
                    }
                    try {
                        return method.invoke(finalRaw, args);
                    } catch (InvocationTargetException ex) {
                        throw ex.getCause();   // unwrap so callers see SQLException
                    }
                }
        );
    }

    // ── Login helper — unchanged ─────────────────────────────────────────────
    public static boolean login(String username, String password) {
        String sql =
                "SELECT 1 FROM UserAccountsTable " +
                        "WHERE Username = ? AND Password = ? LIMIT 1";

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Login Error: " + e.getMessage());
            return false;
        }
    }
}