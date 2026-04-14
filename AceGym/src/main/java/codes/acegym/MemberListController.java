package codes.acegym;

import javafx.application.Platform;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MemberListController {

    @FXML private TableView<Member> memberTable;
    @FXML private TableColumn<Member, String> nameCol;
    @FXML private TableColumn<Member, Integer> idCol;
    @FXML private TableColumn<Member, LocalDate> enrollCol;
    @FXML private TableColumn<Member, LocalDate> expireCol;

    private ObservableList<Member> memberList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        // ✅ Link columns to model
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        idCol.setCellValueFactory(new PropertyValueFactory<>("memberID"));
        enrollCol.setCellValueFactory(new PropertyValueFactory<>("dateEnrolled"));
        expireCol.setCellValueFactory(new PropertyValueFactory<>("dateExpiration"));

        // ✅ Date formatter
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

        // ✅ Format Date Enrolled column
        enrollCol.setCellFactory(column -> new TableCell<Member, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(formatter));
            }
        });

        // ✅ Format Date Expiration column
        expireCol.setCellFactory(column -> new TableCell<Member, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(formatter));
            }
        });

        // ✅ IMPORTANT: Delay loading until UI is ready
        Platform.runLater(this::loadMembers);
    }

    private void loadMembers() {
        String sql = "SELECT * FROM test";

        try (Connection con = DBConnector.connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            memberList.clear();

            while (rs.next()) {

                Date d1 = rs.getDate("DateEnrolled");
                Date d2 = rs.getDate("DateExpiration");

                memberList.add(new Member(
                        rs.getInt("MemberID"),
                        rs.getString("Name"),
                        d1 != null ? d1.toLocalDate() : null,
                        d2 != null ? d2.toLocalDate() : null
                ));
            }

            memberTable.setItems(memberList);
            memberTable.refresh();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}