package codes.acegym.DB;

import codes.acegym.Objects.Receipt;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;

public class ReceiptDAO {

    private static final String BASE_SQL =
            "SELECT " +
                    "    r.ReceiptID, " +
                    "    r.ClientID, " +
                    "    c.FirstName, " +
                    "    c.LastName, " +
                    "    pt.PaymentType, " +
                    "    r.TotalPayment, " +
                    "    r.PaymentDate, " +
                    "    tt.TrainingCategory, " +
                    "    pp.PaymentPeriod, " +                          // from PaymentPeriodTable
                    "    CASE " +
                    "        WHEN r.ClientStaffAssignmentID IS NOT NULL " +
                    "        THEN CONCAT(s.StaffFirstName, ' ', s.StaffLastName) " +
                    "        ELSE NULL " +
                    "    END AS CoachName, " +                          // coach full name or NULL
                    "    ct.ClientType AS MembershipType " +            // membership type or NULL
                    "FROM ReceiptTable r " +
                    "JOIN ClientTable c             ON r.ClientID        = c.ClientID " +
                    "JOIN PaymentTypeTable pt       ON r.PaymentTypeID   = pt.PaymentTypeID " +
                    "JOIN RateTable ra              ON r.RateID          = ra.RateID " +
                    "JOIN TrainingTypeTable tt      ON ra.TrainingTypeID = tt.TrainingTypeID " +
                    "JOIN PaymentPeriodTable pp     ON ra.PaymentPeriodID = pp.PaymentPeriodID " +
                    // Coach: only join if the receipt actually has an assignment
                    "LEFT JOIN ClientStaffAssignmentTable csa " +
                    "                               ON r.ClientStaffAssignmentID = csa.ClientStaffAssignmentID " +
                    "                              AND r.ClientStaffAssignmentID IS NOT NULL " +
                    "LEFT JOIN StaffTable s         ON csa.StaffID = s.StaffID " +
                    // Most recent membership for this client
                    "LEFT JOIN MembershipTable m    ON m.ClientID = r.ClientID " +
                    "                              AND m.MembershipID = (" +
                    "                                  SELECT MembershipID FROM MembershipTable " +
                    "                                  WHERE ClientID = r.ClientID " +
                    "                                  ORDER BY DateApplied DESC LIMIT 1" +
                    "                              ) " +
                    "LEFT JOIN ClientTypeTable ct   ON m.ClientTypeID = ct.ClientTypeID";

    public static ObservableList<Receipt> getAllReceipts() {
        return query(BASE_SQL + " ORDER BY r.PaymentDate DESC", null, null, null);
    }

    public static ObservableList<Receipt> getByDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) return getAllReceipts();
        String sql = BASE_SQL +
                " WHERE DATE(r.PaymentDate) BETWEEN ? AND ?" +
                " ORDER BY r.PaymentDate DESC";
        return query(sql, from.toString(), to.toString(), null);
    }

    public static ObservableList<Receipt> getByDateRangeAndType(
            LocalDate from, LocalDate to, String paymentType) {

        if (from == null || to == null) {
            if (paymentType == null || paymentType.isBlank()) return getAllReceipts();
            String sql = BASE_SQL + " WHERE pt.PaymentType = ? ORDER BY r.PaymentDate DESC";
            return query(sql, paymentType, null, null);
        }
        if (paymentType == null || paymentType.isBlank()) {
            return getByDateRange(from, to);
        }
        String sql = BASE_SQL +
                " WHERE DATE(r.PaymentDate) BETWEEN ? AND ?" +
                "   AND pt.PaymentType = ?" +
                " ORDER BY r.PaymentDate DESC";
        return query(sql, from.toString(), to.toString(), paymentType);
    }

    public static ObservableList<String> getPaymentTypes() {
        ObservableList<String> types = FXCollections.observableArrayList();
        types.add("All");
        String sql = "SELECT DISTINCT PaymentType FROM PaymentTypeTable ORDER BY PaymentType";
        try (Connection con = DBConnector.connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) types.add(rs.getString("PaymentType"));
        } catch (SQLException e) { e.printStackTrace(); }
        return types;
    }

    private static ObservableList<Receipt> query(String sql, String p1, String p2, String p3) {
        ObservableList<Receipt> list = FXCollections.observableArrayList();
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (p1 != null) ps.setString(1, p1);
            if (p2 != null) ps.setString(2, p2);
            if (p3 != null) ps.setString(3, p3);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Receipt(
                            rs.getInt("ReceiptID"),
                            rs.getInt("ClientID"),
                            rs.getString("FirstName"),
                            rs.getString("LastName"),
                            rs.getString("PaymentType"),
                            rs.getDouble("TotalPayment"),
                            rs.getString("PaymentDate"),
                            rs.getString("TrainingCategory"),
                            rs.getString("PaymentPeriod"),   // ← must match alias exactly
                            rs.getString("CoachName"),       // ← must match alias exactly
                            rs.getString("MembershipType")   // ← must match alias exactly
                    ));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
}