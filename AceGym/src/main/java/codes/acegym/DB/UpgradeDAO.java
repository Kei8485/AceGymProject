package codes.acegym.DB;

import codes.acegym.Objects.UpgradeResult;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Data-access layer for the Plan Upgrade / Switch protocol.
 *
 * Responsibilities:
 *  1. Check active-membership status for a client.
 *  2. Evaluate whether a new plan selection is an upgrade, downgrade, or same.
 *  3. Filter coaches by training category match.
 *  4. Execute the three-step atomic upgrade transaction.
 *
 * DB schema facts (actual columns verified from dump):
 *   MembershipTable : MembershipID, ClientID, ClientTypeID, DateApplied, DateExpired
 *   ReceiptTable    : ReceiptID, ClientID, RateID, ClientStaffAssignmentID,
 *                     PaymentTypeID, TotalPayment, PaymentDate,
 *                     SnapshotTrainingCategory, SnapshotPaymentPeriod,
 *                     SnapshotCoachName, SnapshotMembershipType
 *   (Run migration.sql first to add the four Snapshot columns.)
 */
public class UpgradeDAO {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. MEMBERSHIP GATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the client has an ACTIVE membership (DateExpired >= today).
     * Non-members or expired memberships return false.
     */
    public static boolean hasActiveMembership(int clientID) {
        String sql =
                "SELECT 1 FROM MembershipTable " +
                        "WHERE ClientID = ? " +
                        "  AND DateExpired >= CURDATE() " +
                        "LIMIT 1";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clientID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. UPGRADE ELIGIBILITY CHECK
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Compares the client's currently active plan (if any) against the
     * newly selected training type + payment period + client type combination.
     */
    public static UpgradeResult checkUpgrade(int clientID,
                                             int newTrainingTypeID,
                                             int newPaymentPeriodID,
                                             int newClientTypeID) {
        double newPrice = getRatePrice(newTrainingTypeID, newPaymentPeriodID, newClientTypeID);
        if (newPrice < 0) return UpgradeResult.noActivePlan();

        String[] active = PaymentDAO.getActivePlan(clientID);
        if (active == null) return UpgradeResult.noActivePlan();

        // active[0]=TrainingTypeID  [1]=TrainingCategory  [2]=PaymentPeriodID
        // active[3]=PaymentPeriod   [4]=FinalPrice        [5]=ExpiryDate  [6]=ClientTypeID
        int    currentTTID  = Integer.parseInt(active[0]);
        int    currentPPID  = Integer.parseInt(active[2]);
        int    currentCTID  = Integer.parseInt(active[6]);
        double currentPrice = Double.parseDouble(active[4]);
        String expiry       = formatExpiry(active[5]);
        String category     = active[1];
        String period       = active[3];

        if (currentTTID == newTrainingTypeID
                && currentPPID == newPaymentPeriodID
                && currentCTID == newClientTypeID) {
            return UpgradeResult.samePlanBlocked(currentPrice, expiry, category, period);
        }

        if (newPrice > currentPrice) {
            return UpgradeResult.upgradeAllowed(currentPrice, newPrice, expiry, category, period);
        } else {
            return UpgradeResult.downgradeBlocked(currentPrice, newPrice, expiry, category, period);
        }
    }

    private static double getRatePrice(int trainingTypeID, int paymentPeriodID, int clientTypeID) {
        String sql =
                "SELECT FinalPrice FROM RateTable " +
                        "WHERE TrainingTypeID = ? AND PaymentPeriodID = ? AND ClientTypeID = ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, trainingTypeID);
            ps.setInt(2, paymentPeriodID);
            ps.setInt(3, clientTypeID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("FinalPrice");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. COACH FILTERING BY TRAINING CATEGORY
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the coach's category is compatible with the selected category.
     * "Both" coaches always qualify; exact-category coaches also qualify.
     */
    public static boolean isCoachCompatible(String coachCategory, String selectedCategory) {
        if (coachCategory == null || selectedCategory == null) return false;
        return "Both".equalsIgnoreCase(coachCategory)
                || coachCategory.equalsIgnoreCase(selectedCategory);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. ATOMIC UPGRADE TRANSACTION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Executes the upgrade transaction atomically:
     *   Step 1 — Update MembershipTable: ClientTypeID + DateApplied/DateExpired
     *            (MembershipTable has NO RateID column — only dates are touched)
     *   Step 2 — Coach assignment is created by the controller before this call
     *   Step 3 — Insert ReceiptTable row for the top-up/switch amount
     *
     * @param clientID               the client
     * @param newRateID              RateID for the receipt row
     * @param newClientTypeID        ClientTypeID (1=Non-Member, 2=Member) for MembershipTable
     * @param newAssignmentID        ClientStaffAssignmentID (-1 = no coach)
     * @param paymentTypeID          PaymentTypeID
     * @param topUpAmount            amount to charge
     * @param newPeriodDays          days for DateExpired extension
     * @param snapshotCategory       frozen category for audit
     * @param snapshotPeriod         frozen period for audit
     * @param snapshotCoachName      frozen coach name (null if no coach)
     * @param snapshotMembershipType frozen membership type for audit
     * @param isUpgrade              true = "Upgrade Fee", false = "Plan Switch Fee"
     * @return new ReceiptID, or -1 on failure
     */
    public static int executeUpgradeTransaction(
            int     clientID,
            int     newRateID,
            int     newClientTypeID,
            int     newAssignmentID,
            int     paymentTypeID,
            double  topUpAmount,
            int     newPeriodDays,
            String  snapshotCategory,
            String  snapshotPeriod,
            String  snapshotCoachName,
            String  snapshotMembershipType,
            boolean isUpgrade) {

        String receiptLabel = isUpgrade ? "Upgrade Fee" : "Plan Switch Fee";

        try (Connection con = DBConnector.connect()) {
            con.setAutoCommit(false);
            try {
                // Step 1: update membership dates + ClientTypeID
                updateMembership(con, clientID, newClientTypeID, newPeriodDays);

                // Step 2: assignment already created by controller — nothing to do here

                // Step 3: insert the top-up receipt
                int receiptID = insertUpgradeReceipt(
                        con, clientID, newRateID,
                        newAssignmentID > 0 ? newAssignmentID : null,
                        paymentTypeID, topUpAmount,
                        snapshotCategory, receiptLabel,
                        snapshotCoachName, snapshotMembershipType);

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
            return -1;
        }
    }

    // ── Step 1: only update columns that actually exist in MembershipTable ──
    private static void updateMembership(Connection con, int clientID,
                                         int newClientTypeID, int newPeriodDays)
            throws SQLException {
        int membershipID = -1;
        String findSQL =
                "SELECT MembershipID FROM MembershipTable " +
                        "WHERE ClientID = ? ORDER BY DateApplied DESC LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(findSQL)) {
            ps.setInt(1, clientID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) membershipID = rs.getInt("MembershipID");
            }
        }
        if (membershipID < 0) return;

        String updateSQL =
                "UPDATE MembershipTable " +
                        "SET ClientTypeID = ?, " +
                        "    DateApplied  = CURDATE(), " +
                        "    DateExpired  = DATE_ADD(CURDATE(), INTERVAL ? DAY) " +
                        "WHERE MembershipID = ?";
        try (PreparedStatement ps = con.prepareStatement(updateSQL)) {
            ps.setInt(1, newClientTypeID);
            ps.setInt(2, newPeriodDays);
            ps.setInt(3, membershipID);
            ps.executeUpdate();
        }
    }

    // ── Step 3: insert receipt using snapshot columns ──────────────────────
    private static int insertUpgradeReceipt(Connection con,
                                            int clientID,
                                            int rateID,
                                            Integer assignmentID,
                                            int paymentTypeID,
                                            double amount,
                                            String snapshotCategory,
                                            String receiptLabel,
                                            String snapshotCoachName,
                                            String snapshotMembershipType)
            throws SQLException {
        String sql =
                "INSERT INTO ReceiptTable " +
                        "(ClientID, RateID, ClientStaffAssignmentID, PaymentTypeID, " +
                        " TotalPayment, PaymentDate, " +
                        " SnapshotTrainingCategory, SnapshotPaymentPeriod, " +
                        " SnapshotCoachName, SnapshotMembershipType) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, clientID);
            ps.setInt(2, rateID);
            if (assignmentID != null && assignmentID > 0) {
                ps.setInt(3, assignmentID);
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setInt(4, paymentTypeID);
            ps.setDouble(5, amount);
            ps.setString(6, snapshotCategory);
            ps.setString(7, receiptLabel);   // "Upgrade Fee" — visible in Report tab
            ps.setString(8, snapshotCoachName);
            ps.setString(9, snapshotMembershipType);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private static String formatExpiry(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) return "—";
        try {
            LocalDate d = LocalDate.parse(isoDate);
            return d.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        } catch (Exception e) {
            return isoDate;
        }
    }
}