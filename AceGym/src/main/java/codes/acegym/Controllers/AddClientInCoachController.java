package codes.acegym.Controllers;

import codes.acegym.DB.ClientDAO;
import codes.acegym.DB.CoachDAO;
import codes.acegym.Objects.Client;
import codes.acegym.Objects.Coach;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class AddClientInCoachController {

    // ── FXML — main list view ────────────────────────────────────────────────
    @FXML private VBox  mainListView;
    @FXML private Label headerLabel;
    @FXML private VBox  clientListContainer;

    // ── FXML — add-client view ───────────────────────────────────────────────
    @FXML private VBox              clientDetailsView;
    @FXML private TextField         searchField;
    @FXML private ListView<Client>  clientListView;
    @FXML private TextField         displayClientId;
    @FXML private TextField         displayFullName;
    @FXML private TextField         displayClientType;
    @FXML private TextField         displayContact;
    @FXML private TextField         displayEmail;
    @FXML private Label             errorLabel;

    // ── State ────────────────────────────────────────────────────────────────
    private Coach               coach;
    private CoachCardController cardController;
    private Client              selectedClient  = null;
    private ObservableList<Client> allUnassigned    = FXCollections.observableArrayList();
    // Stable list bound to the ListView ONCE — never replaced via setItems().
    // Filtered by mutating in place inside Platform.runLater() to avoid the
    // JavaFX 21 IndexOutOfBoundsException when a click is in-flight.
    private final ObservableList<Client> displayedClients = FXCollections.observableArrayList();

    // ── Injected by CoachCardController ─────────────────────────────────────
    public void setCoach(Coach coach, CoachCardController cardController) {
        this.coach          = coach;
        this.cardController = cardController;
        if (headerLabel != null)
            headerLabel.setText("Manage Clients — " + coach.getFullName());
        loadAssignedClients();
    }

    @FXML
    public void initialize() {
        mainListView.setVisible(true);
        clientDetailsView.setVisible(false);

        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }

        setupSearch();
        setupListView();
    }

    // ── Search TextField — filters ListView live ─────────────────────────────
    private void setupSearch() {
        if (searchField == null) return;
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String q = (newVal == null) ? "" : newVal.trim().toLowerCase();

            // Build the result list immediately (safe — no UI mutation yet)
            final java.util.List<Client> next = new java.util.ArrayList<>();
            if (q.isEmpty()) {
                next.addAll(allUnassigned);
            } else {
                for (Client c : allUnassigned) {
                    if (c.getFullName().toLowerCase().contains(q)) next.add(c);
                }
            }

            // Defer the mutation to the next pulse so we never touch the
            // ListView's backing list while a mouse-click event is in-flight.
            // JavaFX 21 IndexOutOfBoundsException fix — same root cause as
            // the ComboBox/TableView crashes fixed elsewhere in this project.
            javafx.application.Platform.runLater(() -> {
                clientListView.getSelectionModel().clearSelection();
                displayedClients.setAll(next);   // mutates in place — setItems() never called again
                clearDisplayFields();
                selectedClient = null;
            });
        });
    }

    // ── ListView — fills detail panel on selection ───────────────────────────
    private void setupListView() {
        if (clientListView == null) return;

        clientListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Client c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(c.getFullName() + "   [" + c.getClientType() + "]");
                    setStyle("-fx-text-fill: #e8e8f0; -fx-font-size: 13px;" +
                            "-fx-background-color: transparent;");
                }
            }
        });

        clientListView.setStyle(
                "-fx-background-color: #1c2237;" +
                        "-fx-border-color: #2e3349;" +
                        "-fx-border-radius: 8;" +
                        "-fx-border-width: 1.5;");

        // Bind the ListView to the stable list ONCE — setupSearch() filters it in place.
        clientListView.setItems(displayedClients);

        clientListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newClient) -> {
                    if (newClient == null) {
                        clearDisplayFields();
                        selectedClient = null;
                        return;
                    }
                    selectedClient = newClient;
                    displayClientId.setText("CL" + String.format("%03d", newClient.getClientID()));
                    displayFullName.setText(newClient.getFullName());
                    displayClientType.setText(newClient.getClientType());
                    displayContact.setText(
                            newClient.getContact() == null || newClient.getContact().isBlank()
                                    ? "N/A" : newClient.getContact());
                    if (displayEmail != null)
                        displayEmail.setText(
                                newClient.getEmail() == null || newClient.getEmail().isBlank()
                                        ? "N/A" : newClient.getEmail());
                    hideError();
                });
    }

    // ── Load currently assigned clients ──────────────────────────────────────
    private void loadAssignedClients() {
        if (clientListContainer == null || coach == null) return;
        clientListContainer.getChildren().clear();

        ObservableList<String[]> assigned = CoachDAO.getAssignedClients(coach.getStaffID());
        if (assigned.isEmpty()) {
            Label none = new Label("No clients assigned yet.");
            none.setStyle("-fx-text-fill: #6b7280; -fx-font-style: italic;");
            clientListContainer.getChildren().add(none);
            return;
        }
        for (String[] row : assigned) {
            addClientRowToList(row[2] + " " + row[3], row[1], Integer.parseInt(row[0]));
        }
    }

    private void addClientRowToList(String name, String clientIDStr, int assignmentID) {
        HBox card = new HBox();
        card.setStyle(
                "-fx-background-color: #1c2237;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #2e3349;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 12 16 12 16;");
        card.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(4);
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-text-fill: #e8e8f0; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label idLbl = new Label("Client ID: CL" +
                String.format("%03d", Integer.parseInt(clientIDStr)));
        idLbl.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 16px;");
        info.getChildren().addAll(nameLabel, idLbl);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button removeBtn = new Button("Remove");
        removeBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #e53935; -fx-border-radius: 6;" +
                        "-fx-text-fill: #e53935; -fx-font-size: 16px;" +
                        "-fx-padding: 5 14 5 14; -fx-cursor: hand;");

        removeBtn.setOnAction(e -> showConfirmPopup(
                "Remove " + name + " from this coach?",
                () -> {
                    if (CoachDAO.removeClientAssignment(assignmentID)) {
                        clientListContainer.getChildren().remove(card);
                        if (cardController != null) cardController.refreshClientsList();
                        if (clientListContainer.getChildren().isEmpty()) {
                            Label none = new Label("No clients assigned yet.");
                            none.setStyle("-fx-text-fill: #6b7280; -fx-font-style: italic;");
                            clientListContainer.getChildren().add(none);
                        }
                    }
                }
        ));

        card.getChildren().addAll(info, removeBtn);
        clientListContainer.getChildren().add(card);
    }

    @FXML
    private void handleSwitchToSelection() {
        mainListView.setVisible(false);
        clientDetailsView.setVisible(true);

        allUnassigned = ClientDAO.getUnassignedClients(coach.getStaffID());
        // Mutate the stable list in place — never call setItems() again
        javafx.application.Platform.runLater(() -> {
            displayedClients.setAll(allUnassigned);
            clientListView.getSelectionModel().clearSelection();
        });

        if (searchField != null) searchField.clear();
        clearDisplayFields();
        selectedClient = null;
        hideError();
    }

    @FXML
    private void handleConfirmAddClient() {
        if (selectedClient == null) {
            showError("Please select a client from the list.");
            return;
        }
        showConfirmPopup(
                "Assign " + selectedClient.getFullName() + " to this coach?",
                () -> {
                    boolean ok = CoachDAO.assignClient(
                            coach.getStaffID(), selectedClient.getClientID(), 0);
                    if (ok) {
                        if (cardController != null) cardController.refreshClientsList();
                        handleBackToMain();
                        loadAssignedClients();
                    } else {
                        showError("Failed to assign client. They may already be assigned.");
                    }
                }
        );
    }

    @FXML
    private void handleBackToMain() {
        mainListView.setVisible(true);
        clientDetailsView.setVisible(false);
        hideError();
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) mainListView.getScene().getWindow();
        stage.close();
    }

    private void showConfirmPopup(String message, Runnable onConfirm) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initOwner((Stage) mainListView.getScene().getWindow());

        VBox root = new VBox(20);
        root.setPadding(new Insets(35, 35, 35, 35));
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(480);
        root.setMaxWidth(480);
        root.setStyle(
                "-fx-background-color: #1c2237;" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: #e53935;" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.85), 40, 0, 0, 0);");

        // ── Icon badge ──
        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setMinSize(60, 60);
        iconBox.setMaxSize(60, 60);
        iconBox.setStyle(
                "-fx-background-color: #e53935;" +
                        "-fx-background-radius: 16;");
        Label icon = new Label("?");
        icon.setStyle(
                "-fx-font-family: 'Bebas Neue';" +
                        "-fx-font-size: 36px;" +
                        "-fx-text-fill: #ffffff;");
        iconBox.getChildren().add(icon);

        // ── Title ──
        Label title = new Label("Confirm Action");
        title.setStyle(
                "-fx-text-fill: #ffffff;" +
                        "-fx-font-family: 'Inter';" +
                        "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;");

        // ── Divider ──
        javafx.scene.layout.Region divider = new javafx.scene.layout.Region();
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setStyle("-fx-background-color: #2e3349;");

        // ── Message ──
        Label msg = new Label(message);
        msg.setStyle(
                "-fx-text-fill: #9da3b4;" +
                        "-fx-font-family: 'Inter';" +
                        "-fx-font-size: 15px;" +
                        "-fx-line-spacing: 4;");
        msg.setWrapText(true);
        msg.setMaxWidth(410);
        msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        msg.setAlignment(Pos.CENTER);

        // ── Buttons ──
        HBox btnRow = new HBox(14);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setPrefWidth(140);
        cancelBtn.setPrefHeight(44);
        cancelBtn.setStyle(
                "-fx-background-color: #1e2130;" +
                        "-fx-text-fill: #c0c5d8;" +
                        "-fx-font-family: 'Inter';" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #2e3349;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-cursor: hand;");
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(
                "-fx-background-color: #252a3a;" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-font-family: 'Inter';" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #3e4460;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-cursor: hand;"));
        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(
                "-fx-background-color: #1e2130;" +
                        "-fx-text-fill: #c0c5d8;" +
                        "-fx-font-family: 'Inter';" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #2e3349;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-cursor: hand;"));

        Button confirmBtn = new Button("Confirm");
        confirmBtn.setPrefWidth(160);
        confirmBtn.setPrefHeight(44);
        confirmBtn.setStyle(
                "-fx-background-color: #e53935;" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-font-family: 'Inter';" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: transparent;" +
                        "-fx-border-radius: 10;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(229,57,53,0.45), 12, 0, 0, 3);");
        confirmBtn.setOnMouseEntered(e -> confirmBtn.setStyle(
                "-fx-background-color: #c62828;" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-font-family: 'Inter';" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: transparent;" +
                        "-fx-border-radius: 10;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(229,57,53,0.65), 18, 0, 0, 4);"));
        confirmBtn.setOnMouseExited(e -> confirmBtn.setStyle(
                "-fx-background-color: #e53935;" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-font-family: 'Inter';" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: transparent;" +
                        "-fx-border-radius: 10;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(229,57,53,0.45), 12, 0, 0, 3);"));

        cancelBtn.setOnAction(e -> popup.close());
        confirmBtn.setOnAction(e -> { popup.close(); onConfirm.run(); });

        btnRow.getChildren().addAll(cancelBtn, confirmBtn);
        root.getChildren().addAll(iconBox, title, divider, msg, btnRow);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.show();

        Stage owner = (Stage) mainListView.getScene().getWindow();
        popup.setX(owner.getX() + (owner.getWidth()  / 2) - (popup.getWidth()  / 2));
        popup.setY(owner.getY() + (owner.getHeight() / 2) - (popup.getHeight() / 2));

        root.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(200), root);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }
    private void clearDisplayFields() {
        if (displayClientId   != null) displayClientId.clear();
        if (displayFullName   != null) displayFullName.clear();
        if (displayClientType != null) displayClientType.clear();
        if (displayContact    != null) displayContact.clear();
        if (displayEmail      != null) displayEmail.clear();
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }
}