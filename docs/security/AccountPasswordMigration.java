import org.zstack.identity.AccountPassword;

import java.io.Console;
import java.sql.*;
import java.util.Arrays;

/** Offline maintenance tool; not part of the running management server. */
public class AccountPasswordMigration {
    public static void main(String[] args) throws Exception {
        boolean apply = args.length == 1 && "--apply".equals(args[0]);
        if (args.length > 0 && !apply) {
            throw new IllegalArgumentException("Use no arguments for dry-run, or --apply after upgrading all nodes and taking a database backup");
        }
        Console console = System.console();
        String url = System.getenv("ZSTACK_DB_URL");
        if (console == null || url == null) {
            throw new IllegalArgumentException("Run in a terminal with ZSTACK_DB_URL set; credentials are read from the terminal");
        }
        String user = console.readLine("Database user: ");
        char[] password = console.readPassword("Database password: ");
        try (Connection conn = DriverManager.getConnection(url, user, new String(password))) {
            if (apply && !"UPGRADED_AND_BACKED_UP".equals(console.readLine(
                    "After verifying every management node supports the new format and a backup exists, type UPGRADED_AND_BACKED_UP: "))) {
                throw new IllegalStateException("No records changed");
            }
            migrate(conn, apply);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    public static int migrate(Connection conn, boolean apply) throws SQLException {
        if (!conn.getAutoCommit()) {
            throw new IllegalArgumentException("A dedicated auto-commit connection is required");
        }
        if (apply) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "select value from GlobalConfigVO where category = 'identity' and name = 'enableSaltedPassword'");
                 ResultSet rs = stmt.executeQuery()) {
                if (!rs.next() || !"true".equals(rs.getString(1))) {
                    throw new IllegalStateException("Enable salted writes only after upgrading all management nodes; no records changed");
                }
            }
        }
        return migrateTable(conn, "AccountVO", "uuid", apply)
                + migrateTable(conn, "HistoricalPasswordVO", "id", apply);
    }

    private static int migrateTable(Connection conn, String table, String key, boolean apply) throws SQLException {
        int legacy = 0, updated = 0, concurrent = 0;
        // Table/key come only from the two fixed calls above, never user input.
        // Preserve both tables' ON UPDATE timestamps when re-encoding credentials.
        String sql = "update " + table + " set password = ?"
                + ("AccountVO".equals(table) ? ", lastOpDate = lastOpDate" : ", createDate = createDate")
                + " where " + key + " = ? and binary password = binary ?";
        try (PreparedStatement select = conn.prepareStatement("select " + key + ", password from " + table);
             PreparedStatement update = conn.prepareStatement(sql);
             ResultSet rs = select.executeQuery()) {
            while (rs.next()) {
                String old = rs.getString(2);
                if (old == null || AccountPassword.isEncoded(old)) {
                    continue;
                }
                legacy++;
                if (!apply) {
                    continue;
                }
                String encoded = AccountPassword.encode(old);
                if (!AccountPassword.matches(old, encoded)) {
                    throw new IllegalStateException("Hash verification failed; migration stopped");
                }
                update.setString(1, encoded);
                update.setObject(2, rs.getObject(1));
                update.setString(3, old);
                if (update.executeUpdate() == 1) {
                    updated++;
                } else {
                    concurrent++;
                }
            }
        }
        System.out.println(table + ": legacy=" + legacy + ", updated=" + updated + ", changed-concurrently=" + concurrent);
        return updated;
    }
}
