package codes.acegym.DB;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;

/**
 * Data-access layer for the Add-Payment screen.
 * Handles training types, payment periods, rates, payment types,
 * coach assignments, and receipt insertion.
 *
 * KEY CHANGES:
 *  - insertReceipt() now also upserts MembershipTable so Non Members who pay
 *    a plan are recorded as Members immediately on Save Payment.
 *  - getMembershipFee(clientTypeID) returns the MembershipFee for a client type.
 *  - isActivePlanNonDaily(clientID) returns true if the client has an active
 *    Monthly or Yearly plan (used by registration flow to skip plan selection).
 */
public class PaymentDAO {

    // ─────────────────────────────────────────────────────────────────────────
    // LOOK-UP TABLES
    // ─────────────────────────────────────────────────────────────────────────

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
    // MEMBERSHIP FEE  (NEW)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the MembershipFee for a given ClientTypeID from ClientTypeTable.
     * Returns 0.0 if the column doesn't exist yet (migration not run).
     *
     * @param clientTypeID  2 = Member, 1 = Non Member
     */
    public static double getMembershipFee(int clientTypeID) {
        String sql = "SELECT MembershipFee FROM ClientTypeTable WHERE ClientTypeID = ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clientTypeID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("MembershipFee");
            }
        } catch (SQLException e) {
            // Column doesn't exist yet — migration not run. Return 0 gracefully.
            System.err.println("[PaymentDAO] MembershipFee column missing — run migration_membership_fee.sql");
        }
        return 0.0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTIVE PLAN CHECKS  (NEW / UPDATED)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the client currently has an active Monthly or Yearly plan
     * (PaymentPeriodID 2 or 3, i.e. Days >= 30).
     *
     * Used by the registration → payment flow:
     *   - If true  → the client already has a solid plan; skip plan selection,
     *                only collect the membership fee.
     *   - If false → the client only has Daily or no plan; let them choose.
     */
    public static boolean hasActiveNonDailyPlan(int clientID) {
        String sql =
                "SELECT 1 FROM ReceiptTable r " +
                        "JOIN RateTable ra          ON r.RateID          = ra.RateID " +
                        "JOIN PaymentPeriodTable pp ON ra.PaymentPeriodID = pp.PaymentPeriodID " +
                        "JOIN TrainingTypeTable tt  ON ra.TrainingTypeID  = tt.TrainingTypeID " +
                        "WHERE r.ClientID = ? " +
                        "  AND tt.TrainingTypeID != 4 " +                 // exclude None
                        "  AND pp.Days >= 30 " +                          // Monthly or longer
                        "  AND DATE_ADD(DATE(r.PaymentDate), INTERVAL pp.Days DAY) >= CURDATE() " +
                        "ORDER BY r.PaymentDate DESC LIMIT 1";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clientID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RATE
    // ─────────────────────────────────────────────────────────────────────────

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
    // ACTIVE PLAN  (existing — unchanged)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the most recent non-expired, non-"None" plan for a client as:
     * [0] TrainingTypeID  [1] TrainingCategory  [2] PaymentPeriodID
     * [3] PaymentPeriod   [4] FinalPrice         [5] ExpiryDate (yyyy-MM-dd)
     * [6] ClientTypeID
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
                        "  AND tt.TrainingTypeID != 4 " +
                        "  AND DATE_ADD(DATE(r.PaymentDate), INTERVAL pp.Days DAY) >= CURDATE() " +
                        "ORDER BY r.PaymentDate DESC " +
                        "LIMIT 1";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clientID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                            String.valueOf(rs.getInt("TrainingTypeID")),
                            rs.getString("TrainingCategory"),
                            String.valueOf(rs.getInt("PaymentPeriodID")),
                            rs.getString("PaymentPeriod"),
                            String.valueOf(rs.getDouble("FinalPrice")),
                            rs.getString("ExpiryDate"),
                            String.valueOf(rs.getInt("ClientTypeID"))
                    };
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLIENT HELPERS
    // ─────────────────────────────────────────────────────────────────────────

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
    // RECEIPT  (UPDATED — now also writes MembershipTable)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Inserts a receipt AND upserts MembershipTable atomically.
     *
     * Why MembershipTable here?  Previously saveToDatabase() only wrote a
     * receipt row — the client never appeared as "Member" in MembershipTable.
     * Now, every time a real payment is saved (non-upgrade path), we also
     * create/update the client's membership row so their status is correct.
     *
     * @param clientTypeID       1=Non Member, 2=Member — drives MembershipTable update
     * @param periodDays         days for DateExpired calculation (0 = no membership update)
     * @param isFromRegistration if true, client is becoming a Member right now
     */
    public static int insertReceipt(int clientID,
                                    int paymentTypeID,
                                    int rateID,
                                    double totalPayment,
                                    Integer clientStaffAssignmentID,
                                    String snapshotTrainingCategory,
                                    String snapshotPaymentPeriod,
                                    String snapshotCoachName,
                                    String snapshotMembershipType,
                                    int clientTypeID,
                                    int periodDays) {
        String receiptSQL =
                "INSERT INTO ReceiptTable " +
                        "(ClientID, RateID, ClientStaffAssignmentID, PaymentTypeID, TotalPayment, PaymentDate, " +
                        " SnapshotTrainingCategory, SnapshotPaymentPeriod, SnapshotCoachName, SnapshotMembershipType) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), ?, ?, ?, ?)";

        try (Connection con = DBConnector.connect()) {
            con.setAutoCommit(false);
            try {
                // ── Step 1: Insert receipt ──────────────────────────────────
                int receiptID = -1;
                try (PreparedStatement ps = con.prepareStatement(receiptSQL, Statement.RETURN_GENERATED_KEYS)) {
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
                    ps.setString(8, snapshotCoachName);
                    ps.setString(9, snapshotMembershipType);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) receiptID = keys.getInt(1);
                    }
                }

                // ── Step 2: Upsert MembershipTable ─────────────────────────
                // Only update membership when:
                //   a) clientTypeID == 2 (becoming/is a Member), AND
                //   b) periodDays > 0 (there is a plan to apply dates to)
                if (periodDays > 0) {
                    upsertMembership(con, clientID, clientTypeID, periodDays);
                }

                con.commit();
                return receiptID;

            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Backward-compatible overload for callers that don't need the membership update.
     * Passes clientTypeID=1 and periodDays=0 so no MembershipTable write happens.
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
        return insertReceipt(clientID, paymentTypeID, rateID, totalPayment,
                clientStaffAssignmentID,
                snapshotTrainingCategory, snapshotPaymentPeriod,
                snapshotCoachName, snapshotMembershipType,
                1, 0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MEMBERSHIP UPSERT  (NEW — called inside insertReceipt transaction)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates or updates the most-recent MembershipTable row for a client.
     * - If no row exists → INSERT with DateApplied=TODAY, DateExpired=TODAY+periodDays
     * - If a row exists  → UPDATE it with the new ClientTypeID and dates
     *
     * This is called inside an existing transaction (con passed in).
     */
    private static void upsertMembership(Connection con, int clientID,
                                         int clientTypeID, int periodDays)
            throws SQLException {
        // Check for existing membership row
        String findSQL =
                "SELECT MembershipID FROM MembershipTable " +
                        "WHERE ClientID = ? ORDER BY DateApplied DESC LIMIT 1";
        int membershipID = -1;
        try (PreparedStatement ps = con.prepareStatement(findSQL)) {
            ps.setInt(1, clientID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) membershipID = rs.getInt("MembershipID");
            }
        }

        if (membershipID > 0) {
            // UPDATE existing row
            String updateSQL =
                    "UPDATE MembershipTable " +
                            "SET ClientTypeID = ?, DateApplied = CURDATE(), " +
                            "    DateExpired  = DATE_ADD(CURDATE(), INTERVAL ? DAY) " +
                            "WHERE MembershipID = ?";
            try (PreparedStatement ps = con.prepareStatement(updateSQL)) {
                ps.setInt(1, clientTypeID);
                ps.setInt(2, periodDays);
                ps.setInt(3, membershipID);
                ps.executeUpdate();
            }
        } else {
            // INSERT new row
            String insertSQL =
                    "INSERT INTO MembershipTable (ClientID, ClientTypeID, DateApplied, DateExpired) " +
                            "VALUES (?, ?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL ? DAY))";
            try (PreparedStatement ps = con.prepareStatement(insertSQL)) {
                ps.setInt(1, clientID);
                ps.setInt(2, clientTypeID);
                ps.setInt(3, periodDays);
                ps.executeUpdate();
            }
        }
    }
}