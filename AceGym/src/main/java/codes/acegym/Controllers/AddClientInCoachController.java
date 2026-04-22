package codes.acegym.Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class AddClientInCoachController {

    @FXML private VBox mainListView, clientDetailsView, clientListContainer;
    @FXML private TextField clientSearchField, searchClientInput;
    @FXML private TextField displayClientId, displayFullName, displayEmail;

    @FXML
    public void initialize() {
        // 1. Default View State: Show current coach clients first
        mainListView.setVisible(true);
        clientDetailsView.setVisible(false);

        // 2. Load Dummy Data for existing coach clients
        loadDummyCoachClients();
    }

    private void loadDummyCoachClients() {
        // This represents clients David Domingo already handles
        addClientToMainList("Steve Rogers", "001");
        addClientToMainList("Bucky Barnes", "002");
    }

    @FXML
    private void handleSwitchToSelection() {
        // Triggered by the '+' button to find new clients
        mainListView.setVisible(false);
        clientDetailsView.setVisible(true);

        // Clear previous search fields
        searchClientInput.clear();
        clearDisplayFields();
    }

    @FXML
    private void handleSearchClient() {
        String input = searchClientInput.getText().trim();

        // Dummy Search Logic
        if (input.equalsIgnoreCase("Tony") || input.equals("102")) {
            displayClientId.setText("C-102");
            displayFullName.setText("Tony Stark");
            displayEmail.setText("stark@starkindustries.com");
        } else if (!input.isEmpty()) {
            displayClientId.setText("C-999");
            displayFullName.setText("Generic Client");
            displayEmail.setText("client@email.com");
        }
    }

    @FXML
    private void handleConfirmAddClient() {
        // Only add if we actually identified a client
        if (!displayFullName.getText().isEmpty()) {
            addClientToMainList(displayFullName.getText(), displayClientId.getText());
            handleBackToMain();
        }
    }

    private void addClientToMainList(String name, String id) {
        HBox card = new HBox();
        card.getStyleClass().add("client-card");

        VBox info = new VBox(5);
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("client-name");
        Label idLabel = new Label("ClientID: " + id);
        idLabel.getStyleClass().add("client-id");

        info.getChildren().addAll(nameLabel, idLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button removeBtn = new Button("Remove");
        removeBtn.getStyleClass().add("remove-button");
        removeBtn.setOnAction(e -> clientListContainer.getChildren().remove(card));

        card.getChildren().addAll(info, removeBtn);
        clientListContainer.getChildren().add(card);
    }

    private void clearDisplayFields() {
        displayClientId.clear();
        displayFullName.clear();
        displayEmail.clear();
    }

    @FXML
    private void handleBackToMain() {
        mainListView.setVisible(true);
        clientDetailsView.setVisible(false);
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) mainListView.getScene().getWindow();
        stage.close();
    }
}