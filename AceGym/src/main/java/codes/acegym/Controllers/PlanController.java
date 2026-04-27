package codes.acegym.Controllers;

import codes.acegym.DB.PlanDAO;
import codes.acegym.Objects.ClientType;
import codes.acegym.Objects.PaymentPeriod;
import codes.acegym.Objects.TrainingCategory;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;


public class PlanController {

    // ══════════════════════════════════════════════════════════════
    // SECTION 1 — PAYMENT PERIOD
    // ══════════════════════════════════════════════════════════════
    @FXML private Button    addPlanBtn;
    @FXML private Button    cancelPlanBtn;
    @FXML private TextField planNameField;
    @FXML private TextField planPeriodField;      // user types months; we store months×30 as days
    @FXML private TextField planSearchField;

    @FXML private TableView<PaymentPeriod>        planTable;
    @FXML private TableColumn<PaymentPeriod, String>  planNameCol;
    @FXML private TableColumn<PaymentPeriod, Integer> planPeriodCol;   // shows months in UI
    @FXML private TableColumn<PaymentPeriod, Void>    planActionsCol;

    // Live-search backing lists for Section 1
    private ObservableList<PaymentPeriod> planMasterList;
    private FilteredList<PaymentPeriod>   planFilteredList;

    // Tracks which row is being edited (null = add mode)
    private PaymentPeriod selectedPeriod = null;

    // ══════════════════════════════════════════════════════════════
    // SECTION 2 — TRAINING CATEGORY
    // ══════════════════════════════════════════════════════════════
    @FXML private Button    addCategoryBtn;
    @FXML private Button    cancelCategoryBtn;
    @FXML private TextField categoryNameField;
    @FXML private TextField categoryAmountField;
    @FXML private TextField categoryCoachingFeeField;   // NEW field wired in FXML
    @FXML private TextField categorySearchField;

    @FXML private TableView<TrainingCategory>              categoryTable;
    @FXML private TableColumn<TrainingCategory, String>    categoryNameCol;
    @FXML private TableColumn<TrainingCategory, Double>    categoryAmountCol;
    @FXML private TableColumn<TrainingCategory, Double>    categoryCoachingFeeCol; // NEW col
    @FXML private TableColumn<TrainingCategory, Void>      categoryActionsCol;

    // Live-search backing lists for Section 2
    private ObservableList<TrainingCategory> categoryMasterList;
    private FilteredList<TrainingCategory>   categoryFilteredList;

    private TrainingCategory selectedCategory = null;

    // ══════════════════════════════════════════════════════════════
    // SECTION 3 — CLIENT DISCOUNT
    // ══════════════════════════════════════════════════════════════
    @FXML private Button         saveDiscountBtn;
    @FXML private Button         cancelDiscountBtn;
    @FXML private ComboBox<ClientType> clientTypeCombo;
    @FXML private TextField      feeField;
    @FXML private TextField      discountField;

    @FXML private TableView<ClientType>             discountTable;
    @FXML private TableColumn<ClientType, String>   clientTypeCol;
    @FXML private TableColumn<ClientType, Double>   feeCol;
    @FXML private TableColumn<ClientType, String>   discountCol;
    // discountActionsCol removed per design decision

    private ObservableList<ClientType> clientTypeList;
    private ClientType selectedClientType = null;

    // ══════════════════════════════════════════════════════════════
    // INITIALIZE
    // ══════════════════════════════════════════════════════════════
    public void initialize() {
        setupSection1();
        setupSection2();
        setupSection3();
    }

