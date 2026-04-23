package codes.acegym.DB;

import java.sql.*;

public class DBConnector {

    private static final String URL = "jdbc:mysql://localhost:3306/acefitnessgymdb";
    private static final String USER = "root";
    private static final String PASS = "";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static boolean login(String username, String password) {
        String sql = "SELECT 1 FROM UserAccountsTable WHERE Username = ? AND Password = ? LIMIT 1";

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // Returns true if a match is found
            }
        } catch (SQLException e) {
            // It's helpful to see the specific error during development
            System.err.println("Login Error: " + e.getMessage());
            return false;
        }
    }
}