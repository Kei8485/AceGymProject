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
     * Returns {RateID, Rate} for the given training-type + payment-period,
     * or {@code null} when no rate is configured.
     */
    public static String[] getRate(int trainingTypeID, int paymentPeriodID) {
        String sql = "SELECT RateID, Rate FROM RateTable WHERE TrainingTypeID = ? AND PaymentPeriodID = ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, trainingTypeID);
            ps.setInt(2, paymentPeriodID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                            String.valueOf(rs.getInt("RateID")),
                            String.valueOf(rs.getDouble("Rate"))
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
     * {StaffID, CoachFullName, Applied_Coaching_Price}, or {@code null}.
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
            // Update existing price
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

        // Insert new
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
     * Inserts a new receipt row.
     * {@code clientStaffAssignmentID} may be {@code null} (no coach).
     *
     * @return the generated ReceiptID, or -1 on failure.
     */
    public static int insertReceipt(int clientID,
                                    int paymentTypeID,
                                    int rateID,
                                    double totalPayment,
                                    LocalDate paymentDate,
                                    Integer clientStaffAssignmentID) {

        String sql =
                "INSERT INTO ReceiptTable " +
                        "(ClientID, PaymentTypeID, RateID, TotalPayment, PaymentDate, ClientStaffAssignmentID) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, clientID);
            ps.setInt(2, paymentTypeID);
            ps.setInt(3, rateID);
            ps.setDouble(4, totalPayment);
            ps.setDate(5, Date.valueOf(paymentDate));

            if (clientStaffAssignmentID != null && clientStaffAssignmentID > 0) {
                ps.setInt(6, clientStaffAssignmentID);
            } else {
                ps.setNull(6, Types.INTEGER);
            }

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }
}