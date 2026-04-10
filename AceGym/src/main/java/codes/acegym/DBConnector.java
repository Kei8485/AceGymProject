package codes.acegym;

import java.sql.*;

public class DBConnector {

    private static final String URL = "jdbc:mysql://localhost:3306/david";
    private static final String USER ="root";
    private static final String PASS = "";

    public static Connection connect() throws SQLException{
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static boolean login(String username, String password){
        String sql = "Select 1 FROM david  WHERE username = ? AND password = ? LIMIT 1";

        try(Connection con = connect();
            PreparedStatement ps = con.prepareStatement(sql)){

            ps.setString(1, username);
            ps.setString(2,password);

            try(ResultSet rs = ps.executeQuery()){
                return rs.next();
            }
        } catch (SQLException e){
            System.out.println("Login failed: " + e.getMessage());
            return false;
        }
    }

}
