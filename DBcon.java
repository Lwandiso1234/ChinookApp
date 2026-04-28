import java.sql.*;

public class DBcon {
    public static void main(String[] args) {
        try {
            // Loading the MariaDB driver
            Class.forName("org.mariadb.jdbc.Driver");
            /*connection details from environment variables*/
            String host = System.getenv("CHINOOK_DB_HOST");
            String port = System.getenv("CHINOOK_DB_PORT");
            String name = System.getenv("CHINOOK_DB_NAME");
            String user = System.getenv("CHINOOK_DB_USERNAME");
            String pass = System.getenv("CHINOOK_DB_PASSWORD");

            String url = "jdbc:mariadb://" + host + ":" + port + "/" + name;
            System.out.println("Trying to connect to: " + url);

            // Connect
            Connection conn = DriverManager.getConnection(url, user, pass);
            System.out.println("SUCCESS: Connected to the database!");
            conn.close();

        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}