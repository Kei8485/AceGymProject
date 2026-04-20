package codes.acegym;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class LogoutOverlayController {

    @FXML private Label headerLabel;
    @FXML private Label subHeaderLabel;
    @FXML private HBox buttonBox;

    private final AtomicReference<Parent> preloadedLogin = new AtomicReference<>(null);
    private HomePageController homeController;

    public void setHomeController(HomePageController controller) {
        this.homeController = controller;
    }

    @FXML
    private void confirmLogout(ActionEvent event) {
        buttonBox.setVisible(false);
        headerLabel.setText("Logged Out!");
        subHeaderLabel.setText("You have been successfully logged out. See you next time!");

        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Stage mainStage = (Stage) currentStage.getOwner();

        Thread loaderThread = new Thread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
                preloadedLogin.set(loader.load());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        loaderThread.setDaemon(true);
        loaderThread.start();

        PauseTransition pause = new PauseTransition(Duration.millis(1500));
        pause.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentStage.getScene().getRoot());
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(fe -> {
                if (preloadedLogin.get() == null) {
                    PauseTransition retry = new PauseTransition(Duration.millis(200));
                    retry.setOnFinished(re -> swapToLogin(currentStage, mainStage));
                    retry.play();
                } else {
                    swapToLogin(currentStage, mainStage);
                }
            });
            fadeOut.play();
        });

        pause.play();
    }

    private void swapToLogin(Stage currentStage, Stage mainStage) {
        Parent loginRoot = preloadedLogin.get();
        if (loginRoot == null) return;

        // Ensure background tasks in the Home Page completely stop
        if (homeController != null) {
            homeController.stopAnimations();
        }

        currentStage.close();
        mainStage.setScene(new Scene(loginRoot));
        mainStage.show();
    }

    @FXML
    private void cancelLogout(ActionEvent event) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), ((Node) event.getSource()).getScene().getRoot());
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.close();
        });
        fadeOut.play();
    }
}