import org.zstack.identity.AccountPassword;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** JDBC regression against repository DDL, using connection-local temporary tables. */
public class AccountPasswordMigrationTest {
    private static final String ORIGINAL_DATE = "2001-02-03 04:05:06";
    private static int assertions;

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || System.getenv("ZSTACK_MIGRATION_TEST_URL") == null) {
            throw new IllegalArgumentException("Pass the repository schema path and set ZSTACK_MIGRATION_TEST_URL");
        }
        String schema = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        String user = System.getenv("ZSTACK_MIGRATION_TEST_USER");
        String password = System.getenv("ZSTACK_MIGRATION_TEST_PASSWORD");
        try (Connection conn = DriverManager.getConnection(System.getenv("ZSTACK_MIGRATION_TEST_URL"),
                user == null ? "root" : user, password == null ? "" : password)) {
            for (String table : new String[]{"AccountVO", "HistoricalPasswordVO", "GlobalConfigVO"}) {
                Matcher ddl = Pattern.compile("(?ms)^CREATE TABLE `" + table + "` \\(.*?;").matcher(schema);
                require(ddl.find(), "Missing repository DDL for " + table);
                execute(conn, ddl.group().replaceFirst("CREATE TABLE", "CREATE TEMPORARY TABLE"));
            }

            execute(conn, "insert into GlobalConfigVO (name, category, value) "
                    + "values ('enableSaltedPassword', 'identity', 'false')");
            execute(conn, "insert into AccountVO (uuid, name, password, type, lastOpDate, createDate) "
                    + "values ('account', 'account', 'account-credential', 'Normal', '"
                    + ORIGINAL_DATE + "', '" + ORIGINAL_DATE + "')");
            execute(conn, "insert into HistoricalPasswordVO (uuid, password, createDate) "
                    + "values ('account', 'historical-credential', '" + ORIGINAL_DATE + "')");
            execute(conn, "insert into HistoricalPasswordVO (uuid, password, createDate) "
                    + "values ('null-record', null, '" + ORIGINAL_DATE + "')");
            String existingHash = AccountPassword.encode("already-converted");
            try (PreparedStatement stmt = conn.prepareStatement(
                    "insert into HistoricalPasswordVO (uuid, password, createDate) values (?, ?, ?)")) {
                stmt.setString(1, "encoded-record");
                stmt.setString(2, existingHash);
                stmt.setString(3, ORIGINAL_DATE);
                stmt.executeUpdate();
            }

            // Prove this schema exercises automatic timestamp updates; a simplified
            // table without ON UPDATE would let the original migration defect pass.
            execute(conn, "update HistoricalPasswordVO set password = 'control-change' where uuid = 'account'");
            require(!ORIGINAL_DATE.equals(value(conn, "select date_format(createDate, '%Y-%m-%d %H:%i:%s') "
                    + "from HistoricalPasswordVO where uuid = 'account'")), "DDL must auto-update createDate");
            execute(conn, "update HistoricalPasswordVO set password = 'historical-credential', createDate = '"
                    + ORIGINAL_DATE + "' where uuid = 'account'");

            require(AccountPasswordMigration.migrate(conn, false) == 0, "Dry-run must not update rows");
            assertUnconverted(conn);
            try {
                AccountPasswordMigration.migrate(conn, true);
                throw new AssertionError("Disabled salted writes must block apply");
            } catch (IllegalStateException expected) {
                assertUnconverted(conn);
            }

            execute(conn, "update GlobalConfigVO set value = 'true' where name = 'enableSaltedPassword'");
            require(AccountPasswordMigration.migrate(conn, true) == 2, "Convert only two legacy credentials");
            String accountHash = value(conn, "select password from AccountVO where uuid = 'account'");
            String historyHash = value(conn, "select password from HistoricalPasswordVO where uuid = 'account'");
            require(AccountPassword.isEncoded(accountHash)
                    && AccountPassword.matches("account-credential", accountHash), "Account credential must remain valid");
            require(AccountPassword.isEncoded(historyHash)
                    && AccountPassword.matches("historical-credential", historyHash), "Historical credential must remain valid");
            assertDates(conn);
            require(value(conn, "select password from HistoricalPasswordVO where uuid = 'null-record'") == null,
                    "Null password must remain null");
            require(existingHash.equals(value(conn,
                    "select password from HistoricalPasswordVO where uuid = 'encoded-record'")),
                    "Existing encoded password must not be rehashed");
            require(AccountPasswordMigration.migrate(conn, true) == 0, "Repeated migration must be idempotent");
            require(accountHash.equals(value(conn, "select password from AccountVO where uuid = 'account'")),
                    "Repeated migration must preserve account hash");
            require(historyHash.equals(value(conn, "select password from HistoricalPasswordVO where uuid = 'account'")),
                    "Repeated migration must preserve historical hash");
            assertDates(conn);
        }
        System.out.println("PASS: " + assertions + " assertions against repository DDL");
    }

    private static void assertUnconverted(Connection conn) throws SQLException {
        require("account-credential".equals(value(conn, "select password from AccountVO where uuid = 'account'")),
                "Account must remain unconverted");
        require("historical-credential".equals(value(conn,
                "select password from HistoricalPasswordVO where uuid = 'account'")), "History must remain unconverted");
        assertDates(conn);
    }

    private static void assertDates(Connection conn) throws SQLException {
        require(ORIGINAL_DATE.equals(value(conn, "select date_format(lastOpDate, '%Y-%m-%d %H:%i:%s') "
                + "from AccountVO where uuid = 'account'")), "Account lastOpDate must be preserved");
        require(ORIGINAL_DATE.equals(value(conn, "select date_format(createDate, '%Y-%m-%d %H:%i:%s') "
                + "from AccountVO where uuid = 'account'")), "Account createDate must be preserved");
        require("0".equals(value(conn, "select count(*) from HistoricalPasswordVO where createDate <> '"
                + ORIGINAL_DATE + "'")), "Every historical createDate must be preserved");
    }

    private static void execute(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private static String value(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            require(rs.next(), "Expected a result row");
            return rs.getString(1);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        assertions++;
    }
}
