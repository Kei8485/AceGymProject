package codes.acegym;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;

public class PaymentController {

    @FXML
    private Button AddPaymentBtn;

    public void initialize() {
        // Set action for the payment button
        AddPaymentBtn.setOnAction(e -> showModal("Confirm Payment Process?", () -> {
            // Your payment logic goes here
            System.out.println("Payment processed successfully!");
        }));
    }

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

            Stage owner = (Stage) AddPaymentBtn.getScene().getWindow();
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

            confirmStage.setOnHidden(e -> owner.getScene().getRoot().setEffect(null));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void centerStage(Stage stage) {
        double ownerX = AddPaymentBtn.getScene().getWindow().getX();
        double ownerY = AddPaymentBtn.getScene().getWindow().getY();
        double ownerWidth = AddPaymentBtn.getScene().getWindow().getWidth();
        double ownerHeight = AddPaymentBtn.getScene().getWindow().getHeight();

        stage.setX(ownerX + (ownerWidth / 2) - (stage.getWidth() / 2));
        stage.setY(ownerY + (ownerHeight / 2) - (stage.getHeight() / 2));
    }
}