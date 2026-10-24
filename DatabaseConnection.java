import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public class DatabaseConnection {
    // Wrapper to allow DriverManager to recognize drivers loaded via a custom ClassLoader
    private static class DriverShim implements Driver {
        private final Driver driver;
        public DriverShim(Driver d) { this.driver = d; }
        public boolean acceptsURL(String u) throws SQLException { return this.driver.acceptsURL(u); }
        public Connection connect(String u, Properties p) throws SQLException { return this.driver.connect(u, p); }
        public int getMajorVersion() { return this.driver.getMajorVersion(); }
        public int getMinorVersion() { return this.driver.getMinorVersion(); }
        public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException { return this.driver.getPropertyInfo(u, p); }
        public boolean jdbcCompliant() { return this.driver.jdbcCompliant(); }
        public Logger getParentLogger() throws SQLFeatureNotSupportedException { return this.driver.getParentLogger(); }
    }

    private static boolean driverRegistered = false;

    private static synchronized void ensureDriverLoaded() {
        if (driverRegistered) {
            return;
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            driverRegistered = true;
            return;
        } catch (ClassNotFoundException ignored) {}

        // Fallback: Locate mysql-connector jar in current dir and load dynamically if classpath omitted it
        try {
            File dir = new File(".");
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().endsWith(".jar") && f.getName().toLowerCase().contains("mysql")) {
                        URLClassLoader ucl = new URLClassLoader(new URL[]{ f.toURI().toURL() });
                        Driver d = (Driver) Class.forName("com.mysql.cj.jdbc.Driver", true, ucl)
                                .getDeclaredConstructor().newInstance();
                        DriverManager.registerDriver(new DriverShim(d));
                        driverRegistered = true;
                        return;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Notice: Could not dynamically load MySQL JDBC driver: " + e.getMessage());
        }
    }

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/oop_inventory?serverTimezone=UTC&createDatabaseIfNotExist=true";

    private final String url;
    private final String user;
    private final String password;

    public DatabaseConnection() {
        ensureDriverLoaded();
        Properties props = loadProperties();
        this.url = getSetting(props, "MYSQL_URL", "mysql.url", DEFAULT_URL);
        this.user = getSetting(props, "MYSQL_USER", "mysql.user", "root");
        this.password = getSetting(props, "MYSQL_PASSWORD", "mysql.password", "");
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        File propFile = new File("db.properties");
        if (propFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propFile)) {
                properties.load(fis);
            } catch (IOException ignored) {}
        } else {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("db.properties")) {
                if (is != null) {
                    properties.load(is);
                }
            } catch (IOException ignored) {}
        }
        return properties;
    }

    private String getSetting(Properties props, String envName, String propName, String defaultValue) {
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue.trim();
        }
        String propValue = props.getProperty(propName);
        if (propValue != null && !propValue.trim().isEmpty()) {
            return propValue.trim();
        }
        return defaultValue;
    }

    public Connection open() throws SQLException {
        ensureDriverLoaded();
        return DriverManager.getConnection(url, user, password);
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }
}
