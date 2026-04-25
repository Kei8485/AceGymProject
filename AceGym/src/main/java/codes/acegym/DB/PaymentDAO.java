package codes.acegym.DB;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;

/**
 * Data-access layer for the Add-Payment screen.
 * Handles training types, payment periods, rates, payment types,
 * coach assignments, and receipt insertion.
 */
public class PaymentDAO {

    // ─────────────────────────────────────────────────────────────────────────
    // LOOK-UP TABLES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns [{TrainingTypeID, TrainingCategory}] ordered alphabetically.
     */
    public static ObservableList<String[]> getTrainingTypes() {
        ObservableList<String[]> list = FXCollections.observableArrayList();
        String sql = "SELECT TrainingTypeID, TrainingCategory FROM TrainingTypeTable ORDER BY TrainingCategory";
        try (Connection con = DBConnector.connect();
             Statement st  = con.createStatement();
             ResultSet rs  = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new String[]{
                        String.valueOf(rs.getInt("TrainingTypeID")),
                        rs.getString("TrainingCategory")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /**
     * Returns [{PaymentPeriodID, PaymentPeriod, Days}] ordered by Days (shortest first).
     */
    public static ObservableList<String[]> getPaymentPeriods() {
        ObservableList<String[]> list = FXCollections.observableArrayList();
        String sql = "SELECT PaymentPeriodID, PaymentPeriod, Days FROM PaymentPeriodTable ORDER BY Days";
        try (Connection con = DBConnector.connect();
             Statement st  = con.createStatement();
             ResultSet rs  = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new String[]{
                        String.valueOf(rs.getInt("PaymentPeriodID")),
                        rs.getString("PaymentPeriod"),
                        String.valueOf(rs.getInt("Days"))
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /**
     * Returns [{PaymentTypeID, PaymentType}] ordered alphabetically.
     */
    public static ObservableList<String[]> getPaymentTypes() {
        ObservableList<String[]> list = FXCollections.observableArrayList();
        String sql = "SELECT PaymentTypeID, PaymentType FROM PaymentTypeTable ORDER BY PaymentType";
        try (Connection con = DBConnector.connect();
             Statement st  = con.createStatement();
             ResultSet rs  = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new String[]{
                        String.valueOf(rs.getInt("PaymentTypeID")),
                        rs.getString("PaymentType")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns {RateID, FinalPrice} for the given combination, or null.
     * BUG FIX: was querying non-existent column "Rate"; correct column is "FinalPrice".
     */
    public static String[] getRate(int trainingTypeID, int paymentPeriodID, int clientTypeID) {
        String sql =
                "SELECT RateID, FinalPrice FROM RateTable " +
                        "WHERE TrainingTypeID = ? AND PaymentPeriodID = ? AND ClientTypeID = ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, trainingTypeID);
            ps.setInt(2, paymentPeriodID);
            ps.setInt(3, clientTypeID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                            String.valueOf(rs.getInt("RateID")),
                            String.valueOf(rs.getDouble("FinalPrice"))
                    };
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTIVE PLAN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the most recent non-expired, non-"None" plan for a client as:
     * [0] TrainingTypeID  [1] TrainingCategory  [2] PaymentPeriodID
     * [3] PaymentPeriod   [4] FinalPrice         [5] ExpiryDate (yyyy-MM-dd)
     * [6] ClientTypeID
     * Returns null if the client has no active plan.
     */
    public static String[] getActivePlan(int clientID) {
        String sql =
                "SELECT " +
                        "    tt.TrainingTypeID, tt.TrainingCategory, " +
                        "    pp.PaymentPeriodID, pp.PaymentPeriod, " +
                        "    ra.FinalPrice, ra.ClientTypeID, " +
                        "    DATE_FORMAT(" +
                        "        DATE_ADD(DATE(r.PaymentDate), INTERVAL pp.Days DAY), " +
                        "        '%Y-%m-%d'" +
                        "    ) AS ExpiryDate " +
                        "FROM ReceiptTable r " +
                        "JOIN RateTable ra          ON r.RateID          = ra.RateID " +
                        "JOIN TrainingTypeTable tt  ON ra.TrainingTypeID = tt.TrainingTypeID " +
                        "JOIN PaymentPeriodTable pp ON ra.PaymentPeriodID = pp.PaymentPeriodID " +
                        "WHERE r.ClientID = ? " +
                        "  AND tt.TrainingTypeID != 4 " +    // exclude the "None" training type
                        "  AND DATE_ADD(DATE(r.PaymentDate), INTERVAL pp.Days DAY) >= CURDATE() " +
                        "ORDER BY r.PaymentDate DESC " +
                        "LIMIT 1";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clientID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                            String.valueOf(rs.getInt("TrainingTypeID")),     // [0]
                            rs.getString("TrainingCategory"),                  // [1]
                            String.valueOf(rs.getInt("PaymentPeriodID")),    // [2]
                            rs.getString("PaymentPeriod"),                    // [3]
                            String.valueOf(rs.getDouble("FinalPrice")),      // [4]
                            rs.getString("ExpiryDate"),                       // [5]
                            String.valueOf(rs.getInt("ClientTypeID"))        // [6]
                    };
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLIENT HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the email address for a client, or an empty string.
     */
    public static String getClientEmail(int clientID) {
        String sql = "SELECT COALESCE(ClientEmail, '') AS ClientEmail FROM ClientTable WHERE ClientID = ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clientID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("ClientEmail");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return "";
    }

    /**
     * Returns the most-recent coach assignment for a client as
     * {StaffID, CoachFullName, Applied_Coaching_Price}, or null.
     */
    public static String[] getClientCurrentCoach(int clientID) {
        String sql =
                "SELECT csa.StaffID, " +
                        "       CONCAT(s.StaffFirstName, ' ', s.StaffLastName) AS CoachName, " +
                        "       csa.Applied_Coaching_Price " +
                        "FROM ClientStaffAssignmentTable csa " +
                        "JOIN StaffTable s ON csa.StaffID = s.StaffID " +
                        "WHERE csa.ClientID = ? " +
                        "ORDER BY csa.ClientStaffAssignmentID DESC LIMIT 1";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clientID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                            String.valueOf(rs.getInt("StaffID")),
                            rs.getString("CoachName"),
                            String.valueOf(rs.getDouble("Applied_Coaching_Price"))
                    };
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COACH ASSIGNMENT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the existing coaching fee for a specific client+staff pair, or 0.0.
     */
    public static double getExistingCoachFee(int clientID, int staffID) {
        String sql =
                "SELECT Applied_Coaching_Price FROM ClientStaffAssignmentTable " +
                        "WHERE ClientID = ? AND StaffID = ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clientID);
            ps.setInt(2, staffID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("Applied_Coaching_Price");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    /**
     * Returns the ClientStaffAssignmentID for a client+staff pair, or -1.
     */
    public static int getAssignmentID(int clientID, int staffID) {
        String sql =
                "SELECT ClientStaffAssignmentID FROM ClientStaffAssignmentTable " +
                        "WHERE ClientID = ? AND StaffID = ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clientID);
            ps.setInt(2, staffID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("ClientStaffAssignmentID");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    /**
     * Creates or updates a coach assignment and returns the ClientStaffAssignmentID.
     * Returns -1 on failure.
     */
    public static int upsertAssignment(int clientID, int staffID, double coachingPrice) {
        int existing = getAssignmentID(clientID, staffID);

        if (existing > 0) {
            String sql =
                    "UPDATE ClientStaffAssignmentTable SET Applied_Coaching_Price = ? " +
                            "WHERE ClientStaffAssignmentID = ?";
            try (Connection con = DBConnector.connect();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setDouble(1, coachingPrice);
                ps.setInt(2, existing);
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
            return existing;
        }

        String sql =
                "INSERT INTO ClientStaffAssignmentTable (ClientID, StaffID, Applied_Coaching_Price) " +
                        "VALUES (?, ?, ?)";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, clientID);
            ps.setInt(2, staffID);
            ps.setDouble(3, coachingPrice);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RECEIPT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Inserts a new receipt row with full snapshot columns for audit trail.
     * clientStaffAssignmentID may be null (no coach).
     * snapshotCoachName may be null (no coach).
     *
     * @return the generated ReceiptID, or -1 on failure.
     */
    public static int insertReceipt(int clientID,
                                    int paymentTypeID,
                                    int rateID,
                                    double totalPayment,
                                    Integer clientStaffAssignmentID,
                                    String snapshotTrainingCategory,
                                    String snapshotPaymentPeriod,
                                    String snapshotCoachName,
                                    String snapshotMembershipType) {
        String sql =
                "INSERT INTO ReceiptTable " +
                        "(ClientID, RateID, ClientStaffAssignmentID, PaymentTypeID, TotalPayment, PaymentDate, " +
                        " SnapshotTrainingCategory, SnapshotPaymentPeriod, SnapshotCoachName, SnapshotMembershipType) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), ?, ?, ?, ?)";

        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, clientID);
            ps.setInt(2, rateID);

            if (clientStaffAssignmentID != null && clientStaffAssignmentID > 0) {
                ps.setInt(3, clientStaffAssignmentID);
            } else {
                ps.setNull(3, Types.INTEGER);
            }

            ps.setInt(4, paymentTypeID);
            ps.setDouble(5, totalPayment);
            ps.setString(6, snapshotTrainingCategory);
            ps.setString(7, snapshotPaymentPeriod);
            ps.setString(8, snapshotCoachName);       // NULL if no coach
            ps.setString(9, snapshotMembershipType);

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }
}