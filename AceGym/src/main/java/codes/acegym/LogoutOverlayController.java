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
            // Load the login page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
            Parent root = loader.load();

            // 1. Get the current popup stage and close it
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();

            // 2. Get the main dashboard window (the owner)
            Stage mainStage = (Stage) currentStage.getOwner();

            // 3. Set the new scene
            Scene scene = new Scene(root);
            mainStage.setScene(scene);

            // --- YOUR FULL SCREEN LOGIC ---
            mainStage.setIconified(false);        // Restore if minimized
            mainStage.setFullScreenExitHint("");  // Remove the "Press ESC to exit" message
            mainStage.setFullScreen(true);         // Set to true full screen mode

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
