package codes.acegym.DB;

import codes.acegym.Objects.ClientType;
import codes.acegym.Objects.PaymentPeriod;
import codes.acegym.Objects.TrainingCategory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;

/**
 * DAO for the Plan Settings screen.
 *
 * Covers:
 *  Section 1 – PaymentPeriodTable  (PaymentPeriodID, PaymentPeriod, Days)
 *  Section 2 – TrainingTypeTable   (TrainingTypeID, TrainingCategory, PriceOfCategory, Coaching_Fee)
 *  Section 3 – ClientTypeTable     (ClientTypeID, ClientType, Discount — type names are fixed)
 *
 * NOTE: RateTable columns in the real DB are:
 *   RateID, TrainingTypeID, PaymentPeriodID, ClientTypeID, FinalPrice
 * — NOT "Rate". All guard checks below use FinalPrice / ClientTypeID accordingly.
 */
public class PlanDAO {

    // ═════════════════════════════════════════════════════════════
    // SECTION 1 — PAYMENT PERIOD
    // ═════════════════════════════════════════════════════════════

    /** Load all payment periods ordered by Days ascending. */
    public static ObservableList<PaymentPeriod> getAllPaymentPeriods() {
        ObservableList<PaymentPeriod> list = FXCollections.observableArrayList();
        String sql = "SELECT PaymentPeriodID, PaymentPeriod, Days " +
                "FROM PaymentPeriodTable ORDER BY Days";
        try (Connection con = DBConnector.connect();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new PaymentPeriod(
                        rs.getInt("PaymentPeriodID"),
                        rs.getString("PaymentPeriod"),
                        rs.getInt("Days")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /**
     * Insert a new payment period.
     * @param periodName  display name (e.g. "Monthly")
     * @param days        actual days stored in DB (caller converts months → days)
     * @return "OK" or an error message.
     */
    public static String addPaymentPeriod(String periodName, int days) {
        if (periodName == null || periodName.isBlank())
            return "Period name cannot be empty.";
        if (days <= 0)
            return "Days must be greater than 0.";

        // Duplicate name check
        String chkName = "SELECT COUNT(*) FROM PaymentPeriodTable " +
                "WHERE LOWER(TRIM(PaymentPeriod)) = LOWER(TRIM(?))";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(chkName)) {
            ps.setString(1, periodName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0)
                    return "A payment period with that name already exists.";
            }
        } catch (SQLException e) { return "Database error: " + e.getMessage(); }

        // Duplicate days check
        String chkDays = "SELECT COUNT(*) FROM PaymentPeriodTable WHERE Days = ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(chkDays)) {
            ps.setInt(1, days);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0)
                    return "A payment period with " + days + " day(s) already exists.";
            }
        } catch (SQLException e) { return "Database error: " + e.getMessage(); }

        String sql = "INSERT INTO PaymentPeriodTable (PaymentPeriod, Days) VALUES (?, ?)";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, periodName.trim());
            ps.setInt(2, days);
            return ps.executeUpdate() > 0 ? "OK" : "Insert failed.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Database error: " + e.getMessage();
        }
    }

    /**
     * Update an existing payment period.
     * @return "OK" or an error message.
     */
    public static String updatePaymentPeriod(int id, String periodName, int days) {
        if (periodName == null || periodName.isBlank())
            return "Period name cannot be empty.";
        if (days <= 0)
            return "Days must be greater than 0.";

        // Duplicate name check (excluding self)
        String chkName = "SELECT COUNT(*) FROM PaymentPeriodTable " +
                "WHERE LOWER(TRIM(PaymentPeriod)) = LOWER(TRIM(?)) AND PaymentPeriodID != ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(chkName)) {
            ps.setString(1, periodName.trim());
            ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0)
                    return "Another payment period with that name already exists.";
            }
        } catch (SQLException e) { return "Database error: " + e.getMessage(); }

        // Duplicate days check (excluding self)
        String chkDays = "SELECT COUNT(*) FROM PaymentPeriodTable WHERE Days = ? AND PaymentPeriodID != ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(chkDays)) {
            ps.setInt(1, days);
            ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0)
                    return "Another payment period with " + days + " day(s) already exists.";
            }
        } catch (SQLException e) { return "Database error: " + e.getMessage(); }

        String sql = "UPDATE PaymentPeriodTable SET PaymentPeriod = ?, Days = ? WHERE PaymentPeriodID = ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, periodName.trim());
            ps.setInt(2, days);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0 ? "OK" : "No rows updated.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Database error: " + e.getMessage();
        }
    }

    /**
     * Delete a payment period.
     * Guard: refuses if used in RateTable (column PaymentPeriodID).
     * @return "OK" or an error message.
     */
    public static String deletePaymentPeriod(int id) {
        String chk = "SELECT COUNT(*) FROM RateTable WHERE PaymentPeriodID = ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(chk)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0)
                    return "Cannot delete: this period is linked to existing rates.\nRemove those rates first.";
            }
        } catch (SQLException e) { return "Database error: " + e.getMessage(); }

        String sql = "DELETE FROM PaymentPeriodTable WHERE PaymentPeriodID = ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0 ? "OK" : "No rows deleted.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Database error: " + e.getMessage();
        }
    }

    // ═════════════════════════════════════════════════════════════
    // SECTION 2 — TRAINING CATEGORY
    // ═════════════════════════════════════════════════════════════

    /** Load all training categories ordered alphabetically. */
    public static ObservableList<TrainingCategory> getAllTrainingCategories() {
        ObservableList<TrainingCategory> list = FXCollections.observableArrayList();
        String sql = "SELECT TrainingTypeID, TrainingCategory, PriceOfCategory, Coaching_Fee " +
                "FROM TrainingTypeTable ORDER BY TrainingCategory";
        try (Connection con = DBConnector.connect();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new TrainingCategory(
                        rs.getInt("TrainingTypeID"),
                        rs.getString("TrainingCategory"),
                        rs.getDouble("PriceOfCategory"),
                        rs.getDouble("Coaching_Fee")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /**
     * Insert a new training category.
     * @return "OK" or an error message.
     */
    public static String addTrainingCategory(String categoryName, double price, double coachingFee) {
        if (categoryName == null || categoryName.isBlank())
            return "Category name cannot be empty.";
        if (price < 0)
            return "Price cannot be negative.";
        if (coachingFee < 0)
            return "Coaching fee cannot be negative.";

        // Duplicate check
        String chk = "SELECT COUNT(*) FROM TrainingTypeTable " +
                "WHERE LOWER(TRIM(TrainingCategory)) = LOWER(TRIM(?))";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(chk)) {
            ps.setString(1, categoryName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0)
                    return "A category with that name already exists.";
            }
        } catch (SQLException e) { return "Database error: " + e.getMessage(); }

        String sql = "INSERT INTO TrainingTypeTable (TrainingCategory, PriceOfCategory, Coaching_Fee) VALUES (?, ?, ?)";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, categoryName.trim());
            ps.setDouble(2, price);
            ps.setDouble(3, coachingFee);
            return ps.executeUpdate() > 0 ? "OK" : "Insert failed.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Database error: " + e.getMessage();
        }
    }

    /**
     * Update an existing training category.
     * @return "OK" or an error message.
     */
    public static String updateTrainingCategory(int id, String categoryName, double price, double coachingFee) {
        if (categoryName == null || categoryName.isBlank())
            return "Category name cannot be empty.";
        if (price < 0)
            return "Price cannot be negative.";
        if (coachingFee < 0)
            return "Coaching fee cannot be negative.";

        // Duplicate name check (excluding self)
        String chk = "SELECT COUNT(*) FROM TrainingTypeTable " +
                "WHERE LOWER(TRIM(TrainingCategory)) = LOWER(TRIM(?)) AND TrainingTypeID != ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(chk)) {
            ps.setString(1, categoryName.trim());
            ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0)
                    return "Another category with that name already exists.";
            }
        } catch (SQLException e) { return "Database error: " + e.getMessage(); }

        String sql = "UPDATE TrainingTypeTable " +
                "SET TrainingCategory = ?, PriceOfCategory = ?, Coaching_Fee = ? " +
                "WHERE TrainingTypeID = ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, categoryName.trim());
            ps.setDouble(2, price);
            ps.setDouble(3, coachingFee);
            ps.setInt(4, id);
            return ps.executeUpdate() > 0 ? "OK" : "No rows updated.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Database error: " + e.getMessage();
        }
    }

    /**
     * Delete a training category.
     * Guards: refuses if used in RateTable or StaffTable.
     * @return "OK" or an error message.
     */
    public static String deleteTrainingCategory(int id) {
        try (Connection con = DBConnector.connect()) {
            // Guard: RateTable
            String chkRate = "SELECT COUNT(*) FROM RateTable WHERE TrainingTypeID = ?";
            try (PreparedStatement ps = con.prepareStatement(chkRate)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0)
                        return "Cannot delete: this category is linked to existing rates.\nRemove those rates first.";
                }
            }
            // Guard: StaffTable
            String chkStaff = "SELECT COUNT(*) FROM StaffTable WHERE TrainingTypeID = ?";
            try (PreparedStatement ps = con.prepareStatement(chkStaff)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0)
                        return "Cannot delete: one or more coaches are assigned to this category.";
                }
            }
        } catch (SQLException e) { return "Database error: " + e.getMessage(); }

        String sql = "DELETE FROM TrainingTypeTable WHERE TrainingTypeID = ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0 ? "OK" : "No rows deleted.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Database error: " + e.getMessage();
        }
    }

    // ═════════════════════════════════════════════════════════════
    // SECTION 3 — CLIENT DISCOUNT
    // ═════════════════════════════════════════════════════════════

    /** Load all client types ordered by ClientTypeID. */
    public static ObservableList<ClientType> getAllClientTypes() {
        ObservableList<ClientType> list = FXCollections.observableArrayList();
        String sql = "SELECT ClientTypeID, ClientType, Discount FROM ClientTypeTable ORDER BY ClientTypeID";
        try (Connection con = DBConnector.connect();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new ClientType(
                        rs.getInt("ClientTypeID"),
                        rs.getString("ClientType"),
                        nvl(rs.getString("Discount"), "0%")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /**
     * Update only the Discount column for a given ClientTypeID.
     * Accepts "40" or "40%" — always stores with "%".
     * @return "OK" or an error message.
     */
    public static String updateDiscount(int clientTypeID, String discountStr) {
        if (discountStr == null || discountStr.isBlank())
            return "Discount value cannot be empty.";

        String raw = discountStr.trim().replace("%", "");
        double val;
        try {
            val = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return "Discount must be a valid number (e.g. 30 or 30%).";
        }
        if (val < 0 || val > 100)
            return "Discount must be between 0 and 100.";

        String stored = raw + "%";
        String sql    = "UPDATE ClientTypeTable SET Discount = ? WHERE ClientTypeID = ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, stored);
            ps.setInt(2, clientTypeID);
            return ps.executeUpdate() > 0 ? "OK" : "No rows updated.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Database error: " + e.getMessage();
        }
    }

    // ─────────────────────────────────────────────────────────────
    private static String nvl(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }
}