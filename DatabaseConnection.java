import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/oop_inventory?serverTimezone=UTC";

    private final String url;
    private final String user;
    private final String password;

    public DatabaseConnection() {
        url = getSetting("MYSQL_URL", DEFAULT_URL);
        user = getSetting("MYSQL_USER", "root");
        password = getSetting("MYSQL_PASSWORD", "");
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private String getSetting(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }
}
