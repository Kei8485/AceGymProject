package codes.acegym.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access layer for the Dashboard screen.
 *
 * Covers:
 *  ── Stat Cards ──────────────────────────────────────────────────
 *  • getTotalMembers()       — total clients in ClientTable
 *  • getNewMembersThisMonth()— clients whose membership DateApplied is this month
 *  • getRenewalsDueCount()   — active memberships expiring within 7 days
 *  • getTotalCoaches()       — staff rows with SystemRole IN ('Staff','Admin')
 *
 *  ── Revenue (current month) ─────────────────────────────────────
 *  • getMonthlySales()       — SUM of TotalPayment this month
 *  • getCashSalesThisMonth() — SUM where PaymentType = 'Cash' this month
 *  • getDigitalSalesThisMonth()— SUM where PaymentType != 'Cash' this month
 *  • getTransactionCountThisMonth()— COUNT of receipts this month
 *  • getTotalSalesAllTime()  — SUM of all TotalPayment ever
 *
 *  ── List data ───────────────────────────────────────────────────
 *  • getRenewalsDue()        — up to 10 members expiring within 7 days
 *  • getRecentMembers()      — last 10 clients by most recent membership DateApplied
 *  • getCoachSummaries()     — all coaches with their assigned client count
 */
public class DashboardDAO {

    // ═══════════════════════════════════════════════════════════════
    // STAT CARDS
    // ═══════════════════════════════════════════════════════════════

    /** Total number of clients ever registered. */
    public static int getTotalMembers() {
        return queryInt("SELECT COUNT(*) FROM ClientTable");
    }

    /**
     * Clients who have a membership row where DateApplied falls in the
     * current calendar month (i.e. "new this month").
     */
    public static int getNewMembersThisMonth() {
        String sql =
                "SELECT COUNT(DISTINCT c.ClientID) " +
                        "FROM ClientTable c " +
                        "JOIN MembershipTable m ON m.ClientID = c.ClientID " +
                        "WHERE YEAR(m.DateApplied)  = YEAR(CURDATE()) " +
                        "  AND MONTH(m.DateApplied) = MONTH(CURDATE())";
        return queryInt(sql);
    }

    /**
     * Active memberships that expire within the next 7 days (inclusive today).
     * These are the clients the gym should contact for renewal.
     */
    public static int getRenewalsDueCount() {
        String sql =
                "SELECT COUNT(*) " +
                        "FROM MembershipTable m " +
                        "WHERE m.DateExpired >= CURDATE() " +
                        "  AND m.DateExpired <= DATE_ADD(CURDATE(), INTERVAL 7 DAY) " +
                        "  AND m.ClientTypeID = 2 " +           // only active Members
                        "  AND m.MembershipID = (" +
                        "      SELECT MembershipID FROM MembershipTable " +
                        "      WHERE ClientID = m.ClientID " +
                        "      ORDER BY DateApplied DESC LIMIT 1" +
                        "  )";
        return queryInt(sql);
    }

    /**
     * Count of staff with SystemRole IN ('Staff','Admin') — the coaches
     * visible in the Coaches tab.
     */
    public static int getTotalCoaches() {
        String sql =
                "SELECT COUNT(*) FROM StaffTable " +
                        "WHERE LOWER(SystemRole) IN ('staff','admin')";
        return queryInt(sql);
    }

    // ═══════════════════════════════════════════════════════════════
    // REVENUE
    // ═══════════════════════════════════════════════════════════════

    /** Sum of all receipts whose PaymentDate is in the current month. */
    public static double getMonthlySales() {
        String sql =
                "SELECT COALESCE(SUM(TotalPayment), 0) FROM ReceiptTable " +
                        "WHERE YEAR(PaymentDate)  = YEAR(CURDATE()) " +
                        "  AND MONTH(PaymentDate) = MONTH(CURDATE())";
        return queryDouble(sql);
    }

    /** Monthly sales paid by Cash. */
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

    /** Monthly sales paid by any non-Cash method (GCash, card, etc.). */
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

