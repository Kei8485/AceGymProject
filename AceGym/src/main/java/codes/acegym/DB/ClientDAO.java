package codes.acegym.DB;

import codes.acegym.Objects.Client;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;

public class ClientDAO {

    // ── All clients ──────────────────────────────────────────────────────────
    public static ObservableList<Client> getAllClients() {
        ObservableList<Client> list = FXCollections.observableArrayList();
        String sql =
                "SELECT c.ClientID, c.FirstName, c.LastName, " +
                        "       COALESCE(ct.ClientType, 'Member') AS ClientType " +
                        "FROM ClientTable c " +
                        "LEFT JOIN ClientTypeTable ct ON c.ClientTypeID = ct.ClientTypeID " +
                        "ORDER BY c.FirstName";
        try (Connection con = DBConnector.connect();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ── Clients NOT yet assigned to the given coach ──────────────────────────
    public static ObservableList<Client> getUnassignedClients(int staffID) {
        ObservableList<Client> list = FXCollections.observableArrayList();
        String sql =
                "SELECT c.ClientID, c.FirstName, c.LastName, " +
                        "       COALESCE(ct.ClientType, 'Member') AS ClientType " +
                        "FROM ClientTable c " +
                        "LEFT JOIN ClientTypeTable ct ON c.ClientTypeID = ct.ClientTypeID " +
                        "WHERE c.ClientID NOT IN (" +
                        "    SELECT ClientID FROM ClientStaffAssignmentTable WHERE StaffID = ?" +
                        ") ORDER BY c.FirstName";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, staffID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ── Single client by ID ──────────────────────────────────────────────────
    public static Client getClientByID(int clientID) {
        String sql =
                "SELECT c.ClientID, c.FirstName, c.LastName, " +
                        "       COALESCE(ct.ClientType, 'Member') AS ClientType " +
                        "FROM ClientTable c " +
                        "LEFT JOIN ClientTypeTable ct ON c.ClientTypeID = ct.ClientTypeID " +
                        "WHERE c.ClientID = ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clientID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // ── Row mapper ────────────────────────────────────────────────────────────
    private static Client mapRow(ResultSet rs) throws SQLException {
        return new Client(
                rs.getInt("ClientID"),
                rs.getString("FirstName"),
                rs.getString("LastName"),
                "",   // Email — not in ClientTable, kept for Client constructor compat
                "",   // Contact — not in ClientTable
                rs.getString("ClientType")
        );
    }
}