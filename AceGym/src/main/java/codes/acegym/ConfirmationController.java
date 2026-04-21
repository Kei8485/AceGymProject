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

    // This is the "container" for your lambda code
    private Runnable onConfirmAction;

    // This method allows other controllers to "inject" their logic
    public void setOnConfirm(Runnable action) {
        this.onConfirmAction = action;
    }

    public void setMessage(String text) {
        messageLabel.setText(text);
    }

    @FXML
    private void handleConfirm() {
        // Run the lambda code ONLY when the button is clicked
        if (onConfirmAction != null) {
            onConfirmAction.run();
        }
        closeStage();
    }

    @FXML
    private void handleCancel() {
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) messageLabel.getScene().getWindow();
        stage.close();
    }
}