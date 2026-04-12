package codes.acegym;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Objects;
import java.util.Random;

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
    private TextField textFieldPass;

    @FXML
    private ImageView showAndHidePass;

    @FXML
    Label validationError;

    @FXML
    StackPane mainBGID;







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

//        bg animation
        startParticleBackground();

        Platform.runLater(() -> {
            Stage stage = (Stage) rememberMeCB.getScene().getWindow();
            addResizeListener(stage);
        });

    }




    private void startParticleBackground() {
        Canvas bgCanvas = new Canvas();
        bgCanvas.widthProperty().bind(mainBGID.widthProperty());
        bgCanvas.heightProperty().bind(mainBGID.heightProperty());
        mainBGID.getChildren().add(0, bgCanvas);

        GraphicsContext gc = bgCanvas.getGraphicsContext2D();
        long startTime = System.nanoTime();

        int COUNT = 25;
        double[] px   = new double[COUNT];
        double[] py   = new double[COUNT];
        double[] pr   = new double[COUNT];
        double[] pa   = new double[COUNT];  // angle
        double[] pd   = new double[COUNT];  // drift
        double[] pspd = new double[COUNT];
        double[] popa = new double[COUNT];  // base opacity
        double[] ppls = new double[COUNT];  // pulse phase
        double[] pplsS= new double[COUNT];  // pulse speed
        double[] pglw = new double[COUNT];  // glow radius
        int[][]  pcol = new int[COUNT][3];

        int[][] palette = {
                {203, 68, 62},
                {139, 45, 47},
                {180, 50, 40},
                {220, 80, 60},
                {100, 30, 35}
        };

        Random rng = new Random();
        for (int i = 0; i < COUNT; i++) {
            px[i]   = rng.nextDouble();
            py[i]   = rng.nextDouble();
            pr[i]   = 2 + rng.nextDouble() * 3.5;
            pa[i]   = rng.nextDouble() * Math.PI * 2;
            pd[i]   = (rng.nextDouble() - 0.5) * 0.0006;
            pspd[i] = 0.00008 + rng.nextDouble() * 0.00014;
            popa[i] = 0.12 + rng.nextDouble() * 0.43;
            ppls[i] = rng.nextDouble() * Math.PI * 2;
            pplsS[i]= 0.008 + rng.nextDouble() * 0.017;
            pglw[i] = 18 + rng.nextDouble() * 34;
            pcol[i] = palette[rng.nextInt(palette.length)];
        }

        // Ambient blobs
        int BLOBS = 5;
        double[] bx   = new double[BLOBS];
        double[] by   = new double[BLOBS];
        double[] br   = new double[BLOBS];
        double[] ba   = new double[BLOBS];
        double[] bd   = new double[BLOBS];
        double[] bspd = new double[BLOBS];
        double[] bopa = new double[BLOBS];
        int[][]  bcol = new int[BLOBS][3];

        for (int i = 0; i < BLOBS; i++) {
            bx[i]   = 0.05 + rng.nextDouble() * 0.9;
            by[i]   = 0.05 + rng.nextDouble() * 0.9;
            br[i]   = 60 + rng.nextDouble() * 70;
            ba[i]   = rng.nextDouble() * Math.PI * 2;
            bd[i]   = (rng.nextDouble() - 0.5) * 0.0002;
            bspd[i] = 0.00004 + rng.nextDouble() * 0.00006;
            bopa[i] = 0.04 + rng.nextDouble() * 0.06;
            bcol[i] = palette[rng.nextInt(palette.length)];
        }

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                double W = bgCanvas.getWidth();
                double H = bgCanvas.getHeight();
                if (W == 0 || H == 0) return;

                gc.setFill(Color.web("#0D1117"));
                gc.fillRect(0, 0, W, H);

                // Ambient blobs
                for (int i = 0; i < BLOBS; i++) {
                    ba[i] += bd[i];
                    bx[i] += Math.cos(ba[i]) * bspd[i];
                    by[i] += Math.sin(ba[i]) * bspd[i];
                    if (bx[i] < 0) bx[i] = 1; if (bx[i] > 1) bx[i] = 0;
                    if (by[i] < 0) by[i] = 1; if (by[i] > 1) by[i] = 0;

                    double cx = bx[i] * W, cy = by[i] * H, rr = br[i];
                    RadialGradient blob = new RadialGradient(0, 0, cx, cy, rr, false,
                            CycleMethod.NO_CYCLE,
                            new Stop(0, Color.rgb(bcol[i][0], bcol[i][1], bcol[i][2], bopa[i])),
                            new Stop(1, Color.TRANSPARENT));
                    gc.setFill(blob);
                    gc.fillOval(cx - rr, cy - rr, rr * 2, rr * 2);
                }

                // Particles
                double t = (now - startTime) / 1_000_000_000.0;
                for (int i = 0; i < COUNT; i++) {
                    pa[i] += pd[i];
                    px[i] += Math.cos(pa[i]) * pspd[i];
                    py[i] += Math.sin(pa[i]) * pspd[i];
                    if (px[i] < -0.05) px[i] = 1.05;
                    if (px[i] > 1.05)  px[i] = -0.05;
                    if (py[i] < -0.05) py[i] = 1.05;
                    if (py[i] > 1.05)  py[i] = -0.05;

                    ppls[i] += pplsS[i];
                    double pulse = 0.8 + 0.2 * Math.sin(ppls[i]);
                    double alpha = popa[i] * pulse;
                    double cx = px[i] * W, cy = py[i] * H;
                    double gr = pglw[i];

                    // Glow halo
                    RadialGradient glow = new RadialGradient(0, 0, cx, cy, gr, false,
                            CycleMethod.NO_CYCLE,
                            new Stop(0, Color.rgb(pcol[i][0], pcol[i][1], pcol[i][2], alpha * 0.35)),
                            new Stop(1, Color.TRANSPARENT));
                    gc.setFill(glow);
                    gc.fillOval(cx - gr, cy - gr, gr * 2, gr * 2);

                    // Core dot
                    double r = pr[i] * pulse;
                    gc.setFill(Color.rgb(pcol[i][0], pcol[i][1], pcol[i][2], alpha));
                    gc.fillOval(cx - r, cy - r, r * 2, r * 2);
                }
            }
        }.start();
    }


    Image eyeOpen = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/eye-solid.png")));
    Image eyeClosed = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/eye-slash-solid.png")));
    boolean isPasswordVisible = false;

    @FXML
    private void togglePassword(MouseEvent event) {
        // 1. Create the fade settings (200ms is a good "fast but smooth" speed)
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200));
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200));

        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        if (isPasswordVisible) {
            // --- Switching to Hidden (Dots) ---
            fadeOut.setNode(textFieldPass);
            fadeIn.setNode(passwordField);

            passwordField.setText(textFieldPass.getText());

            fadeOut.setOnFinished(e -> {
                textFieldPass.setVisible(false);
                passwordField.setVisible(true);
                fadeIn.play();
            });

            showAndHidePass.setImage(eyeClosed);
            isPasswordVisible = false;

        } else {
            // --- Switching to Visible (Plain Text) ---
            fadeOut.setNode(passwordField);
            fadeIn.setNode(textFieldPass);

            textFieldPass.setText(passwordField.getText());

            fadeOut.setOnFinished(e -> {
                passwordField.setVisible(false);
                textFieldPass.setVisible(true);
                fadeIn.play();
            });

            showAndHidePass.setImage(eyeOpen);
            isPasswordVisible = true;
        }

        // Start the animation
        fadeOut.play();
    }



    @FXML
    public void handleLogin() {
        String username = usernameField.getText();

        // 1. Get the password from the field that is currently visible
        String password = isPasswordVisible ? textFieldPass.getText() : passwordField.getText();

        // --- Reset previous error styles for ALL fields ---
        usernameField.getStyleClass().remove("error");
        passwordField.getStyleClass().remove("error");
        textFieldPass.getStyleClass().remove("error");
        validationError.getStyleClass().remove("error-label-visible");

        // --- Check empty fields ---
        if (username.isEmpty() || password.isEmpty()) {
            validationError.setText("⚠ Please fill in all fields!");
            validationError.getStyleClass().add("error-label-visible");
            shakeNode(usernameField);

            if (username.isEmpty()) {
                usernameField.getStyleClass().add("error");
                shakeNode(usernameField);
            }

            if (password.isEmpty()) {
                // Apply error style to both so the red border stays if you toggle the eye
                passwordField.getStyleClass().add("error");
                textFieldPass.getStyleClass().add("error");
                shakeNode(usernameField);

                // SHAKE the one the user is currently looking at
                shakeNode(isPasswordVisible ? textFieldPass : passwordField);
                shakeNode(textFieldPass);
                shakeNode(showAndHidePass);


            }
            return;
        }

        // --- Attempt login ---
        boolean loginSuccess = DBConnector.login(username, password);

        if (loginSuccess) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/codes/acegym/HomePage.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) usernameField.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setIconified(false);
                stage.setMaximized(true);
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

            // Apply error style to both password fields
            passwordField.getStyleClass().add("error");
            textFieldPass.getStyleClass().add("error");

            shakeNode(textFieldPass);
            shakeNode(passwordField);
            shakeNode(usernameField);
            shakeNode(showAndHidePass);
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

    public void addResizeListener(Stage stage) {
        double border = 10; // The "grab" area in pixels

        stage.getScene().setOnMouseMoved(e -> {
            double x = e.getX(), y = e.getY();
            double w = stage.getWidth(), h = stage.getHeight();
            Cursor cursor = Cursor.DEFAULT;

            if (x < border && y < border) cursor = Cursor.NW_RESIZE;
            else if (x < border && y > h - border) cursor = Cursor.SW_RESIZE;
            else if (x > w - border && y < border) cursor = Cursor.NE_RESIZE;
            else if (x > w - border && y > h - border) cursor = Cursor.SE_RESIZE;
            else if (x < border) cursor = Cursor.W_RESIZE;
            else if (x > w - border) cursor = Cursor.E_RESIZE;
            else if (y < border) cursor = Cursor.N_RESIZE;
            else if (y > h - border) cursor = Cursor.S_RESIZE;

            stage.getScene().setCursor(cursor);
        });

        stage.getScene().setOnMouseDragged(e -> {
            double x = e.getScreenX(), y = e.getScreenY();
            // Simple logic for South-East (bottom-right) dragging
            if (stage.getScene().getCursor() == Cursor.SE_RESIZE) {
                stage.setWidth(x - stage.getX());
                stage.setHeight(y - stage.getY());
            }
            // You can add logic for other directions here similarly
        });
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
