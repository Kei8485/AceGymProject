package codes.acegym;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class LoginController {

    @FXML
    CheckBox rememberMeCB;

    @FXML
    Circle logoCircle;

    @FXML
    ImageView logoImg;

    @FXML
    Circle outerCircle2;

    @FXML
    Circle outerCircle3;

    @FXML
    ImageView closeWindowIcon;

    @FXML
    ImageView maxMinWindow;

    @FXML
    ImageView minimizeWindow;

    @FXML
    TextField usernameField;

    @FXML
    PasswordField passwordField;

    @FXML
    Label validationError;

    @FXML
    public void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // --- Reset previous error styles ---
        usernameField.getStyleClass().remove("error");
        passwordField.getStyleClass().remove("error");
        validationError.getStyleClass().remove("error-label-visible");

        // --- Check empty fields ---
        if (username.isEmpty() || password.isEmpty()) {
            validationError.setText("⚠ Please fill in all fields!");
            validationError.getStyleClass().add("error-label-visible");

            if (username.isEmpty()) {
                usernameField.getStyleClass().add("error");
                shakeNode(usernameField);
            }
            if (password.isEmpty()) {
                passwordField.getStyleClass().add("error");
                shakeNode(passwordField);
            }
            return;
        }

        // --- Attempt login ---
        boolean loginSuccess = DBConnector.login(username, password);

        if (loginSuccess) {
            try {
                // Load dashboard FXML
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/codes/acegym/HomePage.fxml")
                );
                Parent root = loader.load();

                // Get the current stage
                Stage stage = (Stage) usernameField.getScene().getWindow();

                // Set the new scene directly
                Scene scene = new Scene(root);
                stage.setScene(scene);

            } catch (IOException e) {
                e.printStackTrace();
                validationError.setText("⚠ Failed to load dashboard!");
                validationError.getStyleClass().add("error-label-visible");
            }
        } else {
            // --- Invalid credentials ---
            validationError.setText("⚠ Incorrect Email or Password!");
            validationError.getStyleClass().add("error-label-visible");
            usernameField.getStyleClass().add("error");
            shakeNode(usernameField);
            passwordField.getStyleClass().add("error");
            shakeNode(passwordField);
        }
    }

    @FXML
    private void shakeNode(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(0);
        tt.setByX(10);          // move right
        tt.setCycleCount(4);    // 3 back-and-forth shakes
        tt.setAutoReverse(true);
        tt.play();
    }

    public void initialize() {
        // This listener runs every time the checkbox is clicked
        rememberMeCB.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
            // Find the checkmark inside the checkbox
            Node mark = rememberMeCB.lookup(".mark");

            if (mark != null && isNowSelected) {
                FadeTransition fade = new FadeTransition(Duration.millis(200), mark);
                fade.setFromValue(0);
                fade.setToValue(1);
                fade.play();
            }
        });

//        circle animation
        createWaveAnimation(outerCircle2, 0);
        createWaveAnimation(outerCircle3, 2000);

//        logo animation
        applyHeartbeat(logoCircle);
        applyHeartbeat(logoImg);


    }


//    window btn

    @FXML
    private void handleClose() {
        System.exit(0);
    }

    @FXML
    private void handleMinimize(MouseEvent event) {
        // Gets the current window and minimizes it to the taskbar
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void handleMaxMin(MouseEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        // Toggles between Maximized and Normal size
        if (stage.isMaximized()) {
            stage.setMaximized(false);
        } else {
            stage.setMaximized(true);
        }
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


    private void createWaveAnimation(Circle circle, double delayMs) {
        if (circle == null) return;

        Duration speed = Duration.millis(4000);

        ScaleTransition scale = new ScaleTransition(speed, circle);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.4);
        scale.setToY(1.4);
        // Linear makes the expansion constant and "smooth"
        scale.setInterpolator(Interpolator.LINEAR);

        FadeTransition fade = new FadeTransition(speed, circle);
        fade.setFromValue(0.4);
        fade.setToValue(0.0);
        fade.setInterpolator(Interpolator.LINEAR);

        ParallelTransition wave = new ParallelTransition(circle, scale, fade);
        wave.setCycleCount(Animation.INDEFINITE);
        wave.setDelay(Duration.millis(delayMs));

        wave.play();
    }


    private void applyHeartbeat(Node target) {
        if (target == null) return;

        ScaleTransition pulse = new ScaleTransition(Duration.millis(2000), target);
        pulse.setByX(0.05); // Grows by 5%
        pulse.setByY(0.05);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);

        // EASE_BOTH makes it feel like a natural "breathing" rhythm
        pulse.setInterpolator(Interpolator.EASE_BOTH);

        pulse.play();
    }


}
