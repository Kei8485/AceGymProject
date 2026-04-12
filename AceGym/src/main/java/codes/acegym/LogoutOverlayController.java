package codes.acegym;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class LogoutOverlayController {

    @FXML
    private void confirmLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
            Parent root = loader.load();

            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Stage mainStage = (Stage) currentStage.getOwner();

            // 1. Close the popup
            currentStage.close();

            // 2. CAPTURE THE STATE before swapping
            boolean wasFullScreen = mainStage.isFullScreen();
            boolean wasMaximized = mainStage.isMaximized();

            // 3. SET THE SCENE
            Scene scene = new Scene(root);
            mainStage.setScene(scene);

            // 4. APPLY THE IF-ELSE LOGIC
            if (wasFullScreen) {
                mainStage.setFullScreen(true);
            } else if (wasMaximized) {
                // Reset to normal first then maximize to clear coordinate bugs
                mainStage.setMaximized(false);
                mainStage.setMaximized(true);
            } else {
                // If it was small, keep it small and center it
                mainStage.setMaximized(false);
                mainStage.setWidth(1100);
                mainStage.setHeight(700);
                mainStage.centerOnScreen();
            }

            mainStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void cancelLogout(ActionEvent event) {
        // Just close the popup and go back to the dashboard
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
