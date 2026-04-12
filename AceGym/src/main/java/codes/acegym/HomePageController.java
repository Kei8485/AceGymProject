package codes.acegym;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class HomePageController {

    @FXML
    private ToggleButton CoachList;

    @FXML
    private ToggleButton ReportForm;

    @FXML
    private ToggleButton MembersForm;

    @FXML
    private ToggleButton PaymentForm;

    @FXML
    private ToggleButton planForm;

    @FXML
    private ToggleButton registrationForm;

    @FXML
    private ToggleButton adminProfile;

    @FXML
    private ToggleButton dashboardBtn;

    // FIXED: Correct fx:id from FXML (logoutButton)
    @FXML
    private Button logoutButton;

    @FXML
    private VBox contentArea;

    @FXML
    private ToggleGroup menuGroup;

    @FXML
    private void initialize() {
        menuGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                oldToggle.setSelected(true);
            }
        });
        loadPage("/codes/acegym/Dashboard.fxml");
        setupNavigation();
    }

    private void loadPage(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            VBox.setVgrow(view, Priority.ALWAYS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupNavigation() {
        dashboardBtn.setOnAction(e -> loadPage("/codes/acegym/Dashboard.fxml"));
        adminProfile.setOnAction(e -> loadPage("/codes/acegym/AdminProfile.fxml"));

    }

    // FIXED: Proper logout method
    @FXML
    private void Logout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) logoutButton.getScene().getWindow();

            stage.setScene(new Scene(root));

            // 🔥 FIX HERE
            stage.setIconified(false);   // restore if minimized
            stage.setFullScreenExitHint("");
            stage.setFullScreen(true);    // then maximize


            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose() {
        System.exit(0);
    }

    @FXML
    private void handleMinimize(MouseEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void handleMaxMin(MouseEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

    private double x = 0;
    private double y = 0;

    @FXML
    private void handleTitleBarDragged(MouseEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setX(event.getScreenX() - x);
        stage.setY(event.getScreenY() - y);
    }

    @FXML
    private void handleTitleBarPressed(MouseEvent event) {
        x = event.getSceneX();
        y = event.getSceneY();
    }
}