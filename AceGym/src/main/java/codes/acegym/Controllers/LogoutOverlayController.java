package codes.acegym.Controllers;

import codes.acegym.Application_Launcher.AceGymApplication; // Import this for the Pref keys
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
import java.util.prefs.Preferences; // Required for saving settings

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
        // --- 1. STOP REMEMBER ME ---
        // Access the same preference node used in AceGymApplication
        Preferences prefs = Preferences.userRoot().node(AceGymApplication.PREF_NODE);
        prefs.putBoolean(AceGymApplication.KEY_REMEMBER, false);
        // Note: We keep KEY_USERNAME so the field stays pre-filled, but auto-login stops.

        // --- 2. UI VISUAL FEEDBACK ---
        buttonBox.setVisible(false);
        headerLabel.setText("Logged Out!");
        subHeaderLabel.setText("You have been successfully logged out. See you next time!");

        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Stage mainStage = (Stage) currentStage.getOwner();

        // --- 3. BACKGROUND LOADING ---
        Thread loaderThread = new Thread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/codes/acegym/Login.fxml"));
                preloadedLogin.set(loader.load());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        loaderThread.setDaemon(true);
        loaderThread.start();

        // --- 4. TRANSITION LOGIC ---
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

        if (homeController != null) {
            homeController.stopAnimations();
        }

        currentStage.close();
        mainStage.setScene(new Scene(loginRoot));
        mainStage.centerOnScreen(); // Optional: keeps the login centered
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