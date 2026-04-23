package codes.acegym.Controllers;

import codes.acegym.DB.ClientDAO;
import codes.acegym.DB.CoachDAO;
import codes.acegym.Objects.Client;
import codes.acegym.Objects.Coach;
import javafx.animation.FadeTransition;
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
import javafx.util.StringConverter;

public class AddClientInCoachController {

    @FXML private VBox             mainListView;
    @FXML private VBox             clientDetailsView;
    @FXML private VBox             clientListContainer;

    // Main list view
    @FXML private Label            headerLabel;

    // Add-client view — now a ComboBox instead of search TextField
    @FXML private ComboBox<Client> clientComboBox;
    @FXML private TextField        displayClientId;
    @FXML private TextField        displayFullName;
    @FXML private TextField        displayClientType;
    @FXML private TextField        displayContact;
    @FXML private Label            errorLabel;

    private Coach               coach;
    private CoachCardController cardController;
    private int                 selectedClientID = -1;

    // ── Injected by CoachCardController ─────────────────────────────────────
    public void setCoach(Coach coach, CoachCardController cardController) {
        this.coach          = coach;
        this.cardController = cardController;
        if (headerLabel != null)
            headerLabel.setText("Manage Clients for Coach " + coach.getFullName());
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

        setupClientComboBox();
    }