    // ──────────────────────────────────────────────────────────────
    // SECTION 1 — PAYMENT PERIOD SETUP
    // ──────────────────────────────────────────────────────────────
    private void setupSection1() {

        // Column bindings — show months in UI (days ÷ 30)
        planNameCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getName()));
        planPeriodCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        data.getValue().getMonths()));

        // Load from DB
        planMasterList   = PlanDAO.getAllPaymentPeriods();
        planFilteredList = new FilteredList<>(planMasterList, p -> true);
        SortedList<PaymentPeriod> sorted = new SortedList<>(planFilteredList);
        sorted.comparatorProperty().bind(planTable.comparatorProperty());
        planTable.setItems(sorted);

        // Actions column
        setupPlanActionsColumn();

        // Cancel btn hidden by default
        hideCancelBtn(cancelPlanBtn);

        // ── Live search ──
        planSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String kw = newVal == null ? "" : newVal.trim().toLowerCase();
            planFilteredList.setPredicate(p ->
                    kw.isEmpty() ||
                            p.getName().toLowerCase().contains(kw) ||
                            String.valueOf(p.getMonths()).contains(kw)
            );
        });

        // ── Add / Update button ──
        addPlanBtn.setOnAction(e -> {
            String name       = planNameField.getText().trim();
            String monthsText = planPeriodField.getText().trim();

            // Validation
            if (name.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Plan name cannot be empty.");
                return;
            }
            if (monthsText.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Period (months) cannot be empty.");
                return;
            }
            int months;
            try {
                months = Integer.parseInt(monthsText);
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Period must be a whole number (e.g. 1, 3, 12).");
                return;
            }
            if (months <= 0) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Period must be greater than 0.");
                return;
            }

            int days = months * 30;   // convert months → days for DB

            if (selectedPeriod == null) {
                // ADD mode
                showModal("Confirm adding this payment period?", () -> {
                    String result = PlanDAO.addPaymentPeriod(name, days);
                    if ("OK".equals(result)) {
                        refreshPlanTable();
                        resetPlanForm();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error", result);
                    }
                });
            } else {
                // UPDATE mode
                showModal("Confirm updating this payment period?", () -> {
                    String result = PlanDAO.updatePaymentPeriod(selectedPeriod.getId(), name, days);
                    if ("OK".equals(result)) {
                        refreshPlanTable();
                        resetPlanForm();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error", result);
                    }
                });
            }
        });

        // ── Cancel ──
        cancelPlanBtn.setOnAction(e -> resetPlanForm());

        // ── Row click → edit mode ──
        planTable.setOnMouseClicked(e -> {
            PaymentPeriod sel = planTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                selectedPeriod = sel;
                planNameField.setText(sel.getName());
                planPeriodField.setText(String.valueOf(sel.getMonths()));
                addPlanBtn.setText("Update Plan");
                showCancelBtn(cancelPlanBtn);
            }
        });
    }

    private void setupPlanActionsColumn() {
        planActionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("Remove");
            {
                deleteBtn.getStyleClass().add("btn-delete");
                deleteBtn.setOnAction(e -> {
                    PaymentPeriod period = getTableView().getItems().get(getIndex());
                    showModal("Confirm removing \"" + period.getName() + "\"?", () -> {
                        String result = PlanDAO.deletePaymentPeriod(period.getId());
                        if ("OK".equals(result)) {
                            refreshPlanTable();
                            if (period.equals(selectedPeriod)) resetPlanForm();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Cannot Delete", result);
                        }
                    });
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
    }

    private void refreshPlanTable() {
        planMasterList.setAll(PlanDAO.getAllPaymentPeriods());
    }

    private void resetPlanForm() {
        selectedPeriod = null;
        planNameField.clear();
        planPeriodField.clear();
        addPlanBtn.setText("+ Add Plan");
        hideCancelBtn(cancelPlanBtn);
        planTable.getSelectionModel().clearSelection();
    }

    // ──────────────────────────────────────────────────────────────
    // SECTION 2 — TRAINING CATEGORY SETUP
    // ──────────────────────────────────────────────────────────────
    private void setupSection2() {

        // Column bindings
        categoryNameCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getCategoryName()));
        categoryAmountCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        data.getValue().getPrice()));
        categoryCoachingFeeCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        data.getValue().getCoachingFee()));

        // Format amount columns as currency
        formatCurrencyColumn(categoryAmountCol);
        formatCurrencyColumn(categoryCoachingFeeCol);

        // Load from DB
        categoryMasterList   = PlanDAO.getAllTrainingCategories();
        categoryFilteredList = new FilteredList<>(categoryMasterList, p -> true);
        SortedList<TrainingCategory> sorted = new SortedList<>(categoryFilteredList);
        sorted.comparatorProperty().bind(categoryTable.comparatorProperty());
        categoryTable.setItems(sorted);

        // Actions column
        setupCategoryActionsColumn();

        // Cancel hidden by default
        hideCancelBtn(cancelCategoryBtn);

        // ── Live search ──
        categorySearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String kw = newVal == null ? "" : newVal.trim().toLowerCase();
            categoryFilteredList.setPredicate(c ->
                    kw.isEmpty() ||
                            c.getCategoryName().toLowerCase().contains(kw)
            );
        });

        // ── Add / Update button ──
        addCategoryBtn.setOnAction(e -> {
            String name          = categoryNameField.getText().trim();
            String amountText    = categoryAmountField.getText().trim();
            String coachingText  = categoryCoachingFeeField.getText().trim();

            // Validation
            if (name.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Category name cannot be empty.");
                return;
            }
            if (amountText.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Amount cannot be empty.");
                return;
            }
            if (coachingText.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Coaching fee cannot be empty.");
                return;
            }

            double price, coachingFee;
            try {
                price = Double.parseDouble(amountText);
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Amount must be a valid number (e.g. 100.00).");
                return;
            }
            try {
                coachingFee = Double.parseDouble(coachingText);
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Coaching fee must be a valid number (e.g. 500.00).");
                return;
            }
            if (price < 0) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Amount cannot be negative.");
                return;
            }
            if (coachingFee < 0) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Coaching fee cannot be negative.");
                return;
            }

            if (selectedCategory == null) {
                showModal("Confirm adding this category?", () -> {
                    String result = PlanDAO.addTrainingCategory(name, price, coachingFee);
                    if ("OK".equals(result)) {
                        refreshCategoryTable();
                        resetCategoryForm();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error", result);
                    }
                });
            } else {
                showModal("Confirm updating this category?", () -> {
                    String result = PlanDAO.updateTrainingCategory(
                            selectedCategory.getId(), name, price, coachingFee);
                    if ("OK".equals(result)) {
                        refreshCategoryTable();
                        resetCategoryForm();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error", result);
                    }
                });
            }
        });

        // ── Cancel ──
        cancelCategoryBtn.setOnAction(e -> resetCategoryForm());

        // ── Row click → edit mode ──
        categoryTable.setOnMouseClicked(e -> {
            TrainingCategory sel = categoryTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                selectedCategory = sel;
                categoryNameField.setText(sel.getCategoryName());
                categoryAmountField.setText(String.valueOf(sel.getPrice()));
                categoryCoachingFeeField.setText(String.valueOf(sel.getCoachingFee()));
                addCategoryBtn.setText("Update Category");
                showCancelBtn(cancelCategoryBtn);
            }
        });
    }

    private void setupCategoryActionsColumn() {
        categoryActionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("Remove");
            {
                deleteBtn.getStyleClass().add("btn-delete");
                deleteBtn.setOnAction(e -> {
                    TrainingCategory cat = getTableView().getItems().get(getIndex());
                    showModal("Confirm removing \"" + cat.getCategoryName() + "\"?", () -> {
                        String result = PlanDAO.deleteTrainingCategory(cat.getId());
                        if ("OK".equals(result)) {
                            refreshCategoryTable();
                            if (cat.equals(selectedCategory)) resetCategoryForm();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Cannot Delete", result);
                        }
                    });
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
    }

    private void refreshCategoryTable() {
        categoryMasterList.setAll(PlanDAO.getAllTrainingCategories());
    }

    private void resetCategoryForm() {
        selectedCategory = null;
        categoryNameField.clear();
        categoryAmountField.clear();
        categoryCoachingFeeField.clear();
        addCategoryBtn.setText("+ Add Category");
        hideCancelBtn(cancelCategoryBtn);
        categoryTable.getSelectionModel().clearSelection();
    }

    // ──────────────────────────────────────────────────────────────
    // SECTION 3 — CLIENT DISCOUNT SETUP
    // ──────────────────────────────────────────────────────────────
    private void setupSection3() {

        // Column bindings
        clientTypeCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getTypeName()));
        feeCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        data.getValue().getMembershipFee()));
        formatCurrencyColumn(feeCol);
        discountCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getDiscount()));

        // Load from DB
        clientTypeList = PlanDAO.getAllClientTypes();
        discountTable.setItems(clientTypeList);

        // Populate combo from the same live list
        clientTypeCombo.setItems(clientTypeList);
        clientTypeCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(ClientType ct)   { return ct == null ? "" : ct.getTypeName(); }
            @Override public ClientType fromString(String s)  { return null; }
        });

        // ── Row click → populate form ──
        discountTable.setOnMouseClicked(e -> {
            ClientType sel = discountTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                selectedClientType = sel;
                clientTypeCombo.setValue(sel);
                feeField.setText(String.valueOf(sel.getMembershipFee()));
                // Strip "%" for editing convenience
                discountField.setText(sel.getDiscount().replace("%", ""));
            }
        });

        // ── Save ──
        saveDiscountBtn.setOnAction(e -> {
            ClientType ct = clientTypeCombo.getValue();
            if (ct == null) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Please select a client type.");
                return;
            }
            String feeText      = feeField.getText().trim();
            String discountText = discountField.getText().trim();

            if (feeText.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Fee cannot be empty.");
                return;
            }
            if (discountText.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Discount value cannot be empty.");
                return;
            }

            double fee;
            try {
                fee = Double.parseDouble(feeText);
                if (fee < 0) {
                    showAlert(Alert.AlertType.WARNING, "Validation", "Fee cannot be negative.");
                    return;
                }
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Fee must be a valid number (e.g. 500.00).");
                return;
            }

            // Basic numeric check before sending to DAO
            String raw = discountText.replace("%", "");
            try {
                double val = Double.parseDouble(raw);
                if (val < 0 || val > 100) {
                    showAlert(Alert.AlertType.WARNING, "Validation", "Discount must be between 0 and 100.");
                    return;
                }
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Discount must be a valid number (e.g. 30 or 30%).");
                return;
            }

            showModal("Confirm saving changes for \"" + ct.getTypeName() + "\"?", () -> {
                String result = PlanDAO.updateClientType(ct.getId(), fee, discountText);
                if ("OK".equals(result)) {
                    refreshDiscountTable();
                    resetDiscountForm();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", result);
                }
            });
        });

        // ── Cancel ──
        cancelDiscountBtn.setOnAction(e -> resetDiscountForm());
    }

    private void refreshDiscountTable() {
        clientTypeList.setAll(PlanDAO.getAllClientTypes());
        // Re-sync combo items (same list reference — already in sync)
    }

    private void resetDiscountForm() {
        selectedClientType = null;
        clientTypeCombo.setValue(null);
        feeField.clear();
        discountField.clear();
        discountTable.getSelectionModel().clearSelection();
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════

    /** Make a Double column display values as "₱ 1,234.00". */
    private <T> void formatCurrencyColumn(TableColumn<T, Double> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("₱ %,.2f", item));
                }
            }
        });
    }

    // ── Show / hide cancel button ──────────────────────────────────
    private void showCancelBtn(Button btn) {
        btn.setVisible(true);
        btn.setManaged(true);
    }

    private void hideCancelBtn(Button btn) {
        btn.setVisible(false);
        btn.setManaged(false);
    }

    // ── Inline alert (no external FXML needed) ────────────────────
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ── Confirmation modal (pure Java — no FXML load lag) ─────────
    private void showModal(String message, Runnable onConfirm) {
        Stage owner = (Stage) addPlanBtn.getScene().getWindow();

        // ── Card (VBox) ──────────────────────────────────────────
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(25);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setPadding(new javafx.geometry.Insets(30));
        card.setMinWidth(400);
        card.setStyle(
                "-fx-background-color: radial-gradient(center 50% 50%, radius 70%, #1e293b 0%, #111827 100%);" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-color: linear-gradient(to bottom, #CB443E, rgba(203,68,62,0.2));" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 15;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(203,68,62,0.4), 15, 0, 0, 0);"
        );

        // ── Question icon (circle drawn in Java — no image file needed) ──
        Label iconCircle = new Label("?");
        iconCircle.setPrefSize(90, 90);
        iconCircle.setMinSize(90, 90);
        iconCircle.setMaxSize(90, 90);
        iconCircle.setAlignment(javafx.geometry.Pos.CENTER);
        iconCircle.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #CB443E;" +
                        "-fx-border-width: 3;" +
                        "-fx-border-radius: 45;" +
                        "-fx-background-radius: 45;" +
                        "-fx-text-fill: #CB443E;" +
                        "-fx-font-size: 42px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: 'Inter';" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(203,68,62,0.5), 10, 0, 0, 0);"
        );

        // ── Message label ────────────────────────────────────────
        Label msgLabel = new Label(message);
        msgLabel.setMaxWidth(500);
        msgLabel.setWrapText(true);
        msgLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        msgLabel.setAlignment(javafx.geometry.Pos.CENTER);
        msgLabel.setStyle(
                "-fx-font-family: 'Inter';" +
                        "-fx-font-size: 22px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;"
        );

        // ── Buttons ──────────────────────────────────────────────
        Button confirmBtn = new Button("Confirm");
        confirmBtn.setPrefWidth(151);
        confirmBtn.setPrefHeight(55);
        confirmBtn.setStyle(
                "-fx-background-color: #CB443E;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 20;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 24px;" +
                        "-fx-padding: 10 25;" +
                        "-fx-cursor: hand;"
        );
        confirmBtn.setOnMouseEntered(e ->
                confirmBtn.setStyle(confirmBtn.getStyle().replace("#CB443E", "#d9635d")));
        confirmBtn.setOnMouseExited(e ->
                confirmBtn.setStyle(confirmBtn.getStyle().replace("#d9635d", "#CB443E")));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setPrefWidth(140);
        cancelBtn.setPrefHeight(45);
        cancelBtn.setStyle(
                "-fx-background-color: white;" +
                        "-fx-text-fill: #0D1117;" +
                        "-fx-background-radius: 20;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 24px;" +
                        "-fx-padding: 10 25;" +
                        "-fx-cursor: hand;"
        );
        cancelBtn.setOnMouseEntered(e ->
                cancelBtn.setStyle(cancelBtn.getStyle().replace("white", "#E8E6E9")));
        cancelBtn.setOnMouseExited(e ->
                cancelBtn.setStyle(cancelBtn.getStyle().replace("#E8E6E9", "white")));

        javafx.scene.layout.HBox btnRow = new javafx.scene.layout.HBox(20);
        btnRow.setAlignment(javafx.geometry.Pos.CENTER);
        btnRow.getChildren().addAll(confirmBtn, cancelBtn);

        card.getChildren().addAll(iconCircle, msgLabel, btnRow);

        // ── Root wrapper (transparent bg for rounded corners) ────
        javafx.scene.layout.AnchorPane root = new javafx.scene.layout.AnchorPane(card);
        root.setStyle("-fx-background-color: transparent;");
        javafx.scene.layout.AnchorPane.setTopAnchor(card, 10.0);
        javafx.scene.layout.AnchorPane.setBottomAnchor(card, 10.0);
        javafx.scene.layout.AnchorPane.setLeftAnchor(card, 10.0);
        javafx.scene.layout.AnchorPane.setRightAnchor(card, 10.0);

        // ── Stage ────────────────────────────────────────────────
        Stage confirmStage = new Stage();
        confirmStage.initStyle(StageStyle.TRANSPARENT);
        confirmStage.initModality(Modality.APPLICATION_MODAL);
        confirmStage.initOwner(owner);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        confirmStage.setScene(scene);

        // ── Blur owner while open ────────────────────────────────
        GaussianBlur blur = new GaussianBlur(10);
        owner.getScene().getRoot().setEffect(blur);
        confirmStage.setOnHidden(ev -> owner.getScene().getRoot().setEffect(null));

        // ── Button actions ───────────────────────────────────────
        confirmBtn.setOnAction(ev -> {
            confirmStage.close();
            if (onConfirm != null) onConfirm.run();
        });
        cancelBtn.setOnAction(ev -> confirmStage.close());

        // ── Show + center + fade in ──────────────────────────────
        confirmStage.show();
        centerStage(confirmStage, owner);

        root.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void centerStage(Stage stage, Stage owner) {
        double ownerX = owner.getX();
        double ownerY = owner.getY();
        double ownerW = owner.getWidth();
        double ownerH = owner.getHeight();
        stage.setX(ownerX + (ownerW / 2) - (stage.getWidth()  / 2));
        stage.setY(ownerY + (ownerH / 2) - (stage.getHeight() / 2));
    }
}