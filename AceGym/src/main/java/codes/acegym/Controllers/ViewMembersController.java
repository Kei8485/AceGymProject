package codes.acegym.Controllers;

import codes.acegym.DB.ClientDAO;
import codes.acegym.Objects.MemberRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;

public class ViewMembersController implements Initializable {

    // ── FXML Bindings ────────────────────────────────────────────────────────
    @FXML private TableView<MemberRow>  membersTable;
    @FXML private ComboBox<String>      entriesCombo;
    @FXML private TextField             searchField;
    @FXML private Button                refreshBtn;
    @FXML private Label                 memberCountLabel;
    @FXML private Label                 filteredCountLabel;
    @FXML private Label                 hintLabel;

    // ── Column declarations ──────────────────────────────────────────────────
    // All Clients view
    @FXML private TableColumn<MemberRow, Integer> nameCol;
    @FXML private TableColumn<MemberRow, String>  typeCol;
    @FXML private TableColumn<MemberRow, String>  membershipCol;
    @FXML private TableColumn<MemberRow, String>  memStatusCol;
    @FXML private TableColumn<MemberRow, String>  lastPlanCol;
    @FXML private TableColumn<MemberRow, String>  planExpiryCol;
    @FXML private TableColumn<MemberRow, String>  planStatusCol;

    // ── Data ────────────────────────────────────────────────────────────────
    private ObservableList<MemberRow> masterList = FXCollections.observableArrayList();
    private FilteredList<MemberRow>   filtered;
    private SortedList<MemberRow>     sorted;

    // ── View modes ───────────────────────────────────────────────────────────
    private static final String VIEW_ALL        = "All Clients";
    private static final String VIEW_PLAN       = "By Plan";
    private static final String VIEW_MEMBERSHIP = "By Membership";
    private static final String VIEW_FULL       = "All Details";

    // ── Init ─────────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupCombo();
        loadData();
        setupSearch();
        setupRefresh();
        setupRowClick();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  COMBO / FILTER
    // ────────────────────────────────────────────────────────────────────────
    private void setupCombo() {
        entriesCombo.setItems(FXCollections.observableArrayList(
                VIEW_ALL, VIEW_PLAN, VIEW_MEMBERSHIP, VIEW_FULL
        ));
        entriesCombo.getSelectionModel().selectFirst();
        entriesCombo.setOnAction(e -> applyView());
    }

