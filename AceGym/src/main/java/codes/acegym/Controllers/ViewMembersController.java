package codes.acegym.Controllers;


import codes.acegym.DB.ExpiryResetDAO;
import javafx.animation.ScaleTransition;
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
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class ViewMembersController implements Initializable {

    // ── FXML injections ──────────────────────────────────────────────────────
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

    // ── Shared colour palette ────────────────────────────────────────────────
    private static final String CARD      = "#1c2237";
    private static final String CARD_ALT  = "#202840";   // slightly lighter for edit fields
    private static final String BORDER    = "#2e3349";
    private static final String DIVIDER   = "#2e3349";
    private static final String GRAY      = "#7a7f94";
    private static final String WHITE     = "white";
    private static final String RED       = "#e53935";
    private static final String RED_DARK  = "#c62828";
    private static final String GREEN_BG  = "rgba(39,174,96,0.18)"; // From .badge-active
    private static final String GREEN_FG  = "#4ade80";               // From .badge-active
    private static final String RED_BG    = "#2c2337";               // From .badge-expired / -fx-accent-red-muted
    private static final String AMBER     = "#e53935";               // From .badge-expired / -fx-accent-red
    private static final String AMBER_BG  = "#2c2337";               // From .badge-expired / -fx-accent-red-muted         // From .badge-expired / -fx-accent-red-muted

    // ── Shared button-style helpers ──────────────────────────────────────────
    private static String btnStyle(String bg, String fg) {
        return "-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                "-fx-font-family:'Inter';-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-background-radius:9;-fx-cursor:hand;";
    }

    // ────────────────────────────────────────────────────────────────────────
    //  INIT
    // ────────────────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Font.loadFont(getClass().getResourceAsStream("/Font/Inter/Inter-VariableFont_opsz,wght.ttf"), 13);
        Font.loadFont(getClass().getResourceAsStream("/Font/Inter/Inter-Italic-VariableFont_opsz,wght.ttf"), 13);
        Font.loadFont(getClass().getResourceAsStream("/Font/Bebas_Neue/BebasNeue-Regular.ttf"), 26);

        entriesCombo.setItems(FXCollections.observableArrayList(
                VIEW_ALL, VIEW_PLAN, VIEW_MEMBERSHIP, VIEW_FULL));
        entriesCombo.getSelectionModel().selectFirst();

        loadData();
        entriesCombo.setOnAction(e -> applyView());
        setupSearch();
        setupRefresh();
        setupRowDoubleClick();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  DATA LOAD
    // ────────────────────────────────────────────────────────────────────────
    private void loadData() {
        // Fetch from DB
        ExpiryResetDAO.runExpiryResets();
        ObservableList<MemberRow> fresh = ClientDAO.getMemberRows();

        javafx.application.Platform.runLater(() -> {
            if (masterList == null) {
                masterList = fresh;
                filtered   = new FilteredList<>(masterList, r -> true);
                sorted     = new SortedList<>(filtered);
                sorted.comparatorProperty().bind(membersTable.comparatorProperty());

                // Set the table items only once
                membersTable.setItems(sorted);
            } else {
                masterList.setAll(fresh);
            }

            applyView();

            // --- NEW TO OLD SORT LOGIC ---
            // Find the "#" (ID) column and force it to sort descending
            membersTable.getColumns().stream()
                    .filter(c -> c.getText().equals("#"))
                    .findFirst()
                    .ifPresent(idCol -> {
                        idCol.setSortType(TableColumn.SortType.DESCENDING);
                        membersTable.getSortOrder().clear();
                        membersTable.getSortOrder().add(idCol);
                        membersTable.sort();
                    });

            memberCountLabel.setText(masterList.size() + " client(s) total");
            updateFooter();
        });
    }
    // ────────────────────────────────────────────────────────────────────────
    //  VIEW SWITCHING
    // ────────────────────────────────────────────────────────────────────────
    private void applyView() {
        if (sorted == null) return;
        String view = entriesCombo.getValue();
        if (view == null) view = VIEW_ALL;
        membersTable.getColumns().clear();

        switch (view) {
            case VIEW_ALL -> membersTable.getColumns().addAll(
                    col("#",         "clientID",   50),
                    col("Full Name", "fullName",   200),
                    col("Contact",   "contact",    150),
                    col("Email",     "email",      230),
                    typeCol());

            case VIEW_PLAN -> membersTable.getColumns().addAll(
                    col("Full Name",    "fullName",         190),
                    typeCol(),
                    periodCol(),
                    col("Paid On",      "lastPaymentDate",  140),
                    col("Plan Expires", "planExpiry",       140),
                    planStatusCol(),
                    col("Coach",        "coachName",        150),
                    col("Training",     "trainingCategory", 130));

            case VIEW_MEMBERSHIP -> membersTable.getColumns().addAll(
                    col("Full Name",    "fullName",          190),
                    typeCol(),
                    col("Mem. Applied", "membershipApplied", 145),
                    col("Mem. Expires", "membershipExpired", 145),
                    membershipStatusCol(),
                    col("Last Plan",    "lastPaymentPeriod", 110),
                    col("Plan Expires", "planExpiry",        140),
                    planStatusCol());

            case VIEW_FULL -> membersTable.getColumns().addAll(
                    col("#",            "clientID",          50),
                    col("Full Name",    "fullName",          180),
                    col("Contact",      "contact",           130),
                    col("Email",        "email",             200),
                    typeCol(),
                    col("Mem. Applied", "membershipApplied", 130),
                    col("Mem. Expires", "membershipExpired", 130),
                    membershipStatusCol(),
                    periodCol(),
                    col("Paid On",      "lastPaymentDate",   125),
                    col("Plan Expires", "planExpiry",        125),
                    planStatusCol(),
                    col("Coach",        "coachName",         140),
                    col("Training",     "trainingCategory",  120),
                    amountCol());
        }

        // setItems() must only be called ONCE (when sorted is first built in loadData).
        // Re-calling it on every view switch triggers the JavaFX 21 subList crash.
        // The columns change is fine — only the items assignment must be guarded.
        if (membersTable.getItems() != sorted) {
            membersTable.setItems(sorted);
        }
        updateFooter();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  SEARCH
    // ────────────────────────────────────────────────────────────────────────
    private void setupSearch() {
        searchField.textProperty().addListener((obs, o, newVal) -> {
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
            // Clear selection first to avoid reading stale indices while the
            // list mutates — same JavaFX 21 defensive pattern used throughout
            membersTable.getSelectionModel().clearSelection();
            loadData();
        });
    }

    // ────────────────────────────────────────────────────────────────────────
    //  ROW DOUBLE-CLICK
    // ────────────────────────────────────────────────────────────────────────
    private void setupRowDoubleClick() {
        membersTable.setRowFactory(tv -> {
            TableRow<MemberRow> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty())
                    showDetail(row.getItem());
            });
            return row;
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DETAIL POPUP
    // ════════════════════════════════════════════════════════════════════════
    private void showDetail(MemberRow r) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.setResizable(false);

        // ── Helpers ──────────────────────────────────────────────────────────
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
            val.setStyle("-fx-text-fill:" + WHITE + ";-fx-font-family:'Inter';-fx-font-size:16px;");
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
            boolean neutral = "None".equalsIgnoreCase(status)
                    || "No Record".equalsIgnoreCase(status)
                    || "Inactive".equalsIgnoreCase(status);
            if (neutral) {
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

        Function<String, Label> sectionLabel = text -> {
            Label lbl = new Label(text.toUpperCase());
            lbl.setStyle("-fx-text-fill:" + GRAY + ";-fx-font-family:'Inter';" +
                    "-fx-font-size:16px;-fx-font-weight:bold;-fx-letter-spacing:1px;");
            VBox.setMargin(lbl, new Insets(6, 0, 2, 0));
            return lbl;
        };

        // ── Title bar ────────────────────────────────────────────────────────
        String initials = r.getFullName().trim().isEmpty() ? "?" :
                String.valueOf(r.getFullName().trim().charAt(0)).toUpperCase();
        Label avatar = new Label(initials);
        avatar.setPrefSize(44, 44); avatar.setMinSize(44, 44);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-background-color:" + RED + ";-fx-background-radius:22;" +
                "-fx-text-fill:white;-fx-font-family:'Inter';-fx-font-size:21px;-fx-font-weight:bold;");

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

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        HBox titleBar = new HBox(14, avatar, nameBlock, titleSpacer, closeBtn);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(20, 20, 16, 20));
        titleBar.setStyle("-fx-background-color:" + CARD + ";-fx-background-radius:16 16 0 0;");

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

        // — Identity —
        content.getChildren().addAll(
                divider.get(),
                sectionLabel.apply("Identity"),
                infoRow.apply("Contact", r.getContact()),
                infoRow.apply("Email",   r.getEmail()));

        // — Membership —
        content.getChildren().addAll(
                divider.get(),
                sectionLabel.apply("Membership"),
                infoRow.apply("Applied", r.getMembershipApplied()),
                infoRow.apply("Expires", r.getMembershipExpired()),
                badgeRow.apply("Status", r.getMembershipStatus()));

        // — Plan —
        boolean planActive   = "Active".equals(r.getPlanStatus());
        boolean hasAnyRecord = !"No Record".equals(r.getPlanStatus());

        String periodDot = switch (r.getLastPaymentPeriod()) {
            case "Daily"   -> "🟡 ";
            case "Monthly" -> "🔵 ";
            case "Yearly"  -> "🟣 ";
            default        -> "";
        };

        if (planActive) {
            content.getChildren().addAll(
                    divider.get(),
                    sectionLabel.apply("Current Plan"),
                    infoRow.apply("Period",   periodDot + r.getLastPaymentPeriod()),
                    infoRow.apply("Training", r.getTrainingCategory()),
                    infoRow.apply("Paid On",  r.getLastPaymentDate()),
                    infoRow.apply("Expires",  r.getPlanExpiry()),
                    badgeRow.apply("Status",  "Active"));
        } else if (hasAnyRecord) {
            content.getChildren().addAll(
                    divider.get(),
                    sectionLabel.apply("Current Plan"),
                    infoRow.apply("Period",  "—"),
                    infoRow.apply("Expires", "—"),
                    badgeRow.apply("Status", "Inactive"));
            content.getChildren().addAll(
                    divider.get(),
                    sectionLabel.apply("Last Plan"),
                    infoRow.apply("Period",   periodDot + r.getLastPaymentPeriod()),
                    infoRow.apply("Training", r.getTrainingCategory()),
                    infoRow.apply("Paid On",  r.getLastPaymentDate()),
                    infoRow.apply("Expired",  r.getPlanExpiry()),
                    badgeRow.apply("Status",  "Expired"));
        } else {
            content.getChildren().addAll(
                    divider.get(),
                    sectionLabel.apply("Current Plan"),
                    infoRow.apply("Period",  "—"),
                    infoRow.apply("Expires", "—"),
                    badgeRow.apply("Status", "Inactive"));
        }

        // — Coach —
        String coachDisplay = (r.getCoachName() == null
                || r.getCoachName().isBlank()
                || r.getCoachName().equalsIgnoreCase("None None"))
                ? "No coach assigned" : r.getCoachName();
        content.getChildren().addAll(
                divider.get(),
                sectionLabel.apply("Coach"),
                infoRow.apply("Coach",    coachDisplay),
                infoRow.apply("Training", r.getTrainingCategory()));

        // — Last Payment —
        String amtText = r.getLastPaymentAmount() == 0.0
                ? "—" : "\u20B1" + String.format("%,.2f", r.getLastPaymentAmount());
        content.getChildren().addAll(
                divider.get(),
                sectionLabel.apply("Last Payment"),
                infoRow.apply("Amount", amtText));

        // ── Footer: Edit + Close ──────────────────────────────────────────────
        Button editBtn = new Button("✎  Edit Info");
        editBtn.setPrefWidth(120); editBtn.setPrefHeight(36);
        editBtn.setStyle(btnStyle(AMBER_BG, AMBER) +
                "-fx-border-color:" + AMBER + ";-fx-border-width:1;-fx-border-radius:9;");
        editBtn.setOnMouseEntered(e -> editBtn.setStyle(
                // #1e253c matches the navy-blue in your image
                btnStyle("#1e253c", AMBER) +
                        "-fx-border-color:" + AMBER + ";-fx-border-width:1;-fx-border-radius:9;"));
        editBtn.setOnMouseExited(e -> editBtn.setStyle(
                btnStyle(AMBER_BG, AMBER) +
                        "-fx-border-color:" + AMBER + ";-fx-border-width:1;-fx-border-radius:9;"));
        addPressEffect(editBtn);


        editBtn.setOnAction(e -> {
            // Get the main window as owner so the edit popup renders on top
            Stage owner = (Stage) membersTable.getScene().getWindow();
            popup.close();
            // Small delay lets JavaFX fully process the close before showing the next window
            javafx.application.Platform.runLater(() -> showEditPopup(r, owner));

        });

        Button okBtn = new Button("Close");
        okBtn.setPrefWidth(100); okBtn.setPrefHeight(36);
        okBtn.setStyle(btnStyle(RED, WHITE));
        okBtn.setOnMouseEntered(e -> okBtn.setStyle(btnStyle(RED_DARK, WHITE)));
        okBtn.setOnMouseExited(e  -> okBtn.setStyle(btnStyle(RED, WHITE)));
        okBtn.setOnAction(e -> popup.close());
        addPressEffect(okBtn);

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        HBox footer = new HBox(12, editBtn, footerSpacer, okBtn);
        footer.setAlignment(Pos.CENTER_LEFT);
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



    // ════════════════════════════════════════════════════════════════════════
    //  EDIT POPUP
    // ════════════════════════════════════════════════════════════════════════
    private void showEditPopup(MemberRow r, Stage owner) {
        Stage popup = new Stage();
        popup.initOwner(owner);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.setResizable(false);

        // ── Split fullName into first / last ──────────────────────────────────
        String[] parts     = r.getFullName().trim().split(" ", 2);
        String   initFirst = parts[0];
        String   initLast  = parts.length > 1 ? parts[1] : "";

        // ── Field factory ─────────────────────────────────────────────────────
        Function<String, TextField> field = placeholder -> {
            TextField tf = new TextField();
            tf.setPromptText(placeholder);
            tf.setPrefHeight(38);
            tf.setStyle(
                    "-fx-background-color:" + CARD_ALT + ";" +
                            "-fx-text-fill:white;" +
                            "-fx-prompt-text-fill:" + GRAY + ";" +
                            "-fx-font-family:'Inter';-fx-font-size:14px;" +
                            "-fx-background-radius:8;" +
                            "-fx-border-color:" + BORDER + ";" +
                            "-fx-border-width:1;-fx-border-radius:8;" +
                            "-fx-padding:0 10;");
            tf.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                String focused = "-fx-background-color:" + CARD_ALT + ";" +
                        "-fx-text-fill:white;" +
                        "-fx-prompt-text-fill:" + GRAY + ";" +
                        "-fx-font-family:'Inter';-fx-font-size:14px;" +
                        "-fx-background-radius:8;" +
                        "-fx-border-color:" + AMBER + ";" +
                        "-fx-border-width:1.5;-fx-border-radius:8;" +
                        "-fx-padding:0 10;";
                String unfocused = "-fx-background-color:" + CARD_ALT + ";" +
                        "-fx-text-fill:white;" +
                        "-fx-prompt-text-fill:" + GRAY + ";" +
                        "-fx-font-family:'Inter';-fx-font-size:14px;" +
                        "-fx-background-radius:8;" +
                        "-fx-border-color:" + BORDER + ";" +
                        "-fx-border-width:1;-fx-border-radius:8;" +
                        "-fx-padding:0 10;";
                tf.setStyle(isFocused ? focused : unfocused);
            });
            return tf;
        };

        // ── Label factory ─────────────────────────────────────────────────────
        Function<String, Label> fieldLabel = text -> {
            Label lbl = new Label(text);
            lbl.setStyle("-fx-text-fill:" + GRAY + ";-fx-font-family:'Inter';" +
                    "-fx-font-size:12px;-fx-font-weight:bold;");
            VBox.setMargin(lbl, new Insets(8, 0, 3, 0));
            return lbl;
        };

        // ── Fields ───────────────────────────────────────────────────────────
        TextField firstNameField = field.apply("First Name");
        firstNameField.setText(initFirst);

        TextField lastNameField = field.apply("Last Name");
        lastNameField.setText(initLast);

        TextField contactField = field.apply("Contact Number");
        contactField.setText(r.getContact() == null ? "" : r.getContact());

        TextField emailField = field.apply("Email Address");
        emailField.setText(r.getEmail() == null ? "" : r.getEmail());

        // ── Error label ───────────────────────────────────────────────────────
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill:" + RED + ";-fx-font-family:'Inter';" +
                "-fx-font-size:12px;");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);

        // ── Title bar ────────────────────────────────────────────────────────
        String initials = r.getFullName().trim().isEmpty() ? "?" :
                String.valueOf(r.getFullName().trim().charAt(0)).toUpperCase();
        Label avatar = new Label(initials);
        avatar.setPrefSize(44, 44); avatar.setMinSize(44, 44);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-background-color:" + AMBER + ";-fx-background-radius:22;" +
                "-fx-text-fill:#1c2237;-fx-font-family:'Inter';-fx-font-size:21px;-fx-font-weight:bold;");

        Label titleLabel = new Label("Edit Client Info");
        titleLabel.setStyle("-fx-text-fill:white;-fx-font-family:'Inter';" +
                "-fx-font-size:18px;-fx-font-weight:bold;");
        Label subtitleLabel = new Label("ID#: CLID00" + r.getClientID() + "  ·  " + r.getFullName());
        subtitleLabel.setStyle("-fx-text-fill:" + GRAY + ";-fx-font-family:'Inter';-fx-font-size:13px;");

        VBox titleBlock = new VBox(2, titleLabel, subtitleLabel);
        titleBlock.setAlignment(Pos.CENTER_LEFT);

        Button closeTitleBtn = new Button("✕");
        closeTitleBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:" + GRAY + ";" +
                "-fx-font-size:14px;-fx-cursor:hand;-fx-padding:4 8;");
        closeTitleBtn.setOnMouseEntered(e -> closeTitleBtn.setStyle(
                "-fx-background-color:#e53935; -fx-text-fill:white;" +
                        "-fx-font-size:14px; -fx-cursor:hand; -fx-padding:4 8; -fx-background-radius:6;"));
        closeTitleBtn.setOnMouseExited(e -> closeTitleBtn.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:" + GRAY + ";" +
                        "-fx-font-size:14px;-fx-cursor:hand;-fx-padding:4 8;"));
        closeTitleBtn.setOnAction(e -> popup.close());

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        HBox titleBar = new HBox(14, avatar, titleBlock, titleSpacer, closeTitleBtn);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(20, 20, 16, 20));
        titleBar.setStyle("-fx-background-color:" + CARD + ";-fx-background-radius:16 16 0 0;");

        final double[] dragDelta = new double[2];
        titleBar.setOnMousePressed(e  -> { dragDelta[0] = e.getSceneX(); dragDelta[1] = e.getSceneY(); });
        titleBar.setOnMouseDragged(e  -> {
            popup.setX(e.getScreenX() - dragDelta[0]);
            popup.setY(e.getScreenY() - dragDelta[1]);
        });

        // ── Notice banner ─────────────────────────────────────────────────────
        Label notice = new Label("⚠  Only contact details are editable. Membership and plan info cannot be changed here.");
        notice.setWrapText(true);
        notice.setMaxWidth(380);
        notice.setStyle("-fx-text-fill:" + AMBER + ";-fx-font-family:'Inter';-fx-font-size:12px;" +
                "-fx-background-color:" + AMBER_BG + ";" +
                "-fx-background-radius:8;-fx-padding:10 12;");

        // ── Divider line ──────────────────────────────────────────────────────
        Region dividerLine = new Region();
        dividerLine.setPrefHeight(1); dividerLine.setMaxHeight(1);
        dividerLine.setStyle("-fx-background-color:" + DIVIDER + ";");

        // ── Name row (First | Last side-by-side) ──────────────────────────────
        VBox firstBox = new VBox(fieldLabel.apply("FIRST NAME"), firstNameField);
        VBox lastBox  = new VBox(fieldLabel.apply("LAST NAME"),  lastNameField);
        HBox.setHgrow(firstBox, Priority.ALWAYS);
        HBox.setHgrow(lastBox,  Priority.ALWAYS);
        HBox nameRow = new HBox(12, firstBox, lastBox);

        // ── Form content ──────────────────────────────────────────────────────
        VBox formContent = new VBox(4,
                notice,
                dividerLine,
                nameRow,
                fieldLabel.apply("CONTACT NUMBER"),
                contactField,
                fieldLabel.apply("EMAIL ADDRESS"),
                emailField,
                errorLabel
        );
        formContent.setPadding(new Insets(16, 20, 16, 20));
        formContent.setStyle("-fx-background-color:" + CARD + ";");

        // ── Footer ────────────────────────────────────────────────────────────
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setPrefWidth(100); cancelBtn.setPrefHeight(36);
        cancelBtn.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:" + GRAY + ";" +
                        "-fx-font-family:'Inter';-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-background-radius:9;-fx-cursor:hand;" +
                        "-fx-border-color:" + BORDER + ";-fx-border-width:1;-fx-border-radius:9;");
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(
                "-fx-background-color:#2e3349;-fx-text-fill:white;" +
                        "-fx-font-family:'Inter';-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-background-radius:9;-fx-cursor:hand;" +
                        "-fx-border-color:" + BORDER + ";-fx-border-width:1;-fx-border-radius:9;"));
        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:" + GRAY + ";" +
                        "-fx-font-family:'Inter';-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-background-radius:9;-fx-cursor:hand;" +
                        "-fx-border-color:" + BORDER + ";-fx-border-width:1;-fx-border-radius:9;"));
        cancelBtn.setOnAction(e -> popup.close());
        addPressEffect(cancelBtn);

        Button saveBtn = new Button("Save Changes");
        saveBtn.setPrefWidth(130); saveBtn.setPrefHeight(36);
        saveBtn.setStyle(btnStyle(AMBER, "#1c2237"));
        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle(
                "-fx-background-color: #1a2e23;" + // Darker Green background
                        "-fx-text-fill: #4ade80;" +       // Bright Green text (GREEN_FG)
                        "-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 9; -fx-cursor: hand;" +
                        "-fx-border-color: #4ade80; -fx-border-width: 1; -fx-border-radius: 9;"));
        saveBtn.setOnMouseExited(e -> saveBtn.setStyle(btnStyle(AMBER, "#1c2237")));
        addPressEffect(saveBtn);

        saveBtn.setOnAction(e -> {
            String newFirst   = firstNameField.getText().trim();
            String newLast    = lastNameField.getText().trim();
            String newContact = contactField.getText().trim();
            String newEmail   = emailField.getText().trim();

            // ── First name ────────────────────────────────────────────────────────
            if (newFirst.isEmpty()) {
                errorLabel.setText("First name cannot be empty.");
                errorLabel.setVisible(true);
                return;
            }
            if (!newFirst.matches("[a-zA-Z\\s\\-'.]+")) {
                errorLabel.setText("First name must contain letters only.");
                errorLabel.setVisible(true);
                return;
            }

            // ── Last name ─────────────────────────────────────────────────────────
            if (newLast.isEmpty()) {
                errorLabel.setText("Last name cannot be empty.");
                errorLabel.setVisible(true);
                return;
            }
            if (!newLast.matches("[a-zA-Z\\s\\-'.]+")) {
                errorLabel.setText("Last name must contain letters only.");
                errorLabel.setVisible(true);
                return;
            }

            // ── Contact — Philippine number ───────────────────────────────────────
            // Accepts: 09XXXXXXXXX, +639XXXXXXXXX, 639XXXXXXXXX (11 or 13 digits)
            if (!newContact.isEmpty()) {
                String normalised = newContact.replaceAll("[\\s\\-]", "");
                if (!normalised.matches("(09|\\+639|639)\\d{9}")) {
                    errorLabel.setText("Enter a valid PH number: 09XXXXXXXXX or +639XXXXXXXXX.");
                    errorLabel.setVisible(true);
                    return;
                }
            }

            // ── Email ─────────────────────────────────────────────────────────────
            if (!newEmail.isEmpty()) {
                if (!newEmail.matches("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
                    errorLabel.setText("Enter a valid email address (e.g. juan@gmail.com).");
                    errorLabel.setVisible(true);
                    return;
                }
            }

            errorLabel.setVisible(false);

            // ── Confirmation dialog ───────────────────────────────────────────────
            boolean confirmed = showConfirmDialog(
                    popup,
                    r.getFullName(),
                    newFirst + " " + newLast,
                    newContact,
                    newEmail
            );

            if (!confirmed) return;

            // ── Save to DB ────────────────────────────────────────────────────────
            boolean ok = ClientDAO.updateClientInfo(
                    r.getClientID(), newFirst, newLast, newContact, newEmail);

            if (ok) {
                popup.close();
                loadData();
                showSuccessToast(newFirst + " " + newLast);
            } else {
                errorLabel.setText("Failed to save. Please check your connection and try again.");
                errorLabel.setVisible(true);
            }
        });
        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        HBox footer = new HBox(12, cancelBtn, footerSpacer, saveBtn);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(12, 20, 18, 20));
        footer.setStyle("-fx-background-color:" + CARD + ";-fx-background-radius:0 0 16 16;");

        // ── Card ──────────────────────────────────────────────────────────────
        VBox card = new VBox(titleBar, formContent, footer);
        card.setStyle("-fx-background-color:" + CARD + ";" +
                "-fx-background-radius:16;" +
                "-fx-border-color:" + BORDER + ";" +
                "-fx-border-width:1.5;-fx-border-radius:16;" +
                "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.7),30,0,0,8);");
        card.setMaxWidth(440);

        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color:transparent;");
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 480, Region.USE_COMPUTED_SIZE);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.showAndWait();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CONFIRMATION DIALOG
    // ════════════════════════════════════════════════════════════════════════
    /**
     * Shows a styled confirmation popup summarising the changes.
     * Returns true if the user clicks Confirm, false if they cancel.
     */
    private boolean showConfirmDialog(Stage owner,
                                      String oldName,
                                      String newName,
                                      String newContact,
                                      String newEmail) {
        final boolean[] confirmed = {false};

        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setResizable(false);

        // ── Icon + heading ────────────────────────────────────────────────────
        Label icon = new Label("?");
        icon.setPrefSize(48, 48); icon.setMinSize(48, 48);
        icon.setAlignment(Pos.CENTER);
        icon.setStyle("-fx-background-color:" + AMBER_BG + ";-fx-background-radius:24;" +
                "-fx-text-fill:" + AMBER + ";-fx-font-family:'Inter';-fx-font-size:24px;-fx-font-weight:bold;" +
                "-fx-border-color:" + AMBER + ";-fx-border-width:1.5;-fx-border-radius:24;");

        Label heading = new Label("Confirm Changes");
        heading.setStyle("-fx-text-fill:white;-fx-font-family:'Inter';" +
                "-fx-font-size:18px;-fx-font-weight:bold;");
        Label subheading = new Label("Review the details below before saving.");
        subheading.setStyle("-fx-text-fill:" + GRAY + ";-fx-font-family:'Inter';-fx-font-size:13px;");

        VBox headingBlock = new VBox(3, heading, subheading);
        headingBlock.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(14, icon, headingBlock);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 20, 14, 20));
        header.setStyle("-fx-background-color:" + CARD + ";-fx-background-radius:16 16 0 0;");

        // Drag support
        final double[] dd = new double[2];
        header.setOnMousePressed(e  -> { dd[0] = e.getSceneX(); dd[1] = e.getSceneY(); });
        header.setOnMouseDragged(e  -> {
            dialog.setX(e.getScreenX() - dd[0]);
            dialog.setY(e.getScreenY() - dd[1]);
        });

        // ── Change summary ────────────────────────────────────────────────────
        VBox summaryBox = new VBox(6);
        summaryBox.setPadding(new Insets(0, 20, 16, 20));
        summaryBox.setStyle("-fx-background-color:" + CARD + ";");

        // Divider
        Region div = new Region();
        div.setPrefHeight(1); div.setMaxHeight(1);
        div.setStyle("-fx-background-color:" + DIVIDER + ";");
        summaryBox.getChildren().add(div);

        // Change rows
        summaryBox.getChildren().addAll(
                confirmRow("Name",    oldName,                   newName),
                confirmRow("Contact", "—",                       newContact.isEmpty() ? "—" : newContact),
                confirmRow("Email",   "—",                       newEmail.isEmpty()   ? "—" : newEmail)
        );

        // ── Buttons ───────────────────────────────────────────────────────────
        Button cancelBtn2 = new Button("Cancel");
        cancelBtn2.setPrefWidth(100); cancelBtn2.setPrefHeight(36);
        cancelBtn2.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:" + GRAY + ";" +
                        "-fx-font-family:'Inter';-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-background-radius:9;-fx-cursor:hand;" +
                        "-fx-border-color:" + BORDER + ";-fx-border-width:1;-fx-border-radius:9;");
        cancelBtn2.setOnMouseEntered(e -> cancelBtn2.setStyle(
                "-fx-background-color:#2e3349;-fx-text-fill:white;" +
                        "-fx-font-family:'Inter';-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-background-radius:9;-fx-cursor:hand;" +
                        "-fx-border-color:" + BORDER + ";-fx-border-width:1;-fx-border-radius:9;"));
        cancelBtn2.setOnMouseExited(e -> cancelBtn2.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:" + GRAY + ";" +
                        "-fx-font-family:'Inter';-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-background-radius:9;-fx-cursor:hand;" +
                        "-fx-border-color:" + BORDER + ";-fx-border-width:1;-fx-border-radius:9;"));
        cancelBtn2.setOnAction(e -> dialog.close());

        Button confirmBtn = new Button("✔  Confirm");
        confirmBtn.setPrefWidth(120); confirmBtn.setPrefHeight(36);
        confirmBtn.setStyle(btnStyle("#27ae60", WHITE));
        confirmBtn.setOnMouseEntered(e -> confirmBtn.setStyle(btnStyle("#219a52", WHITE)));
        confirmBtn.setOnMouseExited(e  -> confirmBtn.setStyle(btnStyle("#27ae60", WHITE)));
        confirmBtn.setOnAction(e -> { confirmed[0] = true; dialog.close(); });

        Region fSpacer = new Region();
        HBox.setHgrow(fSpacer, Priority.ALWAYS);

        HBox dialogFooter = new HBox(12, cancelBtn2, fSpacer, confirmBtn);
        dialogFooter.setAlignment(Pos.CENTER_LEFT);
        dialogFooter.setPadding(new Insets(10, 20, 18, 20));
        dialogFooter.setStyle("-fx-background-color:" + CARD + ";-fx-background-radius:0 0 16 16;");

        // ── Card ──────────────────────────────────────────────────────────────
        VBox dialogCard = new VBox(header, summaryBox, dialogFooter);
        dialogCard.setStyle("-fx-background-color:" + CARD + ";" +
                "-fx-background-radius:16;" +
                "-fx-border-color:" + BORDER + ";" +
                "-fx-border-width:1.5;-fx-border-radius:16;" +
                "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.8),40,0,0,10);");
        dialogCard.setMaxWidth(380);

        StackPane dialogRoot = new StackPane(dialogCard);
        dialogRoot.setStyle("-fx-background-color:transparent;");
        dialogRoot.setPadding(new Insets(20));

        Scene dialogScene = new Scene(dialogRoot, 420, Region.USE_COMPUTED_SIZE);
        dialogScene.setFill(Color.TRANSPARENT);
        dialog.setScene(dialogScene);
        dialog.showAndWait();

        return confirmed[0];
    }

    /** A single labelled row inside the confirmation summary. */
    private VBox confirmRow(String label, String oldVal, String newVal) {
        Label lbl = new Label(label.toUpperCase());
        lbl.setStyle("-fx-text-fill:" + GRAY + ";-fx-font-family:'Inter';" +
                "-fx-font-size:11px;-fx-font-weight:bold;");

        Label valLabel = new Label(newVal);
        valLabel.setStyle("-fx-text-fill:white;-fx-font-family:'Inter';-fx-font-size:14px;-fx-font-weight:bold;");
        valLabel.setWrapText(true);

        VBox box = new VBox(2, lbl, valLabel);
        box.setPadding(new Insets(8, 12, 8, 12));
        box.setStyle("-fx-background-color:#202840;-fx-background-radius:8;");
        VBox.setMargin(box, new Insets(4, 0, 0, 0));
        return box;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SUCCESS TOAST
    // ════════════════════════════════════════════════════════════════════════
    private void showSuccessToast(String clientName) {
        Stage toast = new Stage();
        toast.initStyle(StageStyle.TRANSPARENT);
        toast.initModality(Modality.NONE);
        toast.setAlwaysOnTop(true);

        Label icon = new Label("✔");
        icon.setStyle("-fx-text-fill:#27ae60;-fx-font-size:18px;-fx-font-weight:bold;");

        Label msg = new Label(clientName + "'s info has been updated.");
        msg.setStyle("-fx-text-fill:white;-fx-font-family:'Inter';-fx-font-size:14px;-fx-font-weight:bold;");

        HBox toastBox = new HBox(10, icon, msg);
        toastBox.setAlignment(Pos.CENTER_LEFT);
        toastBox.setPadding(new Insets(14, 20, 14, 20));
        toastBox.setStyle(
                "-fx-background-color:#1c2237;" +
                        "-fx-background-radius:12;" +
                        "-fx-border-color:#27ae60;" +
                        "-fx-border-width:1.5;-fx-border-radius:12;" +
                        "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.6),20,0,0,4);");

        StackPane toastRoot = new StackPane(toastBox);
        toastRoot.setStyle("-fx-background-color:transparent;");
        toastRoot.setPadding(new Insets(10));

        Scene toastScene = new Scene(toastRoot, Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        toastScene.setFill(Color.TRANSPARENT);
        toast.setScene(toastScene);
        toast.show();

        // Auto-close after 2.5 seconds
        javafx.animation.PauseTransition pause =
                new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2.5));
        pause.setOnFinished(e -> toast.close());
        pause.play();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  FOOTER
    // ────────────────────────────────────────────────────────────────────────
    private void updateFooter() {
        if (filtered == null || masterList == null) return;
        int shown = filtered.size();
        int total = masterList.size();
        filteredCountLabel.setText(shown == total
                ? "Showing all " + total + " client(s)"
                : "Showing " + shown + " of " + total);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  COLUMN FACTORY HELPERS
    // ════════════════════════════════════════════════════════════════════════
    private TableColumn<MemberRow, String> col(String title, String prop, double pref) {
        TableColumn<MemberRow, String> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(pref);
        c.setMinWidth(Math.max(pref * 0.7, 40));
        c.setReorderable(false);
        return c;
    }

    private TableColumn<MemberRow, String> typeCol() {
        TableColumn<MemberRow, String> c = new TableColumn<>("Type");
        c.setCellValueFactory(new PropertyValueFactory<>("clientType"));
        c.setPrefWidth(120); c.setMinWidth(90); c.setReorderable(false);
        c.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                setGraphic(null); setText(null);
                if (empty || val == null) return;
                Label badge = new Label(val);
                badge.getStyleClass().add("Member".equalsIgnoreCase(val) ? "badge-active" : "badge-expired");
                HBox box = new HBox(badge);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        return c;
    }

    private TableColumn<MemberRow, String> periodCol() {
        TableColumn<MemberRow, String> c = new TableColumn<>("Plan Period");
        c.setCellValueFactory(new PropertyValueFactory<>("lastPaymentPeriod"));
        c.setPrefWidth(125); c.setMinWidth(90); c.setReorderable(false);
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

    private TableColumn<MemberRow, String> planStatusCol() {
        TableColumn<MemberRow, String> c = new TableColumn<>("Plan Status");
        c.setCellValueFactory(new PropertyValueFactory<>("planStatus"));
        c.setPrefWidth(115); c.setMinWidth(90); c.setReorderable(false);
        c.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                setGraphic(null); setText(null); setStyle("");
                if (empty || val == null) return;
                if ("No Record".equals(val)) { setText("—"); setStyle("-fx-text-fill:#7a7f94;"); return; }
                Label badge = new Label();
                if ("Active".equals(val)) {
                    badge.setText("Active");
                    badge.getStyleClass().add("badge-active");
                } else {
                    badge.setText("Inactive");
                    badge.getStyleClass().add("badge-expired");
                }
                HBox box = new HBox(badge);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        return c;
    }

    private TableColumn<MemberRow, String> membershipStatusCol() {
        TableColumn<MemberRow, String> c = new TableColumn<>("Mem. Status");
        c.setCellValueFactory(new PropertyValueFactory<>("membershipStatus"));
        c.setPrefWidth(115); c.setMinWidth(90); c.setReorderable(false);
        c.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                setGraphic(null); setText(null); setStyle("");
                if (empty || val == null) return;
                if ("None".equals(val)) { setText("—"); setStyle("-fx-text-fill:#7a7f94;"); return; }
                Label badge = new Label(val);
                badge.getStyleClass().add("Active".equals(val) ? "badge-active" : "badge-expired");
                HBox box = new HBox(badge);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        return c;
    }

    private TableColumn<MemberRow, Double> amountCol() {
        TableColumn<MemberRow, Double> c = new TableColumn<>("Last Paid");
        c.setCellValueFactory(new PropertyValueFactory<>("lastPaymentAmount"));
        c.setPrefWidth(115); c.setMinWidth(90); c.setReorderable(false);
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

    private void addPressEffect(Button button) {
        button.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), button);
            st.setToX(0.95);
            st.setToY(0.95);
            st.play();
        });
        button.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }
}