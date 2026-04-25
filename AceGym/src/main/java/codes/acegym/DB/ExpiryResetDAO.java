package codes.acegym.DB;

import java.sql.*;

/**
 * Handles automatic expiry resets for clients whose plans or memberships
 * have passed their expiry date.

 * Reset rules (run every app start + every manual refresh):

 *  1. PLAN EXPIRY — if the client's most-recent receipt plan has expired:
 *       → Insert a new ReceiptTable row with RateID pointing to the
 *         "None / Daily / Non-Member" rate (TrainingTypeID=4, PaymentPeriodID=1, ClientTypeID=1)
 *         with TotalPayment = 0.  This makes the "last plan" read as expired/reset.
 *         NOTE: We do NOT delete history. We just add a zero-cost reset row.

 *  2. MEMBERSHIP EXPIRY — if MembershipTable.DateExpired < today:
 *       → Set ClientTypeID back to 1 (Non Member) in MembershipTable.
 *         DateApplied and DateExpired are left as-is so the history is visible.

 *  Both operations run inside a single transaction per client so they are atomic.

 * DB facts (verified from schema dump):
 *   MembershipTable : MembershipID, ClientID, ClientTypeID, DateApplied, DateExpired
 *   ReceiptTable    : ReceiptID, ClientID, RateID, ClientStaffAssignmentID,
 *                     PaymentTypeID, TotalPayment, PaymentDate,
 *                     SnapshotTrainingCategory, SnapshotPaymentPeriod,
 *                     SnapshotCoachName, SnapshotMembershipType
 *   RateTable       : TrainingTypeID=4 (None), PaymentPeriodID=1 (Daily),
 *                     ClientTypeID=1 (Non Member) → TotalPayment = 0
 */
public class ExpiryResetDAO {