    /** Switches visible columns based on the selected filter/view. */
    private void applyView() {
        String view = entriesCombo.getValue();
        if (view == null) view = VIEW_ALL;

        membersTable.getColumns().clear();

        switch (view) {

            // ── 1. All Clients: basic identity only ───────────────────────
            case VIEW_ALL -> {
                membersTable.getColumns().addAll(
                        makeCol("Client",  "fullName",  200),
                        makeCol("Contact", "contact",   140),
                        makeCol("Email",   "email",     220),
                        makeTypeCol()
                );
            }

            // ── 2. By Plan: payment-period centric ────────────────────────
            case VIEW_PLAN -> {
                membersTable.getColumns().addAll(
                        makeCol("Client",       "fullName",          180),
                        makeTypeCol(),
                        makePeriodCol(),           // Daily / Monthly / Yearly with colour dot
                        makeCol("Paid On",      "lastPaymentDate",   130),
                        makeCol("Plan Expires", "planExpiry",        130),
                        makePlanStatusCol(),
                        makeCol("Coach",        "coachName",         140),
                        makeCol("Training",     "trainingCategory",  120)
                );
            }

            // ── 3. By Membership: membership-date centric ─────────────────
            case VIEW_MEMBERSHIP -> {
                membersTable.getColumns().addAll(
                        makeCol("Client",             "fullName",          180),
                        makeTypeCol(),
                        makeCol("Mem. Applied",       "membershipApplied", 140),
                        makeCol("Mem. Expires",       "membershipExpired", 140),
                        makeMembershipStatusCol(),
                        makeCol("Last Plan",          "lastPaymentPeriod", 110),
                        makeCol("Plan Expires",       "planExpiry",        130),
                        makePlanStatusCol()
                );
            }

            // ── 4. All Details: everything ────────────────────────────────
            case VIEW_FULL -> {
                membersTable.getColumns().addAll(
                        makeCol("Client",         "fullName",          170),
                        makeCol("Contact",        "contact",           120),
                        makeTypeCol(),
                        makeCol("Mem. Applied",   "membershipApplied", 120),
                        makeCol("Mem. Expires",   "membershipExpired", 120),
                        makeMembershipStatusCol(),
                        makePeriodCol(),
                        makeCol("Paid On",        "lastPaymentDate",   115),
                        makeCol("Plan Expires",   "planExpiry",        115),
                        makePlanStatusCol(),
                        makeCol("Coach",          "coachName",         130),
                        makeCol("Training",       "trainingCategory",  110)
                );
            }
        }

        // Re-bind sorted list after column swap
        membersTable.setItems(sorted);
        updateFooter();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  DATA LOAD
    // ────────────────────────────────────────────────────────────────────────
    private void loadData() {
        masterList = ClientDAO.getMemberRows();

        filtered = new FilteredList<>(masterList, r -> true);
        sorted   = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(membersTable.comparatorProperty());

        applyView();
        memberCountLabel.setText(masterList.size() + " clients total");
        updateFooter();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  SEARCH
    // ────────────────────────────────────────────────────────────────────────
    private void setupSearch() {
        searchField.textProperty().addListener((obs, old, val) -> {
            String q = val == null ? "" : val.trim().toLowerCase();
            filtered.setPredicate(row -> {
                if (q.isEmpty()) return true;
                return row.getFullName().toLowerCase().contains(q)
                        || row.getContact().toLowerCase().contains(q)
                        || row.getEmail().toLowerCase().contains(q)
                        || row.getClientType().toLowerCase().contains(q)
                        || row.getCoachName().toLowerCase().contains(q)
                        || row.getTrainingCategory().toLowerCase().contains(q)
                        || row.getLastPaymentPeriod().toLowerCase().contains(q)
                        || row.getMembershipStatus().toLowerCase().contains(q)
                        || row.getPlanStatus().toLowerCase().contains(q);
            });
            updateFooter();
        });
    }

    // ────────────────────────────────────────────────────────────────────────
    //  REFRESH
    // ────────────────────────────────────────────────────────────────────────
    private void setupRefresh() {
        refreshBtn.setOnAction(e -> {
            searchField.clear();
            loadData();
        });
    }

    // ────────────────────────────────────────────────────────────────────────
    //  ROW CLICK — placeholder for detail popup
    // ────────────────────────────────────────────────────────────────────────
    private void setupRowClick() {
        membersTable.setRowFactory(tv -> {
            TableRow<MemberRow> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    MemberRow r = row.getItem();
                    showClientDetail(r);
                }
            });
            return row;
        });
    }

    private void showClientDetail(MemberRow r) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Client Detail");
        alert.setHeaderText(r.getFullName());
        alert.setContentText(
                "Contact:          " + r.getContact()            + "\n" +
                        "Email:            " + r.getEmail()              + "\n" +
                        "Type:             " + r.getClientType()         + "\n" +
                        "\n── Membership ──────────────────\n" +
                        "Applied:          " + r.getMembershipApplied()  + "\n" +
                        "Expires:          " + r.getMembershipExpired()  + "\n" +
                        "Mem. Status:      " + r.getMembershipStatus()   + "\n" +
                        "\n── Last Plan ───────────────────\n" +
                        "Period:           " + r.getLastPaymentPeriod()  + "\n" +
                        "Paid On:          " + r.getLastPaymentDate()    + "\n" +
                        "Plan Expires:     " + r.getPlanExpiry()         + "\n" +
                        "Plan Status:      " + r.getPlanStatus()         + "\n" +
                        "\n── Coach ───────────────────────\n" +
                        "Coach:            " + r.getCoachName()          + "\n" +
                        "Training:         " + r.getTrainingCategory()   + "\n" +
                        "\n── Last Payment ────────────────\n" +
                        "Amount:           ₱" + String.format("%.2f", r.getLastPaymentAmount())
        );
        alert.showAndWait();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  COLUMN FACTORIES
    // ────────────────────────────────────────────────────────────────────────

    /** Plain text column. */
    private TableColumn<MemberRow, String> makeCol(String title, String property, double width) {
        TableColumn<MemberRow, String> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setPrefWidth(width);
        col.setReorderable(false);
        return col;
    }

    /** "Type" column: Member = green badge, Non Member = muted badge. */
    private TableColumn<MemberRow, String> makeTypeCol() {
        TableColumn<MemberRow, String> col = new TableColumn<>("Type");
        col.setCellValueFactory(new PropertyValueFactory<>("clientType"));
        col.setPrefWidth(110);
        col.setReorderable(false);
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                setGraphic(null);
                if (empty || val == null) { setText(null); return; }

                Label badge = new Label(val);
                boolean isMember = "Member".equalsIgnoreCase(val);
                badge.getStyleClass().add(isMember ? "badge-active" : "badge-expired");

                HBox box = new HBox(badge);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
            }
        });
        return col;
    }

    /** "Last Plan" column: shows Daily / Monthly / Yearly with a coloured dot. */
    private TableColumn<MemberRow, String> makePeriodCol() {
        TableColumn<MemberRow, String> col = new TableColumn<>("Plan Period");
        col.setCellValueFactory(new PropertyValueFactory<>("lastPaymentPeriod"));
        col.setPrefWidth(120);
        col.setReorderable(false);
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                setGraphic(null);
                if (empty || val == null || "—".equals(val)) {
                    setText("—");
                    setStyle("-fx-text-fill: #7a7f94;");
                    return;
                }

                // Dot color by period
                String color = switch (val) {
                    case "Daily"   -> "#facc15"; // yellow
                    case "Monthly" -> "#60a5fa"; // blue
                    case "Yearly"  -> "#a78bfa"; // purple
                    default        -> "#7a7f94";
                };

                Label dot  = new Label("●");
                dot.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 10px;");
                Label text = new Label(val);
                text.setStyle("-fx-text-fill: white; -fx-font-family: 'Inter'; -fx-font-size: 13px;");

                HBox box = new HBox(4, dot, text);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
                setStyle("");
            }
        });
        return col;
    }

    /** Plan Status: Active = green badge, Expired = red badge, No Record = muted. */
    private TableColumn<MemberRow, String> makePlanStatusCol() {
        TableColumn<MemberRow, String> col = new TableColumn<>("Plan Status");
        col.setCellValueFactory(new PropertyValueFactory<>("planStatus"));
        col.setPrefWidth(110);
        col.setReorderable(false);
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                setGraphic(null);
                if (empty || val == null) { setText(null); return; }

                Label badge = new Label(val);
                if ("Active".equals(val))        badge.getStyleClass().add("badge-active");
                else if ("Expired".equals(val))  badge.getStyleClass().add("badge-expired");
                else                             badge.setStyle("-fx-text-fill: #7a7f94; -fx-font-size: 12px;");

                HBox box = new HBox(badge);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
            }
        });
        return col;
    }

    /** Membership Status: Active = green badge, Expired = red badge, None = muted. */
    private TableColumn<MemberRow, String> makeMembershipStatusCol() {
        TableColumn<MemberRow, String> col = new TableColumn<>("Mem. Status");
        col.setCellValueFactory(new PropertyValueFactory<>("membershipStatus"));
        col.setPrefWidth(110);
        col.setReorderable(false);
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                setGraphic(null);
                if (empty || val == null) { setText(null); return; }

                if ("None".equals(val)) {
                    setText("—");
                    setStyle("-fx-text-fill: #7a7f94;");
                    return;
                }

                Label badge = new Label(val);
                if ("Active".equals(val))       badge.getStyleClass().add("badge-active");
                else if ("Expired".equals(val)) badge.getStyleClass().add("badge-expired");

                HBox box = new HBox(badge);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
                setStyle("");
            }
        });
        return col;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  FOOTER
    // ────────────────────────────────────────────────────────────────────────
    private void updateFooter() {
        int shown = (filtered != null) ? filtered.size() : 0;
        int total = masterList.size();
        if (shown == total) {
            filteredCountLabel.setText("Showing all " + total + " clients");
        } else {
            filteredCountLabel.setText("Showing " + shown + " of " + total);
        }
    }
}