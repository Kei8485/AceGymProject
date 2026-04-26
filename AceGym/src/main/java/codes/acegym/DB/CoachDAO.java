package codes.acegym.DB;

import codes.acegym.Objects.Coach;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;

public class CoachDAO {

    // ── Fetch all coaches — case-insensitive role match ──────────────────────
    public static ObservableList<Coach> getAllCoaches() {
        ObservableList<Coach> list = FXCollections.observableArrayList();

        // LOWER() makes the filter work regardless of how the role is stored
        // e.g. 'Coach', 'coach', 'COACH' all match
        String sql =
                "SELECT s.StaffID, s.StaffFirstName, s.StaffLastName, " +
                        "       COALESCE(s.TrainingTypeID, 0) AS TrainingTypeID, " +
                        "       s.SystemRole, s.StaffImage, " +
                        "       COALESCE(tt.TrainingCategory, 'Unassigned') AS TrainingCategory " +
                        "FROM StaffTable s " +
                        "LEFT JOIN TrainingTypeTable tt ON s.TrainingTypeID = tt.TrainingTypeID " +
                        "WHERE LOWER(s.SystemRole) IN ('staff', 'admin') " + // Only include these two
                        "ORDER BY s.StaffFirstName";

        try (Connection con = DBConnector.connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // DEBUG — prints to console so you can see exactly what was found
        System.out.println("[CoachDAO] getAllCoaches() returned " + list.size() + " coach(es).");
        for (Coach c : list) {
            System.out.println("  → ID:" + c.getStaffID()
                    + " | " + c.getFullName()
                    + " | Role: " + c.getSystemRole()
                    + " | Training: " + c.getTrainingCategory());
        }

        return list;
    }

    // ── Call this once from your app to see ALL staff rows in the console ────
    // Useful to check what SystemRole values actually exist in your DB.
    // Remove or comment out after you confirm everything works.
    public static void debugPrintAllStaff() {
        System.out.println("[CoachDAO] ── All StaffTable rows ──────────────────");
        String sql = "SELECT StaffID, StaffFirstName, StaffLastName, SystemRole FROM StaffTable";
        try (Connection con = DBConnector.connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                System.out.printf("  StaffID=%-4d | %-15s %-15s | Role='%s'%n",
                        rs.getInt("StaffID"),
                        rs.getString("StaffFirstName"),
                        rs.getString("StaffLastName"),
                        rs.getString("SystemRole"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("[CoachDAO] ─────────────────────────────────────────");
    }

    // ── Fetch single coach by ID ─────────────────────────────────────────────
    public static Coach getCoachByID(int staffID) {
        String sql =
                "SELECT s.StaffID, s.StaffFirstName, s.StaffLastName, " +
                        "       COALESCE(s.TrainingTypeID, 0) AS TrainingTypeID, " +
                        "       s.SystemRole, s.StaffImage, " +
                        "       COALESCE(tt.TrainingCategory, 'Unassigned') AS TrainingCategory " +
                        "FROM StaffTable s " +
                        "LEFT JOIN TrainingTypeTable tt ON s.TrainingTypeID = tt.TrainingTypeID " +
                        "WHERE s.StaffID = ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, staffID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // ── Insert new coach ─────────────────────────────────────────────────────
    public static boolean addCoach(String firstName, String lastName,
                                   int trainingTypeID, String imagePath) {
        if (firstName == null || firstName.isBlank()) return false;
        if (lastName  == null || lastName.isBlank())  return false;

        String sql = "INSERT INTO StaffTable " +
                "(StaffFirstName, StaffLastName, TrainingTypeID, SystemRole, StaffImage) " +
                "VALUES (?, ?, ?, 'Staff', ?)";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, firstName.trim());
            ps.setString(2, lastName.trim());
            if (trainingTypeID > 0) ps.setInt(3, trainingTypeID);
            else                    ps.setNull(3, Types.INTEGER);
            ps.setString(4, imagePath);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // ── Update existing coach ────────────────────────────────────────────────
    public static boolean updateCoach(int staffID, String firstName, String lastName,
                                      int trainingTypeID, String imagePath) {
        if (firstName == null || firstName.isBlank()) return false;
        if (lastName  == null || lastName.isBlank())  return false;

        String sql = "UPDATE StaffTable SET StaffFirstName=?, StaffLastName=?, " +
                "TrainingTypeID=?, StaffImage=? WHERE StaffID=?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, firstName.trim());
            ps.setString(2, lastName.trim());
            if (trainingTypeID > 0) ps.setInt(3, trainingTypeID);
            else                    ps.setNull(3, Types.INTEGER);
            ps.setString(4, imagePath);
            ps.setInt(5, staffID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // ── Check if a coach has any assigned clients ────────────────────────────
    // Returns true if there is at least one active assignment for this staff member.
    public static boolean hasClients(int staffID) {
        String sql = "SELECT COUNT(*) FROM ClientStaffAssignmentTable WHERE StaffID = ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, staffID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // ── Delete coach — only allowed when the coach has no assigned clients ───
    // Clients are never force-deleted here; use the Manage Clients modal to
    // remove all assignments first (they fall back to StaffID 1 / "None").
    public static boolean deleteCoach(int staffID) {
        String deleteAccount = "DELETE FROM UserAccountsTable WHERE StaffID = ?";
        String deleteStaff   = "DELETE FROM StaffTable WHERE StaffID = ?";

        try (Connection con = DBConnector.connect()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps1 = con.prepareStatement(deleteAccount);
                 PreparedStatement ps2 = con.prepareStatement(deleteStaff)) {

                // Delete the account first (child row), then the staff (parent row)
                ps1.setInt(1, staffID);
                ps1.executeUpdate(); // OK if 0 rows — coach may not have an account

                ps2.setInt(1, staffID);
                int rows = ps2.executeUpdate();

                con.commit();
                return rows > 0;

            } catch (SQLException e) {
                con.rollback();
                e.printStackTrace();
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    // ── Clients assigned to a coach (name only, for card display) ────────────
    public static ObservableList<String> getClientNamesForCoach(int staffID) {
        ObservableList<String> names = FXCollections.observableArrayList();
        String sql =
                "SELECT CONCAT(c.FirstName, ' ', c.LastName) AS FullName " +
                        "FROM ClientStaffAssignmentTable csa " +
                        "JOIN ClientTable c ON csa.ClientID = c.ClientID " +
                        "WHERE csa.StaffID = ? ORDER BY c.FirstName";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, staffID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) names.add(rs.getString("FullName"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return names;
    }

    // ── Clients NOT yet assigned to this coach ────────────────────────────────
    public static ObservableList<String[]> getUnassignedClients(int staffID) {
        ObservableList<String[]> clients = FXCollections.observableArrayList();
        String sql =
                "SELECT c.ClientID, c.FirstName, c.LastName " +
                        "FROM ClientTable c " +
                        "WHERE c.ClientID NOT IN (" +
                        "    SELECT csa.ClientID FROM ClientStaffAssignmentTable csa WHERE csa.StaffID = ?" +
                        ") ORDER BY c.FirstName";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, staffID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    clients.add(new String[]{
                            String.valueOf(rs.getInt("ClientID")),
                            rs.getString("FirstName"),
                            rs.getString("LastName")
                    });
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return clients;
    }

    // ── Clients already assigned (with assignment ID + coaching price) ─────────
    public static ObservableList<String[]> getAssignedClients(int staffID) {
        ObservableList<String[]> clients = FXCollections.observableArrayList();
        String sql =
                "SELECT csa.ClientStaffAssignmentID, c.ClientID, " +
                        "       c.FirstName, c.LastName, csa.Applied_Coaching_Price " +
                        "FROM ClientStaffAssignmentTable csa " +
                        "JOIN ClientTable c ON csa.ClientID = c.ClientID " +
                        "WHERE csa.StaffID = ? ORDER BY c.FirstName";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, staffID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    clients.add(new String[]{
                            rs.getString("ClientStaffAssignmentID"),
                            rs.getString("ClientID"),
                            rs.getString("FirstName"),
                            rs.getString("LastName"),
                            rs.getString("Applied_Coaching_Price")
                    });
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return clients;
    }

    // ── Assign a client to a coach ────────────────────────────────────────────
    public static boolean assignClient(int staffID, int clientID, double coachingPrice) {
        String sql = "INSERT INTO ClientStaffAssignmentTable " +
                "(ClientID, StaffID, Applied_Coaching_Price) VALUES (?, ?, ?)";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clientID);
            ps.setInt(2, staffID);
            ps.setDouble(3, coachingPrice);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // ── Remove a client assignment ────────────────────────────────────────────
    public static boolean removeClientAssignment(int assignmentID) {
        // receipttable has a FK on ClientStaffAssignmentID — NULL it out first
        // so the assignment row can be deleted without violating the constraint.
        String nullReceipts = "UPDATE receipttable SET ClientStaffAssignmentID = NULL " +
                "WHERE ClientStaffAssignmentID = ?";
        String deleteAssign = "DELETE FROM ClientStaffAssignmentTable " +
                "WHERE ClientStaffAssignmentID = ?";
        try (Connection con = DBConnector.connect()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps1 = con.prepareStatement(nullReceipts);
                 PreparedStatement ps2 = con.prepareStatement(deleteAssign)) {
                ps1.setInt(1, assignmentID);
                ps1.executeUpdate();
                ps2.setInt(1, assignmentID);
                int rows = ps2.executeUpdate();
                con.commit();
                return rows > 0;
            } catch (SQLException e) {
                con.rollback();
                e.printStackTrace();
                return false;
            }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // ── All training types (for dropdowns) ───────────────────────────────────
    public static ObservableList<String[]> getTrainingTypes() {
        ObservableList<String[]> types = FXCollections.observableArrayList();
        String sql = "SELECT TrainingTypeID, TrainingCategory FROM TrainingTypeTable ORDER BY TrainingCategory";
        try (Connection con = DBConnector.connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                types.add(new String[]{
                        rs.getString("TrainingTypeID"),
                        rs.getString("TrainingCategory")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return types;
    }

    // ── Row mapper ────────────────────────────────────────────────────────────
    private static Coach mapRow(ResultSet rs) throws SQLException {
        return new Coach(
                rs.getInt("StaffID"),
                rs.getString("StaffFirstName"),
                rs.getString("StaffLastName"),
                rs.getInt("TrainingTypeID"),
                rs.getString("TrainingCategory"),
                rs.getString("SystemRole"),
                rs.getString("StaffImage")
        );
    }
}