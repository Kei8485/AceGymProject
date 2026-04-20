package codes.acegym;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.control.Button;

public class ConfirmationController {

    @FXML
    private Label messageLabel;

    @FXML
    private Button confirmBtn;

    @FXML
    private Button cancelBtn;

    // 1. This must match the onAction in your FXML
    @FXML
    private void handleConfirm() {
        System.out.println("User confirmed!");
        // Add your logic here (save to database, etc.)
        closeStage();
    }

    // 2. This must match the onAction in your FXML
    @FXML
    private void handleCancel() {
        System.out.println("User cancelled.");
        closeStage();
    }

    public void setMessage(String text) {
        messageLabel.setText(text);
    }

    private void closeStage() {
        Stage stage = (Stage) messageLabel.getScene().getWindow();
        stage.close();
    }
}