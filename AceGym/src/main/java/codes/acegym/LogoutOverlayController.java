package codes.acegym;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
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

    // Thread-safe holder for the preloaded login page
    private final AtomicReference<Parent> preloadedLogin = new AtomicReference<>(null);

    @FXML
    private void confirmLogout(ActionEvent event) {

        // 1. Update UI immediately
        buttonBox.setVisible(false);
        headerLabel.setText("Logged Out!");
        subHeaderLabel.setText("You have been successfully logged out. See you next time!");

        // 2. Capture stage references before any async work
        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Stage mainStage    = (Stage) currentStage.getOwner();
        boolean wasFullScreen = mainStage.isFullScreen();
        boolean wasMaximized  = mainStage.isMaximized();

        // 3. Load login.fxml in background RIGHT NOW during the 1.5s pause
        Thread loaderThread = new Thread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
                Parent root = loader.load();
                preloadedLogin.set(root); // thread-safe set
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        loaderThread.setDaemon(true);
        loaderThread.start();

        // 4. After 1.5s, fade out then swap — no joining, just check if ready
        PauseTransition pause = new PauseTransition(Duration.millis(1500));
        pause.setOnFinished(e -> {

            FadeTransition fadeOut = new FadeTransition(Duration.millis(300),
                    currentStage.getScene().getRoot());
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(fe -> {

                Parent loginRoot = preloadedLogin.get();

                if (loginRoot == null) {
                    // Rare edge case: still loading, wait a tiny bit more
                    PauseTransition retry = new PauseTransition(Duration.millis(200));
                    retry.setOnFinished(re -> swapToLogin(currentStage, mainStage,
                            wasFullScreen, wasMaximized));
                    retry.play();
                } else {
                    swapToLogin(currentStage, mainStage, wasFullScreen, wasMaximized);
                }
            });
            fadeOut.play();
        });

        pause.play();
    }

    private void swapToLogin(Stage currentStage, Stage mainStage,
                             boolean wasFullScreen, boolean wasMaximized) {
        Parent loginRoot = preloadedLogin.get();
        if (loginRoot == null) return;

        currentStage.close();
        mainStage.setScene(new Scene(loginRoot));
        mainStage.show();
    }

    @FXML
    private void cancelLogout(ActionEvent event) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200),
                ((Node) event.getSource()).getScene().getRoot());
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.close();
        });
        fadeOut.play();
    }
}