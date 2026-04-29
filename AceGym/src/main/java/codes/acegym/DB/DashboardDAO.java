package codes.acegym.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DashboardDAO {

    // ═══════════════════════════════════════════════════════════════
    // STAT CARDS
    // ═══════════════════════════════════════════════════════════════

    public static int getTotalMembers() {
        return queryInt("SELECT COUNT(*) FROM ClientTable");
    }

    public static int getNewMembersThisMonth() {
        String sql =
                "SELECT COUNT(DISTINCT c.ClientID) " +
                        "FROM ClientTable c " +
                        "JOIN MembershipTable m ON m.ClientID = c.ClientID " +
                        "WHERE YEAR(m.DateApplied)  = YEAR(CURDATE()) " +
                        "  AND MONTH(m.DateApplied) = MONTH(CURDATE())";
        return queryInt(sql);
    }

    public static int getRenewalsDueCount() {
        String sql =
                "SELECT COUNT(*) " +
                        "FROM MembershipTable m " +
                        "WHERE m.DateExpired >= CURDATE() " +
                        "  AND m.DateExpired <= DATE_ADD(CURDATE(), INTERVAL 7 DAY) " +
                        "  AND m.ClientTypeID = 2 " +
                        "  AND m.MembershipID = (" +
                        "      SELECT MembershipID FROM MembershipTable " +
                        "      WHERE ClientID = m.ClientID " +
                        "      ORDER BY DateApplied DESC LIMIT 1" +
                        "  )";
        return queryInt(sql);
    }

    public static int getTotalCoaches() {
        String sql =
                "SELECT COUNT(*) FROM StaffTable " +
                        "WHERE LOWER(SystemRole) IN ('staff','admin')";
        return queryInt(sql);
    }

    // ═══════════════════════════════════════════════════════════════
    // REVENUE
    // ═══════════════════════════════════════════════════════════════

    public static double getMonthlySales() {
        String sql =
                "SELECT COALESCE(SUM(TotalPayment), 0) FROM ReceiptTable " +
                        "WHERE YEAR(PaymentDate)  = YEAR(CURDATE()) " +
                        "  AND MONTH(PaymentDate) = MONTH(CURDATE())";
        return queryDouble(sql);
    }

    public static double getCashSalesThisMonth() {
        String sql =
                "SELECT COALESCE(SUM(r.TotalPayment), 0) " +
                        "FROM ReceiptTable r " +
                        "JOIN PaymentTypeTable pt ON r.PaymentTypeID = pt.PaymentTypeID " +
                        "WHERE LOWER(pt.PaymentType) = 'cash' " +
                        "  AND YEAR(r.PaymentDate)  = YEAR(CURDATE()) " +
                        "  AND MONTH(r.PaymentDate) = MONTH(CURDATE())";
        return queryDouble(sql);
    }

    public static double getDigitalSalesThisMonth() {
        String sql =
                "SELECT COALESCE(SUM(r.TotalPayment), 0) " +
                        "FROM ReceiptTable r " +
                        "JOIN PaymentTypeTable pt ON r.PaymentTypeID = pt.PaymentTypeID " +
                        "WHERE LOWER(pt.PaymentType) != 'cash' " +
                        "  AND YEAR(r.PaymentDate)  = YEAR(CURDATE()) " +
                        "  AND MONTH(r.PaymentDate) = MONTH(CURDATE())";
        return queryDouble(sql);
    }

    public static int getTransactionCountThisMonth() {
        String sql =
                "SELECT COUNT(*) FROM ReceiptTable " +
                        "WHERE YEAR(PaymentDate)  = YEAR(CURDATE()) " +
                        "  AND MONTH(PaymentDate) = MONTH(CURDATE())";
        return queryInt(sql);
    }

    public static double getTotalSalesAllTime() {
        return queryDouble("SELECT COALESCE(SUM(TotalPayment), 0) FROM ReceiptTable");
    }

    // ═══════════════════════════════════════════════════════════════
    // LIST DATA
    // ═══════════════════════════════════════════════════════════════

    /**
     * @param name                full name
     * @param email               client email — used by the Notify button
     * @param membershipExpiryDate  e.g. "Expires Membership in Apr 28, 2026"
     * @param planExpiryDate        e.g. "Expires Monthly plan in Apr 26, 2026"
     *                              null if no receipt/plan found
     * @param daysLeft            display badge e.g. "3 days" or "Today"
     * @param daysLeftRaw         raw int — passed to RenewalNotificationService
     * @param paymentPeriod       e.g. "Monthly" / "Daily" / "Yearly"
     */
    public record RenewalRow(String name, String email,
                             String membershipExpiryDate, String planExpiryDate,
                             String daysLeft, int daysLeftRaw, String paymentPeriod) {}

    public static List<RenewalRow> getRenewalsDue() {
        List<RenewalRow> list = new ArrayList<>();

        // Outer query filters and formats. Inner query computes the raw plan expiry date
        // so we can reference it in WHERE and DATEDIFF without repeating the CASE expression.
        String sql =
                "SELECT " +
                        "    FullName, ClientEmail, PaymentPeriod, " +
                        "    MembershipExpiry, MembershipDaysLeft, " +
                        "    DATE_FORMAT(PlanExpiryRaw, '%b %d, %Y') AS PlanExpiry, " +
                        "    DATEDIFF(PlanExpiryRaw, CURDATE())        AS PlanDaysLeft " +
                        "FROM ( " +
                        "    SELECT " +
                        "        CONCAT(c.FirstName,' ',c.LastName)          AS FullName, " +
                        "        COALESCE(c.ClientEmail, '')                  AS ClientEmail, " +
                        "        DATE_FORMAT(m.DateExpired, '%b %d, %Y')     AS MembershipExpiry, " +
                        "        DATEDIFF(m.DateExpired, CURDATE())           AS MembershipDaysLeft, " +
                        "        COALESCE(pp.PaymentPeriod, '—')              AS PaymentPeriod, " +
                        "        CASE LOWER(COALESCE(pp.PaymentPeriod,'')) " +
                        "            WHEN 'daily'   THEN DATE_ADD(r_last.PaymentDate, INTERVAL 1 DAY) " +
                        "            WHEN 'monthly' THEN DATE_ADD(r_last.PaymentDate, INTERVAL 1 MONTH) " +
                        "            WHEN 'yearly'  THEN DATE_ADD(r_last.PaymentDate, INTERVAL 1 YEAR) " +
                        "            ELSE NULL " +
                        "        END AS PlanExpiryRaw " +
                        "    FROM MembershipTable m " +
                        "    JOIN ClientTable c ON c.ClientID = m.ClientID " +
                        "    LEFT JOIN ReceiptTable r_last " +
                        "           ON r_last.ReceiptID = ( " +
                        "               SELECT ReceiptID FROM ReceiptTable " +
                        "               WHERE ClientID = c.ClientID " +
                        "                 AND (SnapshotPaymentPeriod IS NULL " +
                        "                      OR SnapshotPaymentPeriod NOT IN ('Upgrade Fee','Plan Switch Fee')) " +
                        "               ORDER BY PaymentDate DESC LIMIT 1 " +
                        "           ) " +
                        "    LEFT JOIN RateTable ra ON ra.RateID = r_last.RateID " +
                        "    LEFT JOIN PaymentPeriodTable pp ON pp.PaymentPeriodID = ra.PaymentPeriodID " +
                        "    WHERE m.ClientTypeID = 2 " +
                        "      AND m.MembershipID = ( " +
                        "              SELECT MembershipID FROM MembershipTable " +
                        "              WHERE ClientID = m.ClientID " +
                        "              ORDER BY DateApplied DESC LIMIT 1 " +
                        "          ) " +
                        ") sub " +
                        // Show row if EITHER membership OR plan is expiring within 7 days
                        "WHERE (MembershipDaysLeft >= 0 AND MembershipDaysLeft <= 7) " +
                        "   OR (PlanExpiryRaw IS NOT NULL " +
                        "       AND DATEDIFF(PlanExpiryRaw, CURDATE()) >= 0 " +
                        "       AND DATEDIFF(PlanExpiryRaw, CURDATE()) <= 7) " +
                        "ORDER BY LEAST( " +
                        "    CASE WHEN MembershipDaysLeft >= 0 AND MembershipDaysLeft <= 7 " +
                        "         THEN MembershipDaysLeft ELSE 999 END, " +
                        "    CASE WHEN PlanExpiryRaw IS NOT NULL " +
                        "          AND DATEDIFF(PlanExpiryRaw, CURDATE()) BETWEEN 0 AND 7 " +
                        "         THEN DATEDIFF(PlanExpiryRaw, CURDATE()) ELSE 999 END " +
                        ") ASC " +
                        "LIMIT 10";

        try (Connection con = DBConnector.connect();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(sql)) {

            while (rs.next()) {
                int    memDays  = rs.getInt("MembershipDaysLeft");
                int    planDays = rs.getInt("PlanDaysLeft");   // 0 if NULL (rs returns 0)
                boolean planNull = rs.wasNull();               // true if PlanDaysLeft was NULL

                String period       = rs.getString("PaymentPeriod");
                String memExpiry    = rs.getString("MembershipExpiry");
                String planExpiry   = rs.getString("PlanExpiry");

                // Only show membership line if it's expiring within 7 days
                String membershipLine = (memDays >= 0 && memDays <= 7)
                        ? "Membership expires in " + memExpiry
                        : null;

                // Only show plan line if plan exists and is expiring within 7 days
                String planLine = (!planNull && planDays >= 0 && planDays <= 7
                        && planExpiry != null && !period.equals("—"))
                        ? period + " plan expires in " + planExpiry
                        : null;

                // Badge = soonest expiry among what's actually expiring
                int badgeDays = 999;
                if (membershipLine != null) badgeDays = Math.min(badgeDays, memDays);
                if (planLine       != null) badgeDays = Math.min(badgeDays, planDays);

                String badge = badgeDays == 0   ? "Today"
                        : badgeDays == 1   ? "1 day"
                        : badgeDays < 999  ? badgeDays + " days"
                        : "—";

                list.add(new RenewalRow(
                        rs.getString("FullName"),
                        rs.getString("ClientEmail"),
                        membershipLine,
                        planLine,
                        badge,
                        badgeDays == 999 ? 0 : badgeDays,
                        period
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    // ─────────────────────────────────────────────────────────────
    // RECENT MEMBERS
    // ─────────────────────────────────────────────────────────────

    public record MemberSummaryRow(String name, String enrolled, String status, String type) {}

    public static List<MemberSummaryRow> getRecentMembers() {
        List<MemberSummaryRow> list = new ArrayList<>();
        String sql =
                // New clients — first ever membership applied today
                "SELECT CONCAT(c.FirstName,' ',c.LastName)      AS FullName, " +
                        "       DATE_FORMAT(m.DateApplied, '%b %d, %Y') AS Enrolled, " +
                        "       CASE WHEN m.DateExpired >= CURDATE() THEN 'Active' ELSE 'Expired' END AS Status, " +
                        "       'New'                                    AS Type " +
                        "FROM MembershipTable m " +
                        "JOIN ClientTable c ON c.ClientID = m.ClientID " +
                        "WHERE m.DateApplied = CURDATE() " +
                        "AND NOT EXISTS (" +
                        "    SELECT 1 FROM MembershipTable prev " +
                        "    WHERE prev.ClientID = m.ClientID " +
                        "    AND prev.DateApplied < CURDATE() " +
                        ") " +

                        "UNION ALL " +

                        // Renewed clients — membership applied today but had one before
                        "SELECT CONCAT(c.FirstName,' ',c.LastName)      AS FullName, " +
                        "       DATE_FORMAT(m.DateApplied, '%b %d, %Y') AS Enrolled, " +
                        "       CASE WHEN m.DateExpired >= CURDATE() THEN 'Active' ELSE 'Expired' END AS Status, " +
                        "       'Renewed'                                AS Type " +
                        "FROM MembershipTable m " +
                        "JOIN ClientTable c ON c.ClientID = m.ClientID " +
                        "WHERE m.DateApplied = CURDATE() " +
                        "AND EXISTS (" +
                        "    SELECT 1 FROM MembershipTable prev " +
                        "    WHERE prev.ClientID = m.ClientID " +
                        "    AND prev.DateApplied < CURDATE() " +
                        ") " +

                        "UNION ALL " +

                        // New clients registered today but no membership yet
                        "SELECT CONCAT(c.FirstName,' ',c.LastName)      AS FullName, " +
                        "       DATE_FORMAT(c.DateRegistered, '%b %d, %Y') AS Enrolled, " +
                        "       'None'                                   AS Status, " +
                        "       'New Client'                             AS Type " +
                        "FROM ClientTable c " +
                        "WHERE c.DateRegistered = CURDATE() " +
                        "AND NOT EXISTS (" +
                        "    SELECT 1 FROM MembershipTable m " +
                        "    WHERE m.ClientID = c.ClientID " +
                        ") " +

                        "ORDER BY Enrolled DESC " +
                        "LIMIT 10";

        try (Connection con = DBConnector.connect();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new MemberSummaryRow(
                        rs.getString("FullName"),
                        rs.getString("Enrolled"),
                        rs.getString("Status"),
                        rs.getString("Type")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }    // COACHES
    // ─────────────────────────────────────────────────────────────

    public record CoachSummaryRow(String name, String coachId, String clientCount) {}

    public static List<CoachSummaryRow> getCoachSummaries() {
        List<CoachSummaryRow> list = new ArrayList<>();
        String sql =
                "SELECT s.StaffID, " +
                        "       CONCAT(s.StaffFirstName,' ',s.StaffLastName) AS FullName, " +
                        "       COUNT(csa.ClientID) AS ClientCount " +
                        "FROM StaffTable s " +
                        "LEFT JOIN ClientStaffAssignmentTable csa ON csa.StaffID = s.StaffID " +
                        "WHERE LOWER(s.SystemRole) IN ('staff','admin') " +
                        "GROUP BY s.StaffID, FullName " +
                        "ORDER BY s.StaffFirstName";

        try (Connection con = DBConnector.connect();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("StaffID");
                list.add(new CoachSummaryRow(
                        rs.getString("FullName"),
                        String.format("C-%03d", id),
                        String.valueOf(rs.getInt("ClientCount"))
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    private static int queryInt(String sql) {
        try (Connection con = DBConnector.connect();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    private static double queryDouble(String sql) {
        try (Connection con = DBConnector.connect();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }
}