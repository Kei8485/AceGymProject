package codes.acegym.Controllers;

import codes.acegym.DB.ReceiptDAO;
import codes.acegym.Objects.Receipt;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class ReportPageController implements Refreshable {

    @Override
    public void refreshData() {
        loadData(null, null);
        updateFooterStats();
        System.out.println("Report Page Refreshed from Database.");
    }

    // ── Table ──
    @FXML private TableView<Receipt>            receiptTable;
    @FXML private TableColumn<Receipt, Integer> colClientID;      // repurposed as row #
    @FXML private TableColumn<Receipt, String>  colFirstName;
    @FXML private TableColumn<Receipt, String>  colLastName;
    @FXML private TableColumn<Receipt, String>  colDate;
    @FXML private TableColumn<Receipt, String>  colPaymentMethod;
    @FXML private TableColumn<Receipt, Double>  colAmount;
    @FXML private TableColumn<Receipt, String>  colStatus;

    // ── Date fields ──
    @FXML private TextField  fromDateField;
    @FXML private TextField  toDateField;
    @FXML private Label      resetDate;
    @FXML private ImageView  fromDateIcon;
    @FXML private ImageView  toDateIcon;

    // ── Filter + search ──
    @FXML private ComboBox<String> filterCombo;
    @FXML private TextField        searchField;

    // ── Footer stat labels ──
    @FXML private Label totalSalesLabel;
    @FXML private Label transactionsLabel;
    @FXML private Label cashSalesLabel;
    @FXML private Label digitalSalesLabel;

    // ── Generate button ──
    @FXML private Button generateBtn;

    // ── Internal state ──
    private ObservableList<Receipt> masterList   = FXCollections.observableArrayList();
    private FilteredList<Receipt>   filteredList;
    private LocalDate               fromDate;
    private LocalDate               toDate;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter DB_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Alert type constants
    private static final String TYPE_WARNING = "WARNING";
    private static final String TYPE_ERROR   = "ERROR";
    private static final String TYPE_INFO    = "INFO";

    // ═══════════════════════════════════════════════════════════════
    // INIT
    // ═══════════════════════════════════════════════════════════════

    @FXML
    private void initialize() {
        setupTableColumns();
        setupTableRowClick();
        setupFilterCombo();
        setupDateFields();
        setupSearch();
        loadData(null, null);
        updateFooterStats();
    }

    @FXML
    private void handleResetDate(MouseEvent event) {
        fromDateField.clear();
        toDateField.clear();
        fromDate = null;
        toDate   = null;
        loadData(null, null);
    }

    // ═══════════════════════════════════════════════════════════════
    // TABLE SETUP
    // ═══════════════════════════════════════════════════════════════

    private void setupTableColumns() {

        // ── Row counter — replaces Client ID column ──
        colClientID.setText("#");
        colClientID.setSortable(false);
        colClientID.setResizable(false);
        colClientID.setPrefWidth(50);
        colClientID.setMaxWidth(50);
        colClientID.setStyle("-fx-alignment: CENTER;");
        colClientID.setCellValueFactory(p -> new SimpleIntegerProperty(0).asObject());
        colClientID.setCellFactory(col -> new TableCell<Receipt, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                setText(empty ? null : String.valueOf(getIndex() + 1));
            }
        });

        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));

        colDate.setCellValueFactory(cell -> {
            String raw = cell.getValue().getDate();
            if (raw == null || raw.isBlank()) return new SimpleStringProperty("—");
            try {
                String datePart = raw.length() >= 10 ? raw.substring(0, 10) : raw;
                LocalDate ld = LocalDate.parse(datePart, DB_DATE_FMT);
                return new SimpleStringProperty(ld.format(DISPLAY_FMT));
            } catch (DateTimeParseException e) {
                return new SimpleStringProperty(raw);
            }
        });

        colPaymentMethod.setCellValueFactory(new PropertyValueFactory<>("paymentType"));

        colAmount.setCellValueFactory(new PropertyValueFactory<>("total"));
        colAmount.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : String.format("₱%,.2f", value));
            }
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) { setText(null); setStyle(""); }
                else {
                    setText(value);
                    setStyle("Paid".equalsIgnoreCase(value)
                            ? "-fx-text-fill: #4caf50; -fx-font-weight: bold;"
                            : "-fx-text-fill: #e53935; -fx-font-weight: bold;");
                }
            }
        });

        receiptTable.setPlaceholder(new Label("No records found."));
    }

    // ═══════════════════════════════════════════════════════════════
    // ROW-CLICK → PAYMENT DETAILS POPUP
    // ═══════════════════════════════════════════════════════════════

    private void setupTableRowClick() {
        receiptTable.setOnMouseClicked(event -> {
            Receipt selected = receiptTable.getSelectionModel().getSelectedItem();
            if (selected != null) showPaymentDetailsPopup(selected);
        });
    }

    private void showPaymentDetailsPopup(Receipt r) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initOwner(receiptTable.getScene().getWindow());

        VBox root = new VBox(0);
        root.setStyle(
                "-fx-background-color: #1c2237;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #2e3349;" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.85), 40, 0, 0, 0);"
        );
        root.setPrefWidth(700);
        root.setMinWidth(640);

        // ── Header ──────────────────────────────────────────────
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setStyle("-fx-border-color: transparent transparent #2e3349 transparent; -fx-border-width: 0 0 1.5 0;");

        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setMinSize(40, 40);
        iconBox.setMaxSize(40, 40);
        iconBox.setStyle("-fx-background-color: #e53935; -fx-background-radius: 10;");
        try {
            ImageView icon = new ImageView(
                    new Image(getClass().getResourceAsStream("/codes/acegym/image/tag-solid.png"))
            );
            icon.setFitWidth(20); icon.setFitHeight(20);
            icon.setPreserveRatio(true);
            iconBox.getChildren().add(icon);
        } catch (Exception ex) {
            Label fb = new Label("R");
            fb.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-family: 'Inter'; -fx-font-size: 22px;");
            iconBox.getChildren().add(fb);
        }

        VBox titleBlock = new VBox(2);
        Label titleLbl = new Label("Payment Details");
        titleLbl.setStyle("-fx-text-fill: #ffffff; -fx-font-family: 'Inter'; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label receiptBadge = new Label(String.format("RCP-%04d", r.getReceiptID()));
        receiptBadge.setStyle(
                "-fx-text-fill: #7a7f94; -fx-font-family: 'Inter'; -fx-font-size: 13px;"
        );
        titleBlock.getChildren().addAll(titleLbl, receiptBadge);
        header.getChildren().addAll(iconBox, titleBlock);

        // ── Row 1: Client info (left) + Transaction info (right) ──
        HBox row1 = new HBox();
        row1.setPadding(new Insets(24, 32, 20, 32));

        // Resolve display date
        String rawDate = r.getDate();
        String displayDate = "—";
        if (rawDate != null && rawDate.length() >= 10) {
            try {
                LocalDate ld = LocalDate.parse(rawDate.substring(0, 10), DB_DATE_FMT);
                displayDate = ld.format(DISPLAY_FMT);
            } catch (DateTimeParseException ignored) {
                displayDate = rawDate;
            }
        }

        // Left: who paid
        GridPane leftGrid = buildDetailGrid();
        addDetailRow(leftGrid, 0, "Client ID",  String.format("CLID%04d", r.getClientID()));
        addDetailRow(leftGrid, 1, "First Name", r.getFirstName());
        addDetailRow(leftGrid, 2, "Last Name",  r.getLastName());

        Region div1 = makeDivider();

        // Right: when and how
        GridPane rightGrid = buildDetailGrid();
        addDetailRow(rightGrid, 0, "Payment Date",   displayDate);
        addDetailRow(rightGrid, 1, "Payment Method", r.getPaymentType());
        addDetailRow(rightGrid, 2, "Period",
                r.getPaymentPeriod() != null ? r.getPaymentPeriod() : "—");

        HBox.setHgrow(leftGrid,  Priority.ALWAYS);
        HBox.setHgrow(rightGrid, Priority.ALWAYS);
        row1.getChildren().addAll(leftGrid, div1, rightGrid);

        // ── Separator ───────────────────────────────────────────
        Region hSep = new Region();
        hSep.setPrefHeight(1.5);
        hSep.setStyle("-fx-background-color: #2e3349;");

        // ── Purchase Details section ─────────────────────────────
        VBox purchaseSection = buildPurchaseDetails(r);

        // ── Footer ──────────────────────────────────────────────
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(14, 24, 20, 24));
        footer.setStyle("-fx-border-color: #2e3349 transparent transparent transparent; -fx-border-width: 1.5 0 0 0;");

        Button closeBtn = buildRedButton("✕   Close");
        closeBtn.setOnAction(e -> popup.close());
        footer.getChildren().add(closeBtn);

        root.getChildren().addAll(header, row1, hSep, purchaseSection, footer);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.setOnShown(e -> centerOnOwner(popup));
        popup.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────
    // PURCHASE DETAILS SECTION — what the client actually paid for
    // ─────────────────────────────────────────────────────────────

    private VBox buildPurchaseDetails(Receipt r) {
        VBox box = new VBox(14);
        box.setPadding(new Insets(22, 32, 26, 32));
        box.setStyle("-fx-background-color: #161b2e;");

        // Section header
        Label sectionTitle = new Label("PURCHASE DETAILS");
        sectionTitle.setStyle(
                "-fx-text-fill: #4a5068;" +
                        "-fx-font-family: 'Inter';" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;"
        );

        // Training type row — red badge
        String training = (r.getTrainingCategory() != null && !r.getTrainingCategory().isBlank())
                ? r.getTrainingCategory() : "—";
        HBox trainingRow = buildPurchaseRow("Training Type", training, "#e53935");

        // Coach row — plain, with "None None" guard
        String coach = resolveCoachDisplay(r.getCoachName());
        HBox coachRow = buildPurchaseRow("Coach", coach, null);

        // Membership row — green badge for Member, muted for Non Member
        String membership = (r.getMembershipType() != null && !r.getMembershipType().isBlank())
                ? r.getMembershipType() : "Non Member";
        String memberBadge = membership.equalsIgnoreCase("Member") ? "#4caf50" : "#4a5068";
        HBox membershipRow = buildPurchaseRow("Membership", membership, memberBadge);

        // ── Total amount — prominent, bottom-right ───────────────
        HBox totalContainer = new HBox();
        totalContainer.setAlignment(Pos.CENTER_RIGHT);
        totalContainer.setPadding(new Insets(10, 0, 0, 0));
        totalContainer.setStyle(
                "-fx-border-color: #2e3349 transparent transparent transparent;" +
                        "-fx-border-width: 1.5 0 0 0;"
        );

        VBox totalBlock = new VBox(2);
        totalBlock.setAlignment(Pos.CENTER_RIGHT);
        Label totalCaption = new Label("TOTAL PAID");
        totalCaption.setStyle(
                "-fx-text-fill: #4a5068; -fx-font-family: 'Inter'; -fx-font-size: 11px; -fx-font-weight: bold;"
        );
        Label totalAmount = new Label(String.format("₱%,.2f", r.getTotal()));
        totalAmount.setStyle(
                "-fx-text-fill: #e53935; -fx-font-family: 'Inter'; -fx-font-size: 32px; -fx-font-weight: bold;"
        );
        totalBlock.getChildren().addAll(totalCaption, totalAmount);
        totalContainer.getChildren().add(totalBlock);

        box.getChildren().addAll(
                sectionTitle, trainingRow, coachRow, membershipRow, totalContainer
        );
        return box;
    }

    /**
     * Builds one row in the Purchase Details section.
     * If badgeColor is non-null, the value is rendered as a colored pill/badge.
     * If null, the value is rendered as plain white text.
     */
    private HBox buildPurchaseRow(String labelText, String valueText, String badgeColor) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(labelText);
        lbl.setMinWidth(140);
        lbl.setStyle("-fx-text-fill: #7a7f94; -fx-font-family: 'Inter'; -fx-font-size: 15px;");

        String display = (valueText == null || valueText.isBlank()) ? "—" : valueText;

        if (badgeColor != null) {
            // Derive rgba from hex for a subtle tinted background
            String rgba = hexToRgba(badgeColor, 0.15);
            Label badge = new Label(display);
            badge.setStyle(
                    "-fx-background-color: " + rgba + ";" +
                            "-fx-text-fill: " + badgeColor + ";" +
                            "-fx-font-family: 'Inter';" +
                            "-fx-font-size: 14px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 6;" +
                            "-fx-padding: 4 12 4 12;"
            );
            row.getChildren().addAll(lbl, badge);
        } else {
            Label val = new Label(display);
            val.setStyle(
                    "-fx-text-fill: #ffffff; -fx-font-family: 'Inter'; -fx-font-size: 15px; -fx-font-weight: 600;"
            );
            row.getChildren().addAll(lbl, val);
        }
        return row;
    }

    /**
     * Guards against the "None None" placeholder staff entry (StaffID = 1).
     * Returns "No coach assigned" whenever the name is blank, null, or "None None".
     */
    private String resolveCoachDisplay(String coachName) {
        if (coachName == null || coachName.isBlank()) return "No coach assigned";
        if (coachName.equalsIgnoreCase("None None") || coachName.equalsIgnoreCase("none none"))
            return "No coach assigned";
        return coachName;
    }

    /**
     * Converts a 6-char hex color (e.g. "#e53935") to an rgba() string with the given alpha.
     * Falls back to a safe transparent black on any parse error.
     */
    private String hexToRgba(String hex, double alpha) {
        try {
            String clean = hex.replace("#", "");
            int r = Integer.parseInt(clean.substring(0, 2), 16);
            int g = Integer.parseInt(clean.substring(2, 4), 16);
            int b = Integer.parseInt(clean.substring(4, 6), 16);
            return String.format("rgba(%d,%d,%d,%.2f)", r, g, b, alpha);
        } catch (Exception e) {
            return "rgba(0,0,0,0.15)";
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SHARED POPUP HELPERS
    // ═══════════════════════════════════════════════════════════════

    private Region makeDivider() {
        Region divider = new Region();
        divider.setPrefWidth(1.5);
        divider.setMinHeight(100);
        divider.setStyle("-fx-background-color: #2e3349;");
        HBox.setMargin(divider, new Insets(4, 32, 4, 32));
        return divider;
    }

    private GridPane buildDetailGrid() {
        GridPane grid = new GridPane();
        grid.setVgap(16);
        grid.setHgap(14);
        ColumnConstraints labelCol = new ColumnConstraints(130);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, valueCol);
        return grid;
    }

    private void addDetailRow(GridPane grid, int row, String labelText, String valueText) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill: #7a7f94; -fx-font-family: 'Inter'; -fx-font-size: 15px;");

        Label val = new Label(valueText != null && !valueText.isBlank() ? valueText : "—");
        val.setStyle(
                "-fx-text-fill: #ffffff; -fx-font-family: 'Inter'; -fx-font-size: 15px; -fx-font-weight: 600;"
        );

        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
    }

    // ═══════════════════════════════════════════════════════════════
    // FILTER COMBO
    // ═══════════════════════════════════════════════════════════════

    private void setupFilterCombo() {
        ObservableList<String> types = ReceiptDAO.getPaymentTypes();
        filterCombo.setItems(types);
        filterCombo.getSelectionModel().selectFirst();
        filterCombo.setOnAction(e -> applyFilters());
    }

    // ═══════════════════════════════════════════════════════════════
    // DATE FIELDS
    // ═══════════════════════════════════════════════════════════════

    private void setupDateFields() {
        fromDateField.setPromptText("MM/DD/YYYY");
        toDateField.setPromptText("MM/DD/YYYY");
        fromDateField.setEditable(false);
        toDateField.setEditable(false);

        fromDateField.setOnMouseClicked(e -> openDatePicker(true));
        toDateField.setOnMouseClicked(e   -> openDatePicker(false));
        fromDateIcon.setOnMouseClicked(e  -> openDatePicker(true));
        toDateIcon.setOnMouseClicked(e    -> openDatePicker(false));

        fromDateIcon.setStyle("-fx-cursor: hand;");
        toDateIcon.setStyle("-fx-cursor: hand;");
    }

    private void openDatePicker(boolean isFrom) {
        DatePicker dp = new DatePicker(isFrom ? fromDate : toDate);
        dp.setShowWeekNumbers(false);
        dp.setPrefWidth(200);

        try {
            String css = getClass().getResource("/Css/ReportPage.css").toExternalForm();
            dp.getStylesheets().add(css);
        } catch (NullPointerException e) {
            System.out.println("CSS File not found! Check your folder naming.");
        }

        ContextMenu cm = new ContextMenu();
        cm.getStyleClass().add("date-picker-context-menu");
        CustomMenuItem item = new CustomMenuItem(dp, false);
        item.setHideOnClick(false);
        cm.getItems().add(item);

        TextField field = isFrom ? fromDateField : toDateField;
        cm.show(field, javafx.geometry.Side.BOTTOM, 0, 4);

        dp.setOnAction(ev -> {
            LocalDate chosen = dp.getValue();
            if (isFrom) {
                fromDate = chosen;
                fromDateField.setText(chosen != null ? chosen.format(DISPLAY_FMT) : "");
            } else {
                toDate = chosen;
                toDateField.setText(chosen != null ? chosen.format(DISPLAY_FMT) : "");
            }
            cm.hide();
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // SEARCH
    // ═══════════════════════════════════════════════════════════════

    private void setupSearch() {
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
    }

    // ═══════════════════════════════════════════════════════════════
    // GENERATE REPORT — full validation
    // ═══════════════════════════════════════════════════════════════

    @FXML
    private void handleGenerateReport() {

        if (fromDate != null && toDate == null) {
            showStyledAlert(TYPE_WARNING,
                    "Incomplete Date Range",
                    "You set a 'From Date' but left 'To Date' empty.\nPlease fill in both dates or clear the range.");
            return;
        }
        if (toDate != null && fromDate == null) {
            showStyledAlert(TYPE_WARNING,
                    "Incomplete Date Range",
                    "You set a 'To Date' but left 'From Date' empty.\nPlease fill in both dates or clear the range.");
            return;
        }

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            showStyledAlert(TYPE_WARNING,
                    "Invalid Date Range",
                    "'From Date' cannot be later than 'To Date'.\nPlease correct the dates and try again.");
            return;
        }

        if (fromDate != null && toDate != null && fromDate.plusYears(1).isBefore(toDate)) {
            showStyledAlert(TYPE_INFO,
                    "Large Date Range",
                    "Your selected range spans over a year.\nThis may return a large number of records.");
        }

        if (toDate != null && toDate.isAfter(LocalDate.now())) {
            showStyledAlert(TYPE_INFO,
                    "Future Date Selected",
                    "'To Date' is set to a future date.\nOnly existing records up to today will be shown.");
        }

        loadData(fromDate, toDate);

        if (receiptTable.getItems() == null || receiptTable.getItems().isEmpty()) {
            showStyledAlert(TYPE_INFO,
                    "No Records Found",
                    "No transactions matched your selected filters.\nTry adjusting the date range or payment type.");
        }

        updateFooterStats();
    }

    // ═══════════════════════════════════════════════════════════════
    // DATA LOADING
    // ═══════════════════════════════════════════════════════════════

    private void loadData(LocalDate from, LocalDate to) {
        String selectedType = filterCombo.getValue();
        boolean hasType = selectedType != null && !selectedType.equals("All");

        if (from == null && to == null && !hasType) {
            masterList.setAll(ReceiptDAO.getAllReceipts());
        } else {
            masterList.setAll(ReceiptDAO.getByDateRangeAndType(
                    from, to, hasType ? selectedType : null));
        }

        filteredList = new FilteredList<>(masterList, r -> true);
        SortedList<Receipt> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(receiptTable.comparatorProperty());
        receiptTable.setItems(sortedList);

        applyFilters();
        updateFooterStats();
    }

    // ═══════════════════════════════════════════════════════════════
    // FILTERING
    // ═══════════════════════════════════════════════════════════════

    private void applyFilters() {
        if (filteredList == null) return;

        String search     = searchField.getText() == null ? "" :
                searchField.getText().toLowerCase(Locale.ROOT).trim();
        String typeFilter = filterCombo.getValue();
        boolean filterAll = typeFilter == null || typeFilter.equals("All");

        filteredList.setPredicate(r -> {
            if (!filterAll && !r.getPaymentType().equalsIgnoreCase(typeFilter)) return false;
            if (!search.isEmpty()) {
                boolean match =
                        String.valueOf(r.getClientID()).contains(search)              ||
                                r.getFirstName().toLowerCase(Locale.ROOT).contains(search)   ||
                                r.getLastName().toLowerCase(Locale.ROOT).contains(search)    ||
                                r.getDate().toLowerCase(Locale.ROOT).contains(search)        ||
                                r.getPaymentType().toLowerCase(Locale.ROOT).contains(search) ||
                                String.format("%.2f", r.getTotal()).contains(search);
                if (!match) return false;
            }
            return true;
        });

        updateFooterStats();
    }

    // ═══════════════════════════════════════════════════════════════
    // FOOTER STATS
    // ═══════════════════════════════════════════════════════════════

    private void updateFooterStats() {
        ObservableList<Receipt> visible = receiptTable.getItems();

        if (visible == null || visible.isEmpty()) {
            totalSalesLabel.setText("₱0.00");
            transactionsLabel.setText("0");
            cashSalesLabel.setText("0");
            digitalSalesLabel.setText("0");
            return;
        }

        double totalSales   = 0;
        int    cashCount    = 0;
        int    digitalCount = 0;

        for (Receipt r : visible) {
            totalSales += r.getTotal();
            String type = r.getPaymentType() == null ? "" :
                    r.getPaymentType().toLowerCase(Locale.ROOT);
            if (type.contains("cash")) cashCount++;
            else digitalCount++;
        }

        totalSalesLabel.setText(String.format("₱%,.2f", totalSales));
        transactionsLabel.setText(String.valueOf(visible.size()));
        cashSalesLabel.setText(String.valueOf(cashCount));
        digitalSalesLabel.setText(String.valueOf(digitalCount));
    }

    // ═══════════════════════════════════════════════════════════════
    // STYLED ALERT
    // ═══════════════════════════════════════════════════════════════

    private void showStyledAlert(String alertType, String title, String message) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initOwner(receiptTable.getScene().getWindow());

        String accentColor;
        String iconSymbol;
        switch (alertType) {
            case TYPE_ERROR   -> { accentColor = "#e53935"; iconSymbol = "✕"; }
            case TYPE_INFO    -> { accentColor = "#e53935"; iconSymbol = "i"; }
            default           -> { accentColor = "#e53935"; iconSymbol = "⚠"; }
        }

        VBox root = new VBox(0);
        root.setStyle(
                "-fx-background-color: #1c2237;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #2e3349;" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.85), 40, 0, 0, 0);"
        );
        root.setPrefWidth(420);
        root.setMinWidth(380);

        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 22, 18, 22));
        header.setStyle("-fx-border-color: transparent transparent #2e3349 transparent; -fx-border-width: 0 0 1.5 0;");

        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setMinSize(38, 38);
        iconBox.setMaxSize(38, 38);
        iconBox.setStyle("-fx-background-color: " + accentColor + "; -fx-background-radius: 10;");
        Label iconLbl = new Label(iconSymbol);
        iconLbl.setStyle(
                "-fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: 'Inter';"
        );
        iconBox.getChildren().add(iconLbl);

        Label titleLbl = new Label(title);
        titleLbl.setStyle(
                "-fx-text-fill: #ffffff; -fx-font-family: 'Inter'; -fx-font-size: 20px; -fx-font-weight: bold;"
        );
        header.getChildren().addAll(iconBox, titleLbl);

        Label msgLbl = new Label(message);
        msgLbl.setWrapText(true);
        msgLbl.setStyle(
                "-fx-text-fill: #7a7f94; -fx-font-family: 'Inter'; -fx-font-size: 15px;"
        );
        msgLbl.setPadding(new Insets(20, 24, 20, 24));
        msgLbl.setMaxWidth(380);

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 22, 18, 22));
        footer.setStyle(
                "-fx-border-color: #2e3349 transparent transparent transparent; -fx-border-width: 1.5 0 0 0;"
        );

        Button okBtn = buildAccentButton("OK", accentColor);
        okBtn.setOnAction(e -> popup.close());
        footer.getChildren().add(okBtn);

        root.getChildren().addAll(header, msgLbl, footer);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.setOnShown(e -> centerOnOwner(popup));
        popup.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════════
    // SHARED UI HELPERS
    // ═══════════════════════════════════════════════════════════════

    private Button buildRedButton(String text) {
        return buildAccentButton(text, "#e53935");
    }

    private Button buildAccentButton(String text, String color) {
        String hoverColor = darken(color);

        String baseStyle =
                "-fx-background-color: " + color + ";" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: 'Inter';" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 9;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 9 22 9 22;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 8, 0, 0, 2);";

        String hoverStyle =
                "-fx-background-color: " + hoverColor + ";" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: 'Inter';" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 9;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 9 22 9 22;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 4, 0, 0, 1);";

        Button btn = new Button(text);
        btn.setStyle(baseStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e  -> btn.setStyle(baseStyle));
        return btn;
    }

    private String darken(String hex) {
        return switch (hex.toLowerCase()) {
            case "#e53935" -> "#c62828";
            case "#ff9800" -> "#e65100";
            case "#2196f3" -> "#1565c0";
            default        -> hex;
        };
    }

    private void centerOnOwner(Stage popup) {
        Stage owner = (Stage) receiptTable.getScene().getWindow();
        popup.setX(owner.getX() + (owner.getWidth()  - popup.getWidth())  / 2);
        popup.setY(owner.getY() + (owner.getHeight() - popup.getHeight()) / 2);
    }
}