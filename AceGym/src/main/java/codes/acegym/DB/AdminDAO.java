package codes.acegym.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the Admin Profile page.
 *
 * Tables used:
 *   StaffTable        → StaffID, StaffFirstName, StaffLastName, TrainingTypeID, SystemRole, StaffImage
 *   UserAccountsTable → StaffID, Username, Password
 */
public class AdminDAO {

    // ════════════════════════════════════════════════════════════════════════
    // RECORDS
    // ════════════════════════════════════════════════════════════════════════

    /** Full profile — used by the Admin Profile page. */
    public record StaffProfile(
            int    staffID,
            String firstName,
            String lastName,
            String username,
            String systemRole,
            String staffImage
    ) {}

    /** Lightweight row — used to populate the register-dialog combo box. */
    public record StaffBasicInfo(
            int    staffID,
            String firstName,
            String lastName,
            String systemRole
    ) {
        /** Display string shown in the combo box. */
        public String displayName() {
            return firstName + " " + lastName + " — " + systemRole;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // READ
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Load the full profile for the currently logged-in user.
     */
    public static StaffProfile getProfileByUsername(String username) {
        String sql =
                "SELECT s.StaffID, s.StaffFirstName, s.StaffLastName, " +
                        "       s.SystemRole, s.StaffImage, u.Username " +
                        "FROM UserAccountsTable u " +
                        "JOIN StaffTable s ON u.StaffID = s.StaffID " +
                        "WHERE u.Username = ? LIMIT 1";

        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new StaffProfile(
                            rs.getInt("StaffID"),
                            rs.getString("StaffFirstName"),
                            rs.getString("StaffLastName"),
                            rs.getString("Username"),
                            rs.getString("SystemRole"),
                            rs.getString("StaffImage")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns all staff who do NOT yet have a UserAccountsTable entry,
     * filtered by the intended role ("Admin" or "Staff").
     * Pass null or blank to return all unregistered staff regardless of role.
     */
    public static List<StaffBasicInfo> getStaffWithoutAccount(String role) {
        List<StaffBasicInfo> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT s.StaffID, s.StaffFirstName, s.StaffLastName, s.SystemRole " +
                        "FROM StaffTable s " +
                        "LEFT JOIN UserAccountsTable u ON s.StaffID = u.StaffID " +
                        "WHERE u.StaffID IS NULL " +
                        "  AND s.SystemRole <> 'None' "
        );
        if (role != null && !role.isBlank()) {
            sql.append("AND s.SystemRole = ? ");
        }
        sql.append("ORDER BY s.StaffFirstName, s.StaffLastName");

        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            if (role != null && !role.isBlank()) {
                ps.setString(1, role);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new StaffBasicInfo(
                            rs.getInt("StaffID"),
                            rs.getString("StaffFirstName"),
                            rs.getString("StaffLastName"),
                            rs.getString("SystemRole")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<StaffBasicInfo> getAllStaff() {
        List<StaffBasicInfo> list = new ArrayList<>();
        String sql =
                "SELECT StaffID, StaffFirstName, StaffLastName, SystemRole " +
                        "FROM StaffTable " +
                        "WHERE SystemRole <> 'None' " +
                        "ORDER BY StaffFirstName, StaffLastName";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new StaffBasicInfo(
                        rs.getInt("StaffID"),
                        rs.getString("StaffFirstName"),
                        rs.getString("StaffLastName"),
                        rs.getString("SystemRole")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Returns true if the username is already in use in UserAccountsTable.
     */
    public static boolean isUsernameTaken(String username) {
        String sql = "SELECT 1 FROM UserAccountsTable WHERE Username = ? LIMIT 1";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Same as isUsernameTaken but excludes a specific staffID.
     * Used when saving profile changes so the current user's own username
     * does not trigger a false "already taken" error.
     */
    public static boolean isUsernameTakenByOther(String username, int excludeStaffID) {
        String sql =
                "SELECT 1 FROM UserAccountsTable " +
                        "WHERE Username = ? AND StaffID <> ? LIMIT 1";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ps.setInt(2, excludeStaffID);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // UPDATE — Account Information
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Updates the username in UserAccountsTable for the given staffID.
     */
    public static boolean updateUsername(int staffID, String newUsername) {
        String sql = "UPDATE UserAccountsTable SET Username = ? WHERE StaffID = ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newUsername.trim());
            ps.setInt(2, staffID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates the profile image path in StaffTable for the given staffID.
     */
    public static boolean updateStaffImage(int staffID, String imagePath) {
        String sql = "UPDATE StaffTable SET StaffImage = ? WHERE StaffID = ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, imagePath);
            ps.setInt(2, staffID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // UPDATE — Password
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Verifies the current password, then updates to the new one.
     * Returns false if the current password is wrong.
     */
    public static boolean changePassword(int staffID,
                                         String currentPassword,
                                         String newPassword) {
        String verify = "SELECT 1 FROM UserAccountsTable WHERE StaffID = ? AND Password = ? LIMIT 1";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(verify)) {
            ps.setInt(1, staffID);
            ps.setString(2, currentPassword);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
            }
        } catch (SQLException e) { e.printStackTrace(); return false; }

        String update = "UPDATE UserAccountsTable SET Password = ? WHERE StaffID = ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(update)) {
            ps.setString(1, newPassword);
            ps.setInt(2, staffID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // ════════════════════════════════════════════════════════════════════════
    // REGISTER
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Register an account for an EXISTING staff member.
     *
     * The uniqueness check and the INSERT happen inside the same transaction
     * so there is no TOCTOU gap — even if two requests race, only one wins.
     *
     * Return values (as String tokens, evaluated by the controller):
     *   "OK"             — success
     *   "USERNAME_TAKEN" — username already exists
     *   "ERROR"          — any other DB failure
     */
    public static String registerAccountForStaff(int staffID,
                                                 String username,
                                                 String password,
                                                 String role) {
        String checkUser  = "SELECT 1 FROM UserAccountsTable WHERE Username = ? LIMIT 1";
        String updateRole = "UPDATE StaffTable SET SystemRole = ? WHERE StaffID = ?";
        String insertUser = "INSERT INTO UserAccountsTable (StaffID, Username, Password) VALUES (?, ?, ?)";

        try (Connection con = DBConnector.connect()) {
            con.setAutoCommit(false);
            try {
                // ── 1. Re-check uniqueness INSIDE the transaction ──────────
                try (PreparedStatement ps = con.prepareStatement(checkUser)) {
                    ps.setString(1, username.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            con.rollback();
                            return "USERNAME_TAKEN";
                        }
                    }
                }
                // ── 2. Sync role on the staff row ──────────────────────────
                try (PreparedStatement ps = con.prepareStatement(updateRole)) {
                    ps.setString(1, role);
                    ps.setInt(2, staffID);
                    ps.executeUpdate();
                }
                // ── 3. Create the account ──────────────────────────────────
                try (PreparedStatement ps = con.prepareStatement(insertUser)) {
                    ps.setInt(1, staffID);
                    ps.setString(2, username.trim());
                    ps.setString(3, password);
                    ps.executeUpdate();
                }
                con.commit();
                return "OK";
            } catch (SQLException e) {
                con.rollback();
                e.printStackTrace();
                // MySQL error 1062 = duplicate entry (UNIQUE constraint)
                if (e.getErrorCode() == 1062) return "USERNAME_TAKEN";
                return "ERROR";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    /**
     * Returns true if the given staffID already has a row in UserAccountsTable.
     */
    public static boolean staffHasAccount(int staffID) {
        String sql = "SELECT 1 FROM UserAccountsTable WHERE StaffID = ? LIMIT 1";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, staffID);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * FLOW B — Creates a brand-new staff row in StaffTable AND a matching
     * UserAccountsTable row in one transaction.
     */
    public static boolean registerAccount(String firstName,
                                          String lastName,
                                          String username,
                                          String password,
                                          String role) {
        if (isUsernameTaken(username)) return false;

        String insertStaff =
                "INSERT INTO StaffTable (StaffFirstName, StaffLastName, TrainingTypeID, SystemRole, StaffImage) " +
                        "VALUES (?, ?, 4, ?, 'none.png')";
        String insertUser =
                "INSERT INTO UserAccountsTable (StaffID, Username, Password) VALUES (?, ?, ?)";

        try (Connection con = DBConnector.connect()) {
            con.setAutoCommit(false);
            try (PreparedStatement psStaff = con.prepareStatement(
                    insertStaff, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement psUser = con.prepareStatement(insertUser)) {

                psStaff.setString(1, firstName.trim());
                psStaff.setString(2, lastName.trim());
                psStaff.setString(3, role);
                psStaff.executeUpdate();

                try (ResultSet keys = psStaff.getGeneratedKeys()) {
                    if (!keys.next()) { con.rollback(); return false; }
                    int newStaffID = keys.getInt(1);
                    psUser.setInt(1, newStaffID);
                    psUser.setString(2, username.trim());
                    psUser.setString(3, password);
                    psUser.executeUpdate();
                }

                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                e.printStackTrace();
                return false;
            }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /**
     * Updates first name, last name, username, and profile image atomically.
     * Checks that the new username is not already taken by a DIFFERENT staff
     * member before writing — returns "USERNAME_TAKEN" if so.
     */
    public static String updateAdminProfile(int staffId,
                                            String fName,
                                            String lName,
                                            String uName,
                                            String imagePath) {
        String checkUser  =
                "SELECT 1 FROM UserAccountsTable WHERE Username = ? AND StaffID <> ? LIMIT 1";
        String sqlStaff   =
                "UPDATE StaffTable SET StaffFirstName = ?, StaffLastName = ?, StaffImage = ? WHERE StaffID = ?";
        String sqlAccount =
                "UPDATE UserAccountsTable SET Username = ? WHERE StaffID = ?";

        try (Connection con = DBConnector.connect()) {
            con.setAutoCommit(false);
            try {
                // ── 1. Uniqueness check for the new username ───────────────
                try (PreparedStatement ps = con.prepareStatement(checkUser)) {
                    ps.setString(1, uName.trim());
                    ps.setInt(2, staffId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            con.rollback();
                            return "USERNAME_TAKEN";
                        }
                    }
                }
                // ── 2. Update StaffTable ───────────────────────────────────
                try (PreparedStatement ps = con.prepareStatement(sqlStaff)) {
                    ps.setString(1, fName);
                    ps.setString(2, lName);
                    ps.setString(3, imagePath);
                    ps.setInt(4, staffId);
                    ps.executeUpdate();
                }
                // ── 3. Update UserAccountsTable ────────────────────────────
                try (PreparedStatement ps = con.prepareStatement(sqlAccount)) {
                    ps.setString(1, uName.trim());
                    ps.setInt(2, staffId);
                    ps.executeUpdate();
                }
                con.commit();
                return "OK";
            } catch (SQLException e) {
                con.rollback();
                e.printStackTrace();
                if (e.getErrorCode() == 1062) return "USERNAME_TAKEN";
                return "ERROR";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR";
        }
    }
}