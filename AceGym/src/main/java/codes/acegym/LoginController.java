package codes.acegym;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Duration;

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
