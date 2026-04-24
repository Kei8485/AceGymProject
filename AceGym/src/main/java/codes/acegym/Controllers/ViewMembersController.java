package codes.acegym.Controllers;


import javafx.scene.text.Font;
import codes.acegym.DB.ClientDAO;
import codes.acegym.Objects.MemberRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class ViewMembersController implements Initializable {

    // ── FXML injections — must exactly match fx:id in the FXML ──────────────
    @FXML private TableView<MemberRow> membersTable;
    @FXML private ComboBox<String>     entriesCombo;
    @FXML private TextField            searchField;
    @FXML private Button               refreshBtn;
    @FXML private Label                memberCountLabel;
    @FXML private Label                filteredCountLabel;
    @FXML private Label                hintLabel;

    // ── Internal state ───────────────────────────────────────────────────────
    private ObservableList<MemberRow> masterList;
    private FilteredList<MemberRow>   filtered;
    private SortedList<MemberRow>     sorted;

    // ── View names ───────────────────────────────────────────────────────────
    private static final String VIEW_ALL        = "All Clients";
    private static final String VIEW_PLAN       = "By Plan";
    private static final String VIEW_MEMBERSHIP = "By Membership";
    private static final String VIEW_FULL       = "All Details";

    // ────────────────────────────────────────────────────────────────────────
    //  INIT
    //  Order is critical:
    //    1. Fill combo items (no listener yet)
    //    2. loadData()  →  sets masterList / filtered / sorted, calls applyView()
    //    3. Wire combo listener  →  applyView() is safe because sorted exists
    //    4. Wire search, refresh, row-click
    // ────────────────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        Font.loadFont(getClass().getResourceAsStream("/Font/Inter/Inter-VariableFont_opsz,wght.ttf"), 13);
        Font.loadFont(getClass().getResourceAsStream("/Font/Inter/Inter-Italic-VariableFont_opsz,wght.ttf"), 13);
        Font.loadFont(getClass().getResourceAsStream("/Font/Bebas_Neue/BebasNeue-Regular.ttf"), 26);

        // 1 — populate combo, select default, but DO NOT attach action yet
        entriesCombo.setItems(FXCollections.observableArrayList(
                VIEW_ALL, VIEW_PLAN, VIEW_MEMBERSHIP, VIEW_FULL));
        entriesCombo.getSelectionModel().selectFirst();

        // 2 — load DB rows and push them into the table
        loadData();

        // 3 — now that sorted != null, wire the combo action
        entriesCombo.setOnAction(e -> applyView());

        // 4 — everything else
        setupSearch();
        setupRefresh();
        setupRowDoubleClick();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  DATA LOAD
    // ────────────────────────────────────────────────────────────────────────
    private void loadData() {
        masterList = ClientDAO.getMemberRows();

        filtered = new FilteredList<>(masterList, r -> true);
        sorted   = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(membersTable.comparatorProperty());

        applyView();   // push data + columns into table

        memberCountLabel.setText(masterList.size() + " client(s) total");
        updateFooter();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  VIEW SWITCHING
    // ────────────────────────────────────────────────────────────────────────
    private void applyView() {
        if (sorted == null) return;   // safety guard

        String view = entriesCombo.getValue();
        if (view == null) view = VIEW_ALL;

        membersTable.getColumns().clear();

        switch (view) {

            case VIEW_ALL -> membersTable.getColumns().addAll(
                    col("#",         "clientID",    50),
                    col("Full Name", "fullName",    200),
                    col("Contact",   "contact",     150),
                    col("Email",     "email",        230),
                    typeCol()
            );

            case VIEW_PLAN -> membersTable.getColumns().addAll(
                    col("Full Name",    "fullName",          190),
                    typeCol(),
                    periodCol(),
                    col("Paid On",      "lastPaymentDate",   140),
                    col("Plan Expires", "planExpiry",        140),
                    planStatusCol(),
                    col("Coach",        "coachName",         150),
                    col("Training",     "trainingCategory",  130)
            );

            case VIEW_MEMBERSHIP -> membersTable.getColumns().addAll(
                    col("Full Name",    "fullName",           190),
                    typeCol(),
                    col("Mem. Applied", "membershipApplied",  145),
                    col("Mem. Expires", "membershipExpired",  145),
                    membershipStatusCol(),
                    col("Last Plan",    "lastPaymentPeriod",  110),
                    col("Plan Expires", "planExpiry",         140),
                    planStatusCol()
            );

            case VIEW_FULL -> membersTable.getColumns().addAll(
                    col("#",            "clientID",           50),
                    col("Full Name",    "fullName",           180),
                    col("Contact",      "contact",            130),
                    col("Email",        "email",              200),
                    typeCol(),
                    col("Mem. Applied", "membershipApplied",  130),
                    col("Mem. Expires", "membershipExpired",  130),
                    membershipStatusCol(),
                    periodCol(),
                    col("Paid On",      "lastPaymentDate",    125),
                    col("Plan Expires", "planExpiry",         125),
                    planStatusCol(),
                    col("Coach",        "coachName",          140),
                    col("Training",     "trainingCategory",   120),
                    amountCol()
            );
        }

        membersTable.setItems(sorted);
        updateFooter();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  SEARCH
    // ────────────────────────────────────────────────────────────────────────
    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String q = (newVal == null) ? "" : newVal.trim().toLowerCase();
            filtered.setPredicate(row -> {
                if (q.isEmpty()) return true;
                return contains(row.getFullName(),          q)
                        || contains(row.getContact(),           q)
                        || contains(row.getEmail(),             q)
                        || contains(row.getClientType(),        q)
                        || contains(row.getCoachName(),         q)
                        || contains(row.getTrainingCategory(),  q)
                        || contains(row.getLastPaymentPeriod(), q)
                        || contains(row.getMembershipStatus(),  q)
                        || contains(row.getPlanStatus(),        q);
            });
            updateFooter();
        });
    }

    private boolean contains(String field, String query) {
        return field != null && field.toLowerCase().contains(query);
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
    //  ROW DOUBLE-CLICK → detail popup
    // ────────────────────────────────────────────────────────────────────────
    private void setupRowDoubleClick() {
        membersTable.setRowFactory(tv -> {
            TableRow<MemberRow> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    showDetail(row.getItem());
                }
            });
            return row;
        });
    }

    private void showDetail(MemberRow r) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.setResizable(false);

        // ── Colours (match ViewMembers.css) ─────────────────────────────────
        String BG       = "#0E1223";
        String CARD     = "#1c2237";
        String BORDER   = "#2e3349";
        String DIVIDER  = "#2e3349";
        String GRAY     = "#7a7f94";
        String WHITE    = "white";
        String RED      = "#e53935";
        String GREEN_BG = "rgba(39,174,96,0.18)";
        String GREEN_FG = "#4ade80";
        String RED_BG   = "#2c2337";

        // ── Helper: section label ────────────────────────────────────────────
        Supplier<Region> divider = () -> {
            Region d = new Region();
            d.setPrefHeight(1); d.setMaxHeight(1);
            d.setStyle("-fx-background-color:" + DIVIDER + ";");
            return d;
        };

        BiFunction<String, String, HBox> infoRow = (label, value) -> {
            Label lbl = new Label(label);
            lbl.setStyle("-fx-text-fill:" + GRAY + ";-fx-font-family:'Inter';" +
                    "-fx-font-size:16px;-fx-min-width:90px;");
            Label val = new Label(value == null || value.isBlank() ? "—" : value);
            val.setStyle("-fx-text-fill:" + WHITE + ";-fx-font-family:'Inter';" +
                    "-fx-font-size:16px;");
            val.setWrapText(true);
            val.setMaxWidth(260);
            HBox row = new HBox(12, lbl, val);
            row.setAlignment(Pos.CENTER_LEFT);
            return row;
        };

        BiFunction<String, String, HBox> badgeRow = (label, status) -> {
            Label lbl = new Label(label);
            lbl.setStyle("-fx-text-fill:" + GRAY + ";-fx-font-family:'Inter';" +
                    "-fx-font-size:16px;-fx-min-width:90px;");
            Label badge = new Label(status);
            boolean active  = "Active".equalsIgnoreCase(status);
            boolean none    = "None".equalsIgnoreCase(status) || "No Record".equalsIgnoreCase(status);
            if (none) {
                badge.setText("—");
                badge.setStyle("-fx-text-fill:" + GRAY + ";-fx-font-family:'Inter';-fx-font-size:16px;");
            } else if (active) {
                badge.setStyle("-fx-background-color:" + GREEN_BG + ";-fx-text-fill:" + GREEN_FG + ";" +
                        "-fx-background-radius:6;-fx-padding:3 10;" +
                        "-fx-font-family:'Inter';-fx-font-size:16px;-fx-font-weight:bold;");
            } else {
                badge.setStyle("-fx-background-color:" + RED_BG + ";-fx-text-fill:" + RED + ";" +
                        "-fx-background-radius:6;-fx-padding:3 10;" +
                        "-fx-border-color:#4a1f21;-fx-border-width:1;-fx-border-radius:6;" +
                        "-fx-font-family:'Inter';-fx-font-size:16px;-fx-font-weight:bold;");
            }
            HBox row = new HBox(12, lbl, badge);
            row.setAlignment(Pos.CENTER_LEFT);
            return row;
        };

        Function<String, Label> sectionLabel = (text) -> {
            Label lbl = new Label(text.toUpperCase());
            lbl.setStyle("-fx-text-fill:" + GRAY + ";-fx-font-family:'Inter';" +
                    "-fx-font-size:16px;-fx-font-weight:bold;-fx-letter-spacing:1px;");
            VBox.setMargin(lbl, new Insets(6, 0, 2, 0));
            return lbl;
        };

        // ── Title bar ────────────────────────────────────────────────────────
        // Avatar circle with initials
        String initials = r.getFullName().trim().isEmpty() ? "?" :
                String.valueOf(r.getFullName().trim().charAt(0)).toUpperCase();
        Label avatar = new Label(initials);
        avatar.setPrefSize(44, 44); avatar.setMinSize(44, 44);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-background-color:" + RED + ";-fx-background-radius:22;" +
                "-fx-text-fill:white;-fx-font-family:'Inter';-fx-font-size:21px;" +
                "-fx-font-weight:bold;");

        Label nameLabel = new Label(r.getFullName());
        nameLabel.setStyle("-fx-text-fill:white;-fx-font-family:'Inter';" +
                "-fx-font-size:21px;-fx-font-weight:bold;");

        boolean isMember = "Member".equalsIgnoreCase(r.getClientType());
        Label typeBadge = new Label(r.getClientType());
        typeBadge.setStyle(isMember
                ? "-fx-background-color:" + GREEN_BG + ";-fx-text-fill:" + GREEN_FG + ";" +
                "-fx-background-radius:6;-fx-padding:3 10;" +
                "-fx-font-family:'Inter';-fx-font-size:16px;-fx-font-weight:bold;"
                : "-fx-background-color:" + RED_BG + ";-fx-text-fill:" + RED + ";" +
                "-fx-background-radius:6;-fx-padding:3 10;" +
                "-fx-border-color:#4a1f21;-fx-border-width:1;-fx-border-radius:6;" +
                "-fx-font-family:'Inter';-fx-font-size:16px;-fx-font-weight:bold;");

        VBox nameBlock = new VBox(4, nameLabel, typeBadge);
        nameBlock.setAlignment(Pos.CENTER_LEFT);

        // Close button
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:" + GRAY + ";" +
                "-fx-font-size:14px;-fx-cursor:hand;-fx-padding:4 8;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
                "-fx-background-color:#2e3349;-fx-text-fill:white;" +
                        "-fx-font-size:14px;-fx-cursor:hand;-fx-padding:4 8;-fx-background-radius:6;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:" + GRAY + ";" +
                        "-fx-font-size:14px;-fx-cursor:hand;-fx-padding:4 8;"));
        closeBtn.setOnAction(e -> popup.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox titleBar = new HBox(14, avatar, nameBlock, spacer, closeBtn);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(20, 20, 16, 20));
        titleBar.setStyle("-fx-background-color:" + CARD + ";-fx-background-radius:16 16 0 0;");

        // Allow dragging the popup by the title bar
        final double[] dragDelta = new double[2];
        titleBar.setOnMousePressed(e -> { dragDelta[0] = e.getSceneX(); dragDelta[1] = e.getSceneY(); });
        titleBar.setOnMouseDragged(e -> {
            popup.setX(e.getScreenX() - dragDelta[0]);
            popup.setY(e.getScreenY() - dragDelta[1]);
        });

        // ── Content ──────────────────────────────────────────────────────────
        VBox content = new VBox(8);
        content.setPadding(new Insets(0, 20, 20, 20));
        content.setStyle("-fx-background-color:" + CARD + ";");

        // — Identity section —
        content.getChildren().addAll(
                divider.get(),
                sectionLabel.apply("Identity"),
                infoRow.apply("Contact",  r.getContact()),
                infoRow.apply("Email",    r.getEmail())
        );

        // — Membership section —
        content.getChildren().addAll(
                divider.get(),
                sectionLabel.apply("Membership"),
                infoRow.apply("Applied",  r.getMembershipApplied()),
                infoRow.apply("Expires",  r.getMembershipExpired()),
                badgeRow.apply("Status",  r.getMembershipStatus())
        );

        // — Last Plan section —
        String periodDot = switch (r.getLastPaymentPeriod()) {
            case "Daily"   -> "🟡 ";
            case "Monthly" -> "🔵 ";
            case "Yearly"  -> "🟣 ";
            default        -> "";
        };
        content.getChildren().addAll(
                divider.get(),
                sectionLabel.apply("Last Plan"),
                infoRow.apply("Period",   periodDot + r.getLastPaymentPeriod()),
                infoRow.apply("Paid On",  r.getLastPaymentDate()),
                infoRow.apply("Expires",  r.getPlanExpiry()),
                badgeRow.apply("Status",  r.getPlanStatus())
        );

        // — Coach section —
        content.getChildren().addAll(
                divider.get(),
                sectionLabel.apply("Coach"),
                infoRow.apply("Coach",    r.getCoachName()),
                infoRow.apply("Training", r.getTrainingCategory())
        );

        // — Last Payment section —
        String amtText = r.getLastPaymentAmount() == 0.0
                ? "—"
                : "\u20B1" + String.format("%,.2f", r.getLastPaymentAmount());
        content.getChildren().addAll(
                divider.get(),
                sectionLabel.apply("Last Payment"),
                infoRow.apply("Amount",   amtText)
        );

        // ── Close button at bottom ────────────────────────────────────────────
        Button okBtn = new Button("Close");
        okBtn.setPrefWidth(100); okBtn.setPrefHeight(36);
        okBtn.setStyle("-fx-background-color:" + RED + ";-fx-text-fill:white;" +
                "-fx-font-family:'Inter';-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-background-radius:9;-fx-cursor:hand;");
        okBtn.setOnMouseEntered(e -> okBtn.setStyle(
                "-fx-background-color:#c62828;-fx-text-fill:white;" +
                        "-fx-font-family:'Inter';-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-background-radius:9;-fx-cursor:hand;"));
        okBtn.setOnMouseExited(e -> okBtn.setStyle(
                "-fx-background-color:" + RED + ";-fx-text-fill:white;" +
                        "-fx-font-family:'Inter';-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-background-radius:9;-fx-cursor:hand;"));
        okBtn.setOnAction(e -> popup.close());

        HBox footer = new HBox(okBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 20, 18, 20));
        footer.setStyle("-fx-background-color:" + CARD + ";-fx-background-radius:0 0 16 16;");

        // ── Outer card ────────────────────────────────────────────────────────
        VBox card = new VBox(titleBar, content, footer);
        card.setStyle("-fx-background-color:" + CARD + ";" +
                "-fx-background-radius:16;" +
                "-fx-border-color:" + BORDER + ";" +
                "-fx-border-width:1.5;-fx-border-radius:16;" +
                "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.7),30,0,0,8);");
        card.setMaxWidth(420);

        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color:transparent;");
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 460, Region.USE_COMPUTED_SIZE);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.showAndWait();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  FOOTER
    // ────────────────────────────────────────────────────────────────────────
    private void updateFooter() {
        if (filtered == null || masterList == null) return;
        int shown = filtered.size();
        int total = masterList.size();
        filteredCountLabel.setText(
                shown == total
                        ? "Showing all " + total + " client(s)"
                        : "Showing " + shown + " of " + total);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  COLUMN FACTORY HELPERS
    // ════════════════════════════════════════════════════════════════════════

    /** Plain text column. minWidth = 70% of pref so it never collapses. */
    private TableColumn<MemberRow, String> col(String title, String prop, double pref) {
        TableColumn<MemberRow, String> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(pref);
        c.setMinWidth(Math.max(pref * 0.7, 40));
        c.setReorderable(false);
        return c;
    }

    /** Client Type badge: green = Member, red-muted = Non Member. */
    private TableColumn<MemberRow, String> typeCol() {
        TableColumn<MemberRow, String> c = new TableColumn<>("Type");
        c.setCellValueFactory(new PropertyValueFactory<>("clientType"));
        c.setPrefWidth(120);
        c.setMinWidth(90);
        c.setReorderable(false);
        c.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                setGraphic(null); setText(null);
                if (empty || val == null) return;
                Label badge = new Label(val);
                badge.getStyleClass().add(
                        "Member".equalsIgnoreCase(val) ? "badge-active" : "badge-expired");
                HBox box = new HBox(badge);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        return c;
    }

    /** Plan Period dot: Daily=yellow, Monthly=blue, Yearly=purple. */
    private TableColumn<MemberRow, String> periodCol() {
        TableColumn<MemberRow, String> c = new TableColumn<>("Plan Period");
        c.setCellValueFactory(new PropertyValueFactory<>("lastPaymentPeriod"));
        c.setPrefWidth(125);
        c.setMinWidth(90);
        c.setReorderable(false);
        c.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                setGraphic(null);
                if (empty || val == null || "—".equals(val)) {
                    setText("—"); setStyle("-fx-text-fill:#7a7f94;"); return;
                }
                String dotColor = switch (val) {
                    case "Daily"   -> "#facc15";
                    case "Monthly" -> "#60a5fa";
                    case "Yearly"  -> "#a78bfa";
                    default        -> "#7a7f94";
                };
                Label dot = new Label("●");
                dot.setStyle("-fx-text-fill:" + dotColor + ";-fx-font-size:9px;");
                Label lbl = new Label(val);
                lbl.setStyle("-fx-text-fill:white;-fx-font-family:'Inter';-fx-font-size:13px;");
                HBox box = new HBox(5, dot, lbl);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null); setStyle("");
            }
        });
        return c;
    }

    /** Plan Status: Active=green, Expired=red, No Record=muted dash. */
    private TableColumn<MemberRow, String> planStatusCol() {
        TableColumn<MemberRow, String> c = new TableColumn<>("Plan Status");
        c.setCellValueFactory(new PropertyValueFactory<>("planStatus"));
        c.setPrefWidth(115);
        c.setMinWidth(90);
        c.setReorderable(false);
        c.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                setGraphic(null); setText(null); setStyle("");
                if (empty || val == null) return;
                if ("No Record".equals(val)) {
                    setText("—"); setStyle("-fx-text-fill:#7a7f94;"); return;
                }
                Label badge = new Label(val);
                badge.getStyleClass().add("Active".equals(val) ? "badge-active" : "badge-expired");
                HBox box = new HBox(badge);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        return c;
    }

    /** Membership Status: Active=green, Expired=red, None=muted dash. */
    private TableColumn<MemberRow, String> membershipStatusCol() {
        TableColumn<MemberRow, String> c = new TableColumn<>("Mem. Status");
        c.setCellValueFactory(new PropertyValueFactory<>("membershipStatus"));
        c.setPrefWidth(115);
        c.setMinWidth(90);
        c.setReorderable(false);
        c.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                setGraphic(null); setText(null); setStyle("");
                if (empty || val == null) return;
                if ("None".equals(val)) {
                    setText("—"); setStyle("-fx-text-fill:#7a7f94;"); return;
                }
                Label badge = new Label(val);
                badge.getStyleClass().add("Active".equals(val) ? "badge-active" : "badge-expired");
                HBox box = new HBox(badge);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        return c;
    }

    /** Last Payment Amount formatted as ₱1,234.00 (only in All Details). */
    private TableColumn<MemberRow, Double> amountCol() {
        TableColumn<MemberRow, Double> c = new TableColumn<>("Last Paid");
        c.setCellValueFactory(new PropertyValueFactory<>("lastPaymentAmount"));
        c.setPrefWidth(115);
        c.setMinWidth(90);
        c.setReorderable(false);
        c.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null || val == 0.0) {
                    setText("—"); setStyle("-fx-text-fill:#7a7f94;");
                } else {
                    setText("\u20B1" + String.format("%,.2f", val));
                    setStyle("-fx-text-fill:white;");
                }
            }
        });
        return c;
    }
}