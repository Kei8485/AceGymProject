package codes.acegym;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.io.IOException;

public class CoachCardController {

    @FXML private Button manageClientBtn, editCoachBtn;
    @FXML private ImageView removeCoachBtn; // The trash icon

    @FXML
    public void initialize() {
        // 1. Manage Clients
        if (manageClientBtn != null) {
            manageClientBtn.setOnAction(e -> openModal("/codes/acegym/AddClientInCoach.fxml"));
        }

        // 2. Edit Coach
        if (editCoachBtn != null) {
            editCoachBtn.setOnAction(e -> openModal("/codes/acegym/EditCoach.fxml"));
        }

        // 3. Remove Coach (Delete Logic via Popup)
        if (removeCoachBtn != null) {
            removeCoachBtn.setOnMouseClicked(e -> showModal("Are you sure you want to remove this coach?", () -> {
                System.out.println("Executing Delete Logic for this specific coach...");
                // Place your SQL/DAO delete code here
            }));
        }
    }

    private void showModal(String message, Runnable action) {
        try {
            // Using the path from your RegistrationController example
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/codes/acegym/ConfirmationPopup.fxml"));
            Parent root = loader.load();

            ConfirmationController popupController = loader.getController();
            popupController.setMessage(message);
            popupController.setOnConfirm(action);

            Stage confirmStage = new Stage();
            confirmStage.initStyle(StageStyle.TRANSPARENT);
            confirmStage.initModality(Modality.APPLICATION_MODAL);

            // Get owner from the ImageView or Buttons
            Stage owner = (Stage) removeCoachBtn.getScene().getWindow();
            confirmStage.initOwner(owner);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            confirmStage.setScene(scene);

            GaussianBlur blur = new GaussianBlur(10);
            owner.getScene().getRoot().setEffect(blur);

            confirmStage.show();
            centerStage(confirmStage);

            // Smooth Fade In
            root.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            confirmStage.setOnHidden(e -> owner.getScene().getRoot().setEffect(null));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openModal(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage modalStage = new Stage();
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.initModality(Modality.APPLICATION_MODAL);

            Stage owner = (Stage) manageClientBtn.getScene().getWindow();
            modalStage.initOwner(owner);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            modalStage.setScene(scene);

            owner.getScene().getRoot().setEffect(new GaussianBlur(10));
            modalStage.show();
            centerStage(modalStage);

            root.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            modalStage.setOnHidden(e -> owner.getScene().getRoot().setEffect(null));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void centerStage(Stage stage) {
        // Get the owner stage via the scene window
        Stage owner = (Stage) editCoachBtn.getScene().getWindow();

        stage.setX(owner.getX() + (owner.getWidth() / 2) - (stage.getWidth() / 2));
        stage.setY(owner.getY() + (owner.getHeight() / 2) - (stage.getHeight() / 2));
    }
}