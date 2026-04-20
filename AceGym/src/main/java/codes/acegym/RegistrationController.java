package codes.acegym;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.GaussianBlur;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.io.IOException;

public class RegistrationController {

    @FXML private Button AvailMembershipBtn;
    @FXML private Button RegisterClientBtn;

    @FXML
    public void initialize() {
        // Set actions for your buttons
        RegisterClientBtn.setOnAction(e -> showModal("Confirm Client Registration?"));
        AvailMembershipBtn.setOnAction(e -> showModal("Confirm Membership Availment?"));
    }

    private void showModal(String message) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/codes/acegym/ConfirmationPopup.fxml"));
            Parent root = loader.load();

            ConfirmationController popupController = loader.getController();
            popupController.setMessage(message);

            Stage confirmStage = new Stage();
            confirmStage.initStyle(StageStyle.TRANSPARENT);
            confirmStage.initModality(Modality.APPLICATION_MODAL);

            // Get the current window (owner) for the blur effect
            Stage owner = (Stage) RegisterClientBtn.getScene().getWindow();
            confirmStage.initOwner(owner);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            confirmStage.setScene(scene);

            // 1. Apply Gaussian Blur to the main window
            GaussianBlur blur = new GaussianBlur(10);
            owner.getScene().getRoot().setEffect(blur);

            // 2. Center and Show
            confirmStage.show();
            centerStage(confirmStage);

            // 3. Fade In Animation for the popup
            root.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            // 4. Remove Blur when popup is closed
            confirmStage.setOnHidden(e -> owner.getScene().getRoot().setEffect(null));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void centerStage(Stage stage) {
        // Gets the main window position and sizes
        double ownerX = RegisterClientBtn.getScene().getWindow().getX();
        double ownerY = RegisterClientBtn.getScene().getWindow().getY();
        double ownerWidth = RegisterClientBtn.getScene().getWindow().getWidth();
        double ownerHeight = RegisterClientBtn.getScene().getWindow().getHeight();

        // Sets the popup in the exact middle
        stage.setX(ownerX + (ownerWidth / 2) - (stage.getWidth() / 2));
        stage.setY(ownerY + (ownerHeight / 2) - (stage.getHeight() / 2));
    }
}