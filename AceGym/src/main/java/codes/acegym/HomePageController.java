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

    @FXML private ToggleButton CoachList;
    @FXML private ToggleButton ReportForm;
    @FXML private ToggleButton MembersForm;
    @FXML private ToggleButton PaymentForm;
    @FXML private ToggleButton planForm;
    @FXML private ToggleButton registrationForm;
    @FXML private ToggleButton adminProfile;
    @FXML private ToggleButton dashboardBtn;

    @FXML private Button logoutButton;
    @FXML private VBox contentArea;
    @FXML private ToggleGroup menuGroup;

    // 🔥 Store drag offset
    private double x = 0;
    private double y = 0;

    // 🔥 SAFE way to get Stage (prevents crashes)
    private Stage getStage(Object source) {
        try {
            return (Stage) ((Node) source).getScene().getWindow();
        } catch (Exception e) {
            System.out.println("Error getting stage: " + e.getMessage());
            return null;
        }
    }

    @FXML
    private void initialize() {

        // 🔥 Prevent no selection in toggle group
        menuGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                oldToggle.setSelected(true);
            }
        });

        // 🔥 Load default page
        loadPage("/codes/acegym/Dashboard.fxml");

        setupNavigation();
    }

    // 🔥 LOAD PAGE (optimized + safe)
    private void loadPage(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));

            if (loader.getLocation() == null) {
                throw new IOException("FXML file not found: " + fxmlFile);
            }

            Parent view = loader.load();

            // Replace content safely
            contentArea.getChildren().setAll(view);

            // Make it responsive
            VBox.setVgrow(view, Priority.ALWAYS);

        } catch (IOException e) {
            System.out.println("Error loading page: " + fxmlFile);
            e.printStackTrace();
        }
    }

    // 🔥 NAVIGATION
    private void setupNavigation() {
        dashboardBtn.setOnAction(e -> loadPage("/codes/acegym/Dashboard.fxml"));
        adminProfile.setOnAction(e -> loadPage("/codes/acegym/AdminProfile.fxml"));

        // (You can continue adding others here)
    }

    // 🔥 LOGOUT
    @FXML
    private void Logout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));

            if (loader.getLocation() == null) {
                throw new IOException("login.fxml not found");
            }

            Parent root = loader.load();

            Stage stage = getStage(logoutButton);
            if (stage == null) return;

            stage.setScene(new Scene(root));

            stage.setMaximized(true);
            stage.setFullScreenExitHint("");
            stage.setFullScreen(true);

            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 🔥 CLOSE
    @FXML
    private void handleClose(MouseEvent event) {
        Stage stage = getStage(event.getSource());
        if (stage != null) {
            stage.close(); // better than System.exit(0)
        }
    }

    // 🔥 MINIMIZE
    @FXML
    private void handleMinimize(MouseEvent event) {
        Stage stage = getStage(event.getSource());
        if (stage != null) {
            stage.setIconified(true);
        }
    }

    // 🔥 MAX / RESTORE
    @FXML
    private void handleMaxMin(MouseEvent event) {
        Stage stage = getStage(event.getSource());
        if (stage != null) {
            stage.setMaximized(!stage.isMaximized());
        }
    }

    @FXML
    private void handleTitleBarPressed(MouseEvent event) {
        Stage stage = getStage(event.getSource());
        if (stage != null) {
            x = event.getSceneX();
            y = event.getSceneY();
        }
    }

    @FXML
    private void handleTitleBarDragged(MouseEvent event) {
        Stage stage = getStage(event.getSource());

        // ❗ IMPORTANT: disable drag when maximized
        if (stage != null && !stage.isMaximized()) {
            stage.setX(event.getScreenX() - x);
            stage.setY(event.getScreenY() - y);
        }
    }
}