    // ── ComboBox: editable so user can type to search, selects auto-fills details
    private void setupClientComboBox() {
        if (clientComboBox == null) return;

        clientComboBox.setEditable(true);   // allow typing to filter
        clientComboBox.setPromptText("Type or select a client...");

        // Style the combo to match the dark theme
        clientComboBox.setStyle(
                "-fx-background-color: #1c2237;" +
                        "-fx-border-color: #2e3349;" +
                        "-fx-border-radius: 8;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-text-fill: #e8e8f0;" +
                        "-fx-font-size: 13px;" +
                        "-fx-padding: 6 10 6 10;");

        // Display "First Last" in the combo
        clientComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(Client c)   { return c == null ? "" : c.getFullName(); }
            @Override public Client fromString(String s) {
                // When user types, find matching client from current items
                if (s == null || s.isBlank()) return null;
                for (Client c : clientComboBox.getItems()) {
                    if (c.getFullName().equalsIgnoreCase(s.trim())) return c;
                }
                return null;
            }
        });

        // Live filter the dropdown as user types
        clientComboBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            // If the user is typing (not a programmatic set from selection)
            Client currentVal = clientComboBox.getValue();
            if (currentVal != null && currentVal.getFullName().equals(newVal)) return;

            // Filter shown items to match typed text
            String lower = newVal.toLowerCase();
            ObservableList<Client> allItems = (ObservableList<Client>) clientComboBox.getUserData();
            if (allItems == null) return;

            javafx.collections.ObservableList<Client> filtered =
                    javafx.collections.FXCollections.observableArrayList();
            for (Client c : allItems) {
                if (c.getFullName().toLowerCase().contains(lower)) filtered.add(c);
            }
            clientComboBox.setItems(filtered);
            if (!filtered.isEmpty()) clientComboBox.show();

            // Clear display if user is typing a new search
            selectedClientID = -1;
            clearDisplayFields();
        });

        // Auto-fill details when a client is selected from dropdown
        clientComboBox.setOnAction(e -> {
            Client selected = clientComboBox.getValue();
            if (selected != null) {
                selectedClientID = selected.getClientID();
                displayClientId.setText("ST" + String.format("%03d", selected.getClientID()));
                displayFullName.setText(selected.getFullName());
                displayClientType.setText(selected.getClientType());
                displayContact.setText(
                        selected.getContact() == null || selected.getContact().isBlank()
                                ? "N/A" : selected.getContact());
                hideError();
            }
        });
    }

    // ── Load unassigned clients into the combo (called when switching view) ──
    private void refreshComboBox() {
        if (clientComboBox == null || coach == null) return;
        ObservableList<Client> unassigned = ClientDAO.getUnassignedClients(coach.getStaffID());
        // Store master list in userData so the live-filter can always reset to it
        clientComboBox.setUserData(unassigned);
        clientComboBox.setItems(javafx.collections.FXCollections.observableArrayList(unassigned));
        clientComboBox.setValue(null);
        clientComboBox.getEditor().clear();
        clearDisplayFields();
        selectedClientID = -1;
    }

    // ── Load currently assigned clients from DB into the main list ───────────
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
            // row: [assignmentID, clientID, firstName, lastName, coachingPrice]
            addClientRowToList(
                    row[2] + " " + row[3],
                    row[1],
                    Integer.parseInt(row[0])
            );
        }
    }

    // ── Build a single client row card in the assigned list ──────────────────
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
        nameLabel.setStyle("-fx-text-fill: #e8e8f0; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label idLabel = new Label("Client ID: " + clientIDStr);
        idLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
        info.getChildren().addAll(nameLabel, idLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button removeBtn = new Button("Remove");
        removeBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #e53935;" +
                        "-fx-border-radius: 6;" +
                        "-fx-text-fill: #e53935;" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 5 14 5 14;" +
                        "-fx-cursor: hand;");

        // ── Remove requires confirmation popup ───────────────────────────────
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

    // ── Switch to the add-client view ────────────────────────────────────────
    @FXML
    private void handleSwitchToSelection() {
        mainListView.setVisible(false);
        clientDetailsView.setVisible(true);
        refreshComboBox();
        hideError();
    }

    // ── Confirm adding the selected client ───────────────────────────────────
    @FXML
    private void handleConfirmAddClient() {
        if (selectedClientID < 0) {
            showError("Please select a client from the list.");
            if (clientComboBox != null) {
                clientComboBox.setStyle(
                        clientComboBox.getStyle() +
                                "; -fx-border-color: #e53935; -fx-border-width: 2;");
            }
            return;
        }

        showConfirmPopup(
                "Add " + displayFullName.getText() + " to this coach?",
                () -> {
                    boolean ok = CoachDAO.assignClient(coach.getStaffID(), selectedClientID, 0);
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

    // ── Confirmation popup (reusable inline) ─────────────────────────────────
    private void showConfirmPopup(String message, Runnable onConfirm) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initOwner((Stage) mainListView.getScene().getWindow());

        VBox root = new VBox(20);
        root.setPadding(new Insets(30, 28, 24, 28));
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(360);
        root.setStyle(
                "-fx-background-color: #1c2237;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #2e3349;" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.85), 40, 0, 0, 0);");

        Label icon = new Label("❓");
        icon.setStyle("-fx-font-size: 30px;");

        Label msg = new Label(message);
        msg.setStyle("-fx-text-fill: #c9cdd6; -fx-font-size: 14px;");
        msg.setWrapText(true);
        msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #4b5563;" +
                        "-fx-border-radius: 8;" +
                        "-fx-text-fill: #9ca3af;" +
                        "-fx-font-size: 13px;" +
                        "-fx-padding: 8 20 8 20;" +
                        "-fx-cursor: hand;");

        Button confirmBtn = new Button("Confirm");
        confirmBtn.setStyle(
                "-fx-background-color: #e53935;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13px;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 8 20 8 20;" +
                        "-fx-cursor: hand;");

        cancelBtn.setOnAction(e  -> popup.close());
        confirmBtn.setOnAction(e -> { popup.close(); onConfirm.run(); });

        btnRow.getChildren().addAll(cancelBtn, confirmBtn);
        root.getChildren().addAll(icon, msg, btnRow);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);

        popup.show();

        Stage owner = (Stage) mainListView.getScene().getWindow();
        popup.setX(owner.getX() + (owner.getWidth()  / 2) - (popup.getWidth()  / 2));
        popup.setY(owner.getY() + (owner.getHeight() / 2) - (popup.getHeight() / 2));

        root.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(180), root);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void clearDisplayFields() {
        if (displayClientId   != null) displayClientId.clear();
        if (displayFullName   != null) displayFullName.clear();
        if (displayClientType != null) displayClientType.clear();
        if (displayContact    != null) displayContact.clear();
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