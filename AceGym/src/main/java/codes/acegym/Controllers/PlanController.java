package codes.acegym.Controllers;

import codes.acegym.Objects.Plan;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;

public class PlanController {

    // ── Section 1: Payment Type ──
    @FXML private Button addPlanBtn;
    @FXML private Button cancelPlanBtn;
    @FXML private TextField planNameField;
    @FXML private TextField planPeriodField;

    @FXML private TableView<Plan> planTable;
    @FXML private TableColumn<Plan, String> planNameCol;
    @FXML private TableColumn<Plan, Integer> planPeriodCol;

    // ── Section 2: Training Category ──
    @FXML private Button addCategoryBtn;
    @FXML private Button cancelCategoryBtn;
    @FXML private TextField categoryNameField;
    @FXML private TextField categoryAmountField;
    @FXML private TableView<?> categoryTable;

    // ── Section 3: Client Discount ──
    @FXML private Button saveDiscountBtn;
    @FXML private Button cancelDiscountBtn;
    @FXML private ComboBox<String> clientTypeCombo;
    @FXML private TextField discountField;
    @FXML private TableView<?> discountTable;

//    Action
    @FXML private TableColumn<Plan, Void> planActionsCol;


    // Track which row is being edited
    private Plan selectedPlan = null;
    private Object selectedCategory = null;

    // Class field — move list here
    private ObservableList<Plan> planList;

    public void initialize() {

        // ── Section 1 — Plan Table ──
        planList = FXCollections.observableArrayList(
                new Plan("Monthly Basic", 1),
                new Plan("Quarterly Pro", 3),
                new Plan("Annual VIP", 12)
        );

        planNameCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        planPeriodCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getPeriod()));

        setupPlanActionsColumn();
        planTable.setItems(planList);
        hideCancelBtn(cancelPlanBtn);

        addPlanBtn.setOnAction(e -> {
            String name = planNameField.getText().trim();
            String periodText = planPeriodField.getText().trim();

            if (name.isEmpty() || periodText.isEmpty()) return;

            int period;
            try { period = Integer.parseInt(periodText); }
            catch (NumberFormatException ex) { return; }

            if (selectedPlan == null) {
                showModal("Confirm adding this plan?", () -> {
                    planList.add(new Plan(name, period));
                    resetPlanForm();
                });
            } else {
                showModal("Confirm updating this plan?", () -> {
                    int index = planList.indexOf(selectedPlan);
                    planList.set(index, new Plan(name, period));
                    resetPlanForm();
                });
            }
        });

        cancelPlanBtn.setOnAction(e -> resetPlanForm());

        planTable.setOnMouseClicked(e -> {
            Plan selected = planTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selectedPlan = selected;
                planNameField.setText(selected.getName());
                planPeriodField.setText(String.valueOf(selected.getPeriod()));
                addPlanBtn.setText("Update Plan");
                showCancelBtn(cancelPlanBtn);
            }
        });
        // ── Section 1 — Plan Table ──

        // ── Section 2 — Category ──
        hideCancelBtn(cancelCategoryBtn);

        addCategoryBtn.setOnAction(e -> {
            if (selectedCategory == null) {
                showModal("Confirm adding this category?", () -> {
                    System.out.println("Category added.");
                    resetCategoryForm();
                });
            } else {
                showModal("Confirm updating this category?", () -> {
                    System.out.println("Category updated.");
                    resetCategoryForm();
                });
            }
        });

        cancelCategoryBtn.setOnAction(e -> resetCategoryForm());

        categoryTable.setOnMouseClicked(e -> {
            Object selected = categoryTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selectedCategory = selected;
                categoryNameField.setText("Selected Category");
                categoryAmountField.setText("100");
                addCategoryBtn.setText("Update Category");
                showCancelBtn(cancelCategoryBtn);
            }
        });

        // ── Section 2 — Category ──

        // ── Section 3 — Discount ──
        clientTypeCombo.getItems().addAll("Member", "Non-Member");

        discountTable.setOnMouseClicked(e -> {
            Object selected = discountTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                clientTypeCombo.setValue("Member");
                discountField.setText("30");
            }
        });

        saveDiscountBtn.setOnAction(e ->
                showModal("Confirm saving discount changes?", () -> {
                    System.out.println("Discount saved.");
                    resetDiscountForm();
                })
        );

        cancelDiscountBtn.setOnAction(e -> resetDiscountForm());
    }

// ── Section 3 — Discount ──

//    after initialize


//    Plan Methods
    private void setupPlanActionsColumn() {
        planActionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("Remove");

            {
                deleteBtn.getStyleClass().add("btn-delete");
                deleteBtn.setOnAction(e -> {
                    Plan plan = getTableView().getItems().get(getIndex());
                    showModal("Confirm removing \"" + plan.getName() + "\"?", () -> {
                        planList.remove(plan);
                        // If this row was being edited, reset the form
                        if (plan.equals(selectedPlan)) resetPlanForm();
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

    //    Plan Methods


    //    Other Section Methods dito ilalagay





    //    Other Section Methods dito ilalagay


    // ── Pang Reset ──
    private void resetPlanForm() {
        selectedPlan = null;
        planNameField.clear();
        planPeriodField.clear();
        addPlanBtn.setText("+ Add Plan");
        hideCancelBtn(cancelPlanBtn);
        planTable.getSelectionModel().clearSelection();
    }

    private void resetCategoryForm() {
        selectedCategory = null;
        categoryNameField.clear();
        categoryAmountField.clear();
        addCategoryBtn.setText("+ Add Category");
        hideCancelBtn(cancelCategoryBtn);
        categoryTable.getSelectionModel().clearSelection();
    }

    private void resetDiscountForm() {
        clientTypeCombo.setValue(null);
        discountField.clear();
        discountTable.getSelectionModel().clearSelection();
    }

    // ── Pang Reset ──

    // ── Show/hide cancel button ──
    private void showCancelBtn(Button btn) {
        btn.setText("Cancel");
        btn.setVisible(true);
        btn.setManaged(true);
    }

    private void hideCancelBtn(Button btn) {
        btn.setVisible(false);
        btn.setManaged(false);
    }

    // ── Show/hide cancel button ──


    // ── Modal ──
    private void showModal(String message, Runnable action) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/codes/acegym/ConfirmationPopup.fxml"));
            Parent root = loader.load();

            ConfirmationController popupController = loader.getController();
            popupController.setMessage(message);
            popupController.setOnConfirm(action);

            Stage confirmStage = new Stage();
            confirmStage.initStyle(StageStyle.TRANSPARENT);
            confirmStage.initModality(Modality.APPLICATION_MODAL);

            Stage owner = (Stage) addPlanBtn.getScene().getWindow();
            confirmStage.initOwner(owner);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            confirmStage.setScene(scene);

            GaussianBlur blur = new GaussianBlur(10);
            owner.getScene().getRoot().setEffect(blur);

            confirmStage.show();
            centerStage(confirmStage);

            root.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            confirmStage.setOnHidden(ev -> owner.getScene().getRoot().setEffect(null));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void centerStage(Stage stage) {
        double ownerX      = addPlanBtn.getScene().getWindow().getX();
        double ownerY      = addPlanBtn.getScene().getWindow().getY();
        double ownerWidth  = addPlanBtn.getScene().getWindow().getWidth();
        double ownerHeight = addPlanBtn.getScene().getWindow().getHeight();
        stage.setX(ownerX + (ownerWidth  / 2) - (stage.getWidth()  / 2));
        stage.setY(ownerY + (ownerHeight / 2) - (stage.getHeight() / 2));
    }
}

// ── Modal ──