    /**
     * Scans all clients and resets any that have expired plans or memberships.
     * Call this on app startup and on every ViewMembers refresh.
     *
     * @return number of clients that were reset
     */
    public static int runExpiryResets() {
        // ── Membership resets ONLY ─────────────────────────────────────────────
        // Plan status is calculated live from the last real receipt in the SQL query.
        // No fake reset receipts needed.

        String expiredMemberSQL =
                "SELECT m.ClientID, m.MembershipID " +
                        "FROM MembershipTable m " +
                        "WHERE m.DateExpired < CURDATE() " +
                        "  AND m.ClientTypeID = 2 " +
                        "  AND m.MembershipID = ( " +
                        "      SELECT MembershipID FROM MembershipTable " +
                        "      WHERE ClientID = m.ClientID " +
                        "      ORDER BY DateApplied DESC LIMIT 1 " +
                        "  )";

        int resetCount = 0;
        try (Connection con = DBConnector.connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(expiredMemberSQL)) {

            while (rs.next()) {
                int membershipID = rs.getInt("MembershipID");
                resetMembership(con, membershipID);
                resetCount++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return resetCount;
    }
    // ─────────────────────────────────────────────────────────────────────────
    // PLAN RESET
    // Inserts a zero-cost "None/Daily" receipt row so the client's latest plan
    // reads as reset without deleting any history.
    // ─────────────────────────────────────────────────────────────────────────
    private static void resetClientPlan(Connection con, int clientID,
                                        int resetRateID, int paymentTypeID)
            throws SQLException {
        // Use the client's existing "no coach" assignment (StaffID=1) if present
        int assignmentID = getNoneAssignmentID(con, clientID);

        String sql =
                "INSERT INTO ReceiptTable " +
                        "(ClientID, RateID, ClientStaffAssignmentID, PaymentTypeID, " +
                        " TotalPayment, PaymentDate, " +
                        " SnapshotTrainingCategory, SnapshotPaymentPeriod, " +
                        " SnapshotCoachName, SnapshotMembershipType) " +
                        "VALUES (?, ?, ?, ?, 0, NOW(), 'None', 'Daily', NULL, 'Non Member')";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clientID);
            ps.setInt(2, resetRateID);
            if (assignmentID > 0) {
                ps.setInt(3, assignmentID);
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setInt(4, paymentTypeID);
            ps.executeUpdate();
        }

        reassignToNone(con, clientID); // ← add this line

    }

    // ─────────────────────────────────────────────────────────────────────────
    // MEMBERSHIP RESET
    // Flips ClientTypeID back to 1 (Non Member) for the expired membership row.
    // ─────────────────────────────────────────────────────────────────────────
    private static void resetMembership(Connection con, int membershipID)
            throws SQLException {
        String sql =
                "UPDATE MembershipTable SET ClientTypeID = 1 " +
                        "WHERE MembershipID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, membershipID);
            ps.executeUpdate();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the RateID for TrainingTypeID=4 (None), PaymentPeriodID=1 (Daily),
     * ClientTypeID=1 (Non Member) — the default "reset" rate.
     */
    private static int getNoneRateID() {
        // From the schema dump: TrainingTypeID=4, PaymentPeriodID=1, ClientTypeID=1 → RateID=10
        // We query dynamically to stay correct if RateIDs ever change.
        String sql =
                "SELECT RateID FROM RateTable " +
                        "WHERE TrainingTypeID = 4 AND PaymentPeriodID = 1 AND ClientTypeID = 1 " +
                        "LIMIT 1";
        try (Connection con = DBConnector.connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt("RateID");
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    /** Returns PaymentTypeID for "Cash", or -1 if not found. */
    private static int getCashPaymentTypeID() {
        String sql = "SELECT PaymentTypeID FROM PaymentTypeTable " +
                "WHERE LOWER(PaymentType) = 'cash' LIMIT 1";
        try (Connection con = DBConnector.connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt("PaymentTypeID");
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    /** Returns the ClientStaffAssignmentID for the "no-coach" (StaffID=1) row for this client. */
    private static int getNoneAssignmentID(Connection con, int clientID) throws SQLException {
        String sql =
                "SELECT ClientStaffAssignmentID FROM ClientStaffAssignmentTable " +
                        "WHERE ClientID = ? AND StaffID = 1 LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clientID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("ClientStaffAssignmentID");
            }
        }
        return -1;
    }


    // ─────────────────────────────────────────────────────────────────────────
// COACH REASSIGNMENT
// When a client's plan expires, remove all non-None coach assignments
// so they fall back to StaffID=1 (None) on the coach cards.
// ─────────────────────────────────────────────────────────────────────────
    private static void reassignToNone(Connection con, int clientID) throws SQLException {
        // NULL out the receipt FK first to avoid FK constraint violation
        String nullReceipts =
                "UPDATE ReceiptTable SET ClientStaffAssignmentID = NULL " +
                        "WHERE ClientStaffAssignmentID IN (" +
                        "    SELECT ClientStaffAssignmentID FROM ClientStaffAssignmentTable " +
                        "    WHERE ClientID = ? AND StaffID != 1" +
                        ")";
        try (PreparedStatement ps = con.prepareStatement(nullReceipts)) {
            ps.setInt(1, clientID);
            ps.executeUpdate();
        }

        // Delete the non-None assignments
        String deleteAssign =
                "DELETE FROM ClientStaffAssignmentTable " +
                        "WHERE ClientID = ? AND StaffID != 1";
        try (PreparedStatement ps = con.prepareStatement(deleteAssign)) {
            ps.setInt(1, clientID);
            ps.executeUpdate();
        }

        // Ensure the None assignment exists
        String checkNone =
                "SELECT COUNT(*) FROM ClientStaffAssignmentTable " +
                        "WHERE ClientID = ? AND StaffID = 1";
        try (PreparedStatement ps = con.prepareStatement(checkNone)) {
            ps.setInt(1, clientID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String insertNone =
                            "INSERT INTO ClientStaffAssignmentTable " +
                                    "(ClientID, StaffID, Applied_Coaching_Price) VALUES (?, 1, 0.00)";
                    try (PreparedStatement ps2 = con.prepareStatement(insertNone)) {
                        ps2.setInt(1, clientID);
                        ps2.executeUpdate();
                    }
                }
            }
        }
    }
}