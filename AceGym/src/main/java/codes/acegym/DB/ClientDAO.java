package codes.acegym.DB;

import codes.acegym.Objects.Client;
import codes.acegym.Objects.MemberRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;

public class ClientDAO {

    // ── Base SELECT — joins MembershipTable for ClientType (most recent membership),
    //    and pulls ContactNumber directly from ClientTable.
    private static final String BASE_SELECT =
            "SELECT c.ClientID, c.FirstName, c.LastName, " +
                    "       COALESCE(c.ContactNumber, '')  AS ContactNumber, " +
                    "       COALESCE(ct.ClientType, 'Non Member') AS ClientType " +
                    "FROM ClientTable c " +
                    "LEFT JOIN MembershipTable m ON m.ClientID = c.ClientID " +
                    "    AND m.MembershipID = (" +
                    "        SELECT MembershipID FROM MembershipTable " +
                    "        WHERE ClientID = c.ClientID " +
                    "        ORDER BY DateApplied DESC LIMIT 1" +
                    "    ) " +
                    "LEFT JOIN ClientTypeTable ct ON m.ClientTypeID = ct.ClientTypeID";

    // ────────────────────────────────────────────────────────────────────────────
    // ── UPDATE CLIENT INFO — editable fields only ───────────────────────────────
    // ────────────────────────────────────────────────────────────────────────────
    /**
     * Updates FirstName, LastName, ContactNumber, and ClientEmail
     * for the given ClientID.
     *
     * @return true if exactly one row was updated, false otherwise.
     */
    public static boolean updateClientInfo(int clientID,
                                           String firstName,
                                           String lastName,
                                           String contact,
                                           String email) {
        String sql =
                "UPDATE ClientTable " +
                        "SET FirstName = ?, LastName = ?, ContactNumber = ?, ClientEmail = ? " +
                        "WHERE ClientID = ?";

        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, firstName.trim());
            ps.setString(2, lastName.trim());
            ps.setString(3, contact.trim());
            ps.setString(4, email.trim());
            ps.setInt(5, clientID);

            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // ── MEMBER ROWS — rich query for the ViewMembers TableView ─────────────────
    // ────────────────────────────────────────────────────────────────────────────
    /**
     * Returns one MemberRow per client, combining:
     *  - Most-recent membership dates + status
     *  - Most-recent receipt → payment period + plan expiry + plan status
     *  - Assigned coach name + training category
     */
    public static ObservableList<MemberRow> getMemberRows() {
        ObservableList<MemberRow> list = FXCollections.observableArrayList();

        String sql =
                "SELECT " +
                        "    c.ClientID, " +
                        "    CONCAT(c.FirstName, ' ', c.LastName)   AS FullName, " +
                        "    COALESCE(c.ContactNumber, '')           AS ContactNumber, " +
                        "    COALESCE(c.ClientEmail,  '')            AS ClientEmail, " +
                        "    COALESCE(ct.ClientType, 'Non Member')   AS ClientType, " +

                        // ── Membership ──────────────────────────────────────────────────
                        "    DATE_FORMAT(m.DateApplied, '%b %d, %Y') AS MembershipApplied, " +
                        "    DATE_FORMAT(m.DateExpired, '%b %d, %Y') AS MembershipExpired, " +
                        "    CASE " +
                        "        WHEN m.DateExpired IS NULL          THEN 'None' " +
                        "        WHEN m.DateExpired >= CURDATE()     THEN 'Active' " +
                        "        ELSE                                     'Expired' " +
                        "    END AS MembershipStatus, " +

                        // ── Last payment-period plan ────────────────────────────────────
                        "    COALESCE(pp.PaymentPeriod, '—')         AS LastPaymentPeriod, " +
                        "    DATE_FORMAT(r_last.PaymentDate, '%b %d, %Y') AS LastPaymentDate, " +
                        "    DATE_FORMAT(" +
                        "        DATE_ADD(r_last.PaymentDate, INTERVAL pp.Days DAY), " +
                        "        '%b %d, %Y'" +
                        "    )                                        AS PlanExpiry, " +
                        "    CASE " +
                        "        WHEN r_last.PaymentDate IS NULL     THEN 'No Record' " +
                        "        WHEN DATE_ADD(r_last.PaymentDate, INTERVAL pp.Days DAY) >= CURDATE() " +
                        "                                            THEN 'Active' " +
                        "        ELSE                                     'Expired' " +
                        "    END AS PlanStatus, " +

                        // ── Coach / training ────────────────────────────────────────────
                        "    COALESCE(CONCAT(s.StaffFirstName, ' ', s.StaffLastName), 'Unassigned') AS CoachName, " +
                        "    COALESCE(tt.TrainingCategory, 'None')   AS TrainingCategory, " +
                        "    COALESCE(r_last.TotalPayment, 0)        AS LastPaymentAmount " +

                        "FROM ClientTable c " +

                        // Most-recent membership
                        "LEFT JOIN MembershipTable m " +
                        "       ON m.ClientID = c.ClientID " +
                        "      AND m.MembershipID = (" +
                        "              SELECT MembershipID FROM MembershipTable " +
                        "              WHERE ClientID = c.ClientID " +
                        "              ORDER BY DateApplied DESC LIMIT 1" +
                        "          ) " +
                        "LEFT JOIN ClientTypeTable ct ON m.ClientTypeID = ct.ClientTypeID " +

                        // Most-recent receipt
                        "LEFT JOIN ReceiptTable r_last " +
                        "       ON r_last.ReceiptID = (" +
                        "              SELECT ReceiptID FROM ReceiptTable " +
                        "              WHERE ClientID = c.ClientID " +
                        "              ORDER BY PaymentDate DESC LIMIT 1" +
                        "          ) " +
                        "LEFT JOIN RateTable      ra ON r_last.RateID          = ra.RateID " +
                        "LEFT JOIN PaymentPeriodTable pp ON ra.PaymentPeriodID = pp.PaymentPeriodID " +
                        "LEFT JOIN TrainingTypeTable  tt ON ra.TrainingTypeID  = tt.TrainingTypeID " +

                        // Coach from the receipt's assignment
                        "LEFT JOIN ClientStaffAssignmentTable csa " +
                        "       ON csa.ClientStaffAssignmentID = r_last.ClientStaffAssignmentID " +
                        "LEFT JOIN StaffTable s ON csa.StaffID = s.StaffID " +

                        "ORDER BY c.FirstName";

        try (Connection con = DBConnector.connect();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new MemberRow(
                        rs.getInt("ClientID"),
                        rs.getString("FullName"),
                        rs.getString("ContactNumber"),
                        rs.getString("ClientEmail"),
                        rs.getString("ClientType"),
                        rs.getString("MembershipApplied"),
                        rs.getString("MembershipExpired"),
                        rs.getString("MembershipStatus"),
                        rs.getString("LastPaymentPeriod"),
                        rs.getString("LastPaymentDate"),
                        rs.getString("PlanExpiry"),
                        rs.getString("PlanStatus"),
                        rs.getString("CoachName"),
                        rs.getString("TrainingCategory"),
                        rs.getDouble("LastPaymentAmount")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ── All clients ──────────────────────────────────────────────────────────
    public static ObservableList<Client> getAllClients() {
        ObservableList<Client> list = FXCollections.observableArrayList();
        String sql = BASE_SELECT + " ORDER BY c.FirstName";
        try (Connection con = DBConnector.connect();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ── Clients NOT yet assigned to the given coach ──────────────────────────
    public static ObservableList<Client> getUnassignedClients(int staffID) {
        ObservableList<Client> list = FXCollections.observableArrayList();
        String sql = BASE_SELECT +
                " WHERE c.ClientID NOT IN (" +
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
        String sql = BASE_SELECT + " WHERE c.ClientID = ?";
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
                "",
                rs.getString("ContactNumber"),
                rs.getString("ClientType")
        );
    }
}