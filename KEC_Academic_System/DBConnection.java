import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection - JDBC connection manager for KEC Academic System.
 * Connects to MySQL using MySQL Connector/J driver.
 */
public final class DBConnection {
    private static final String DEFAULT_URL = 
        "jdbc:mysql://localhost:3306/kec_academic?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection] MySQL Connector/J driver not found in classpath.");
        }
    }

    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        String url = System.getenv().getOrDefault("KEC_DB_URL", DEFAULT_URL);
        String user = System.getenv().getOrDefault("KEC_DB_USER", DEFAULT_USER);
        String pass = System.getenv().getOrDefault("KEC_DB_PASSWORD", DEFAULT_PASSWORD);
        return DriverManager.getConnection(url, user, pass);
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