    /** Number of receipt rows this month. */
    public static int getTransactionCountThisMonth() {
        String sql =
                "SELECT COUNT(*) FROM ReceiptTable " +
                        "WHERE YEAR(PaymentDate)  = YEAR(CURDATE()) " +
                        "  AND MONTH(PaymentDate) = MONTH(CURDATE())";
        return queryInt(sql);
    }

    /** Grand total of all payments ever recorded. */
    public static double getTotalSalesAllTime() {
        return queryDouble("SELECT COALESCE(SUM(TotalPayment), 0) FROM ReceiptTable");
    }

    // ═══════════════════════════════════════════════════════════════
    // LIST DATA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Row model for the Renewals Due list.
     * @param name       full name
     * @param expiryDate formatted expiry date
     * @param daysLeft   e.g. "3 days" or "Today"
     */
    public record RenewalRow(String name, String expiryDate, String daysLeft) {}

    /**
     * Up to 10 active members whose membership expires within the next 7 days,
     * sorted soonest first.
     */
    public static List<RenewalRow> getRenewalsDue() {
        List<RenewalRow> list = new ArrayList<>();
        String sql =
                "SELECT CONCAT(c.FirstName,' ',c.LastName) AS FullName, " +
                        "       DATE_FORMAT(m.DateExpired, '%b %d, %Y') AS ExpiryDate, " +
                        "       DATEDIFF(m.DateExpired, CURDATE()) AS DaysLeft " +
                        "FROM MembershipTable m " +
                        "JOIN ClientTable c ON c.ClientID = m.ClientID " +
                        "WHERE m.DateExpired >= CURDATE() " +
                        "  AND m.DateExpired <= DATE_ADD(CURDATE(), INTERVAL 7 DAY) " +
                        "  AND m.ClientTypeID = 2 " +
                        "  AND m.MembershipID = (" +
                        "      SELECT MembershipID FROM MembershipTable " +
                        "      WHERE ClientID = m.ClientID " +
                        "      ORDER BY DateApplied DESC LIMIT 1" +
                        "  ) " +
                        "ORDER BY m.DateExpired ASC " +
                        "LIMIT 10";

        try (Connection con = DBConnector.connect();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(sql)) {

            while (rs.next()) {
                int days = rs.getInt("DaysLeft");
                String badge = days == 0 ? "Today"
                        : days == 1 ? "1 day"
                        : days + " days";
                list.add(new RenewalRow(
                        rs.getString("FullName"),
                        "Expires " + rs.getString("ExpiryDate"),
                        badge
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Row model for the Recent Members list.
     * @param name     full name
     * @param enrolled formatted membership DateApplied
     * @param status   "Active" or "Expired"
     */
    public record MemberSummaryRow(String name, String enrolled, String status) {}

    /**
     * Last 10 clients by most recent membership DateApplied.
     */
    public static List<MemberSummaryRow> getRecentMembers() {
        List<MemberSummaryRow> list = new ArrayList<>();
        String sql =
                "SELECT CONCAT(c.FirstName,' ',c.LastName) AS FullName, " +
                        "       DATE_FORMAT(m.DateApplied, '%b %d, %Y') AS Enrolled, " +
                        "       CASE " +
                        "           WHEN m.DateExpired >= CURDATE() THEN 'Active' " +
                        "           ELSE 'Expired' " +
                        "       END AS Status " +
                        "FROM MembershipTable m " +
                        "JOIN ClientTable c ON c.ClientID = m.ClientID " +
                        "WHERE m.MembershipID = (" +
                        "    SELECT MembershipID FROM MembershipTable " +
                        "    WHERE ClientID = m.ClientID " +
                        "    ORDER BY DateApplied DESC LIMIT 1" +
                        ") " +
                        "ORDER BY m.DateApplied DESC " +
                        "LIMIT 10";

        try (Connection con = DBConnector.connect();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new MemberSummaryRow(
                        rs.getString("FullName"),
                        rs.getString("Enrolled"),
                        rs.getString("Status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Row model for the Coaches list.
     * @param name        full name
     * @param coachId     formatted "C-001"
     * @param clientCount number of assigned clients
     */
    public record CoachSummaryRow(String name, String coachId, String clientCount) {}

    /**
     * All coaches (Staff/Admin) with their total assigned client count,
     * ordered by name.
     */
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