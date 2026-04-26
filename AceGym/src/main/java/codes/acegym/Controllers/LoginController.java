package codes.acegym.Controllers;

import codes.acegym.Application_Launcher.AceGymApplication;
import codes.acegym.DB.DBConnector;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import codes.acegym.Session;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.stage.*;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Objects;
import java.util.Random;
import java.util.prefs.Preferences;

public class LoginController {

    @FXML CheckBox rememberMeCB;
    @FXML Circle logoCircle;
    @FXML ImageView logoImg;
    @FXML Circle outerCircle2;
    @FXML Circle outerCircle3;
    @FXML ImageView closeWindowIcon;
    @FXML ImageView maxMinWindow;
    @FXML ImageView minimizeWindow;
    @FXML TextField usernameField;
    @FXML PasswordField passwordField;
    @FXML private TextField textFieldPass;
    @FXML private ImageView showAndHidePass;
    @FXML Label validationError;
    @FXML StackPane mainBGID;

    // --- ANIMATION VARIABLES ---
    private AnimationTimer particleTimer;
    private ParallelTransition waveAnim2;
    private ParallelTransition waveAnim3;
    private ScaleTransition pulseLogo;
    private ScaleTransition pulseImg;

    // ── Preferences ─────────────────────────────────────────────────────────
    private final Preferences prefs = Preferences.userRoot().node(AceGymApplication.PREF_NODE);

    public void initialize() {
        rememberMeCB.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
            Node mark = rememberMeCB.lookup(".mark");
            if (mark != null && isNowSelected) {
                FadeTransition fade = new FadeTransition(Duration.millis(200), mark);
                fade.setFromValue(0);
                fade.setToValue(1);
                fade.play();
            }
        });

        waveAnim2 = createWaveAnimation(outerCircle2, 0);
        waveAnim3 = createWaveAnimation(outerCircle3, 2000);
        pulseLogo = applyHeartbeat(logoCircle);
        pulseImg  = applyHeartbeat(logoImg);
        startParticleBackground();

        // ── Pre-fill fields if Remember Me was previously checked ────────────
        boolean remembered = prefs.getBoolean(AceGymApplication.KEY_REMEMBER, false);
        if (remembered) {
            String savedUser = prefs.get(AceGymApplication.KEY_USERNAME, "");
            String savedPass = prefs.get(AceGymApplication.KEY_PASSWORD, "");
            usernameField.setText(savedUser);
            passwordField.setText(savedPass);
            textFieldPass.setText(savedPass);
            rememberMeCB.setSelected(true);
        }

        rememberMeCB.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((wObs, oldWindow, newWindow) -> {
                    if (newWindow instanceof Stage stage) {
                        addResizeListener(stage);
                        stage.iconifiedProperty().addListener((o, wasNotMin, isMin) -> {
                            if (isMin) pauseAllAnimations();
                            else resumeAllAnimations();
                        });
                    }
                });
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // LOGIN
    // ═══════════════════════════════════════════════════════════════
    @FXML
    public void handleLogin() {
        String username = usernameField.getText();
        String password = isPasswordVisible ? textFieldPass.getText() : passwordField.getText();

        usernameField.getStyleClass().remove("error");
        passwordField.getStyleClass().remove("error");
        textFieldPass.getStyleClass().remove("error");
        validationError.getStyleClass().remove("error-label-visible");

        if (username.isEmpty() || password.isEmpty()) {
            validationError.setText("⚠ Please fill in all fields!");
            validationError.getStyleClass().add("error-label-visible");
            shakeNode(usernameField);
            if (username.isEmpty()) usernameField.getStyleClass().add("error");
            if (password.isEmpty()) {
                passwordField.getStyleClass().add("error");
                textFieldPass.getStyleClass().add("error");
                shakeNode(isPasswordVisible ? textFieldPass : passwordField);
                shakeNode(textFieldPass);
                shakeNode(showAndHidePass);
            }
            return;
        }

        setInputsDisabled(true);
        validationError.setText("Logging in...");
        validationError.getStyleClass().add("error-label-visible");

        Thread loginThread = new Thread(() -> {
            boolean loginSuccess = DBConnector.login(username, password);

            if (loginSuccess) {
                try {
                    // ── Save or clear Remember Me ────────────────────────────
                    if (rememberMeCB.isSelected()) {
                        prefs.putBoolean(AceGymApplication.KEY_REMEMBER, true);
                        prefs.put(AceGymApplication.KEY_USERNAME, username);
                        prefs.put(AceGymApplication.KEY_PASSWORD, password);
                    } else {
                        prefs.putBoolean(AceGymApplication.KEY_REMEMBER, false);
                        prefs.remove(AceGymApplication.KEY_USERNAME);
                        prefs.remove(AceGymApplication.KEY_PASSWORD);
                    }

                    // ✅ Set session BEFORE loader.load()
                    Session.getInstance().setLoggedInUsername(username);

                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/codes/acegym/HomePage.fxml"));
                    Parent root = loader.load();

                    Platform.runLater(() -> {
                        stopAllAnimations();
                        Stage stage = (Stage) usernameField.getScene().getWindow();
                        stage.setScene(new Scene(root));
                        stage.show();
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        setInputsDisabled(false);
                        validationError.setText("⚠ Failed to load page. Try again.");
                    });
                }

            } else {
                // Bad credentials — clear any saved prefs so stale data doesn't linger
                prefs.putBoolean(AceGymApplication.KEY_REMEMBER, false);
                prefs.remove(AceGymApplication.KEY_USERNAME);
                prefs.remove(AceGymApplication.KEY_PASSWORD);

                Platform.runLater(() -> {
                    setInputsDisabled(false);
                    validationError.setText("⚠ Incorrect Email or Password!");
                    validationError.getStyleClass().add("error-label-visible");

                    usernameField.getStyleClass().add("error");
                    passwordField.getStyleClass().add("error");
                    textFieldPass.getStyleClass().add("error");

                    shakeNode(usernameField);
                    shakeNode(passwordField);
                    shakeNode(textFieldPass);
                    shakeNode(showAndHidePass);
                });
            }
        });

        loginThread.setDaemon(true);
        loginThread.start();
    }

    // ═══════════════════════════════════════════════════════════════
    // FORGOT PASSWORD POPUP
    // ═══════════════════════════════════════════════════════════════
    @FXML
    private void handleForgotPassword() {
        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(usernameField.getScene().getWindow());

        // ── Icon circle ───────────────────────────────────────────
        StackPane iconCircle = new StackPane();
        iconCircle.setMinSize(48, 48);
        iconCircle.setMaxSize(48, 48);
        iconCircle.setStyle(
                "-fx-background-color:#cb443e22;" +
                        "-fx-background-radius:24;" +
                        "-fx-border-color:#cb443e55;" +
                        "-fx-border-radius:24;" +
                        "-fx-border-width:1.5;");
        Label iconLbl = new Label("🔒");
        iconLbl.setStyle("-fx-font-size:20px;");
        iconCircle.getChildren().add(iconLbl);

        // ── Title + message ───────────────────────────────────────
        Label title = new Label("Forgot Password?");
        title.setStyle(
                "-fx-text-fill:white;" +
                        "-fx-font-family:'Inter';" +
                        "-fx-font-size:16px;" +
                        "-fx-font-weight:bold;");

        Label message = new Label(
                "Please contact the developer of the system\nto be assisted with your account.");
        message.setWrapText(true);
        message.setStyle(
                "-fx-text-fill:#7a7f94;" +
                        "-fx-font-family:'Inter';" +
                        "-fx-font-size:13px;" +
                        "-fx-line-spacing:3;");

        VBox textCol = new VBox(6, title, message);
        textCol.setAlignment(Pos.CENTER_LEFT);

        HBox contentRow = new HBox(16, iconCircle, textCol);
        contentRow.setAlignment(Pos.CENTER_LEFT);
        contentRow.setPadding(new Insets(24, 24, 20, 24));
        contentRow.setStyle(
                "-fx-border-color:transparent transparent #2e3349 transparent;" +
                        "-fx-border-width:0 0 1 0;");

        // ── OK button ─────────────────────────────────────────────
        String btnBase  = "-fx-background-color:#cb443e;-fx-text-fill:white;" +
                "-fx-font-family:'Inter';-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-background-radius:9;-fx-cursor:hand;";
        String btnHover = "-fx-background-color:#a83632;-fx-text-fill:white;" +
                "-fx-font-family:'Inter';-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-background-radius:9;-fx-cursor:hand;";

        Button okBtn = new Button("Got it");
        okBtn.setPrefHeight(38);
        okBtn.setPrefWidth(100);
        okBtn.setStyle(btnBase);
        okBtn.setOnMouseEntered(e -> okBtn.setStyle(btnHover));
        okBtn.setOnMouseExited(e  -> okBtn.setStyle(btnBase));
        okBtn.setOnAction(e -> dialog.close());

        HBox footer = new HBox(okBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(14, 24, 18, 24));

        // ── Root card ─────────────────────────────────────────────
        VBox root = new VBox(contentRow, footer);
        root.setStyle(
                "-fx-background-color:#1c2237;" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:#2e3349;" +
                        "-fx-border-radius:16;" +
                        "-fx-border-width:1.5;");
        root.setEffect(new DropShadow(40, Color.web("#000000", 0.85)));

        Scene scene = new Scene(root, 380, 175);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════════
    // ANIMATION CONTROLS
    // ═══════════════════════════════════════════════════════════════
    private void pauseAllAnimations() {
        if (particleTimer != null) particleTimer.stop();
        if (waveAnim2 != null) waveAnim2.pause();
        if (waveAnim3 != null) waveAnim3.pause();
        if (pulseLogo != null) pulseLogo.pause();
        if (pulseImg  != null) pulseImg.pause();
    }

    private void resumeAllAnimations() {
        if (particleTimer != null) particleTimer.start();
        if (waveAnim2 != null) waveAnim2.play();
        if (waveAnim3 != null) waveAnim3.play();
        if (pulseLogo != null) pulseLogo.play();
        if (pulseImg  != null) pulseImg.play();
    }

    private void stopAllAnimations() {
        if (particleTimer != null) particleTimer.stop();
        if (waveAnim2 != null) waveAnim2.stop();
        if (waveAnim3 != null) waveAnim3.stop();
        if (pulseLogo != null) pulseLogo.stop();
        if (pulseImg  != null) pulseImg.stop();
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════
    private void setInputsDisabled(boolean disabled) {
        usernameField.setDisable(disabled);
        passwordField.setDisable(disabled);
        textFieldPass.setDisable(disabled);
        showAndHidePass.setDisable(disabled);
    }

    Image eyeOpen   = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/eye-solid.png")));
    Image eyeClosed = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/eye-slash-solid.png")));
    boolean isPasswordVisible = false;

    @FXML
    private void togglePassword(MouseEvent event) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200));
        FadeTransition fadeIn  = new FadeTransition(Duration.millis(200));
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        if (isPasswordVisible) {
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
        fadeOut.play();
    }

    @FXML
    private void shakeNode(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(0);
        tt.setByX(10);
        tt.setCycleCount(4);
        tt.setAutoReverse(true);
        tt.play();
    }

    @FXML private void handleClose() { System.exit(0); }

    @FXML
    private void handleMinimize(MouseEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void handleMaxMin(MouseEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        if (stage.isMaximized()) {
            stage.setMaximized(false);
            stage.setWidth(1200);
            stage.setHeight(800);
            stage.centerOnScreen();
        } else {
            stage.setMaximized(true);
        }
    }

    private double x = 0, y = 0;

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
        double border = 10;
        stage.getScene().setOnMouseMoved(e -> {
            double x = e.getX(), y = e.getY();
            double w = stage.getWidth(), h = stage.getHeight();
            Cursor cursor = Cursor.DEFAULT;
            if      (x < border && y < border)         cursor = Cursor.NW_RESIZE;
            else if (x < border && y > h - border)     cursor = Cursor.SW_RESIZE;
            else if (x > w - border && y < border)     cursor = Cursor.NE_RESIZE;
            else if (x > w - border && y > h - border) cursor = Cursor.SE_RESIZE;
            else if (x < border)                       cursor = Cursor.W_RESIZE;
            else if (x > w - border)                   cursor = Cursor.E_RESIZE;
            else if (y < border)                       cursor = Cursor.N_RESIZE;
            else if (y > h - border)                   cursor = Cursor.S_RESIZE;
            stage.getScene().setCursor(cursor);
        });

        stage.getScene().setOnMouseDragged(e -> {
            if (stage.isMaximized()) return;
            double x = e.getScreenX(), y = e.getScreenY();
            Cursor cursor = stage.getScene().getCursor();
            if (cursor == Cursor.E_RESIZE  || cursor == Cursor.SE_RESIZE || cursor == Cursor.NE_RESIZE)
                stage.setWidth(x - stage.getX());
            if (cursor == Cursor.S_RESIZE  || cursor == Cursor.SE_RESIZE || cursor == Cursor.SW_RESIZE)
                stage.setHeight(y - stage.getY());
            if (cursor == Cursor.W_RESIZE  || cursor == Cursor.NW_RESIZE || cursor == Cursor.SW_RESIZE) {
                double newWidth = stage.getX() + stage.getWidth() - x;
                if (newWidth > stage.getMinWidth()) { stage.setX(x); stage.setWidth(newWidth); }
            }
            if (cursor == Cursor.N_RESIZE  || cursor == Cursor.NW_RESIZE || cursor == Cursor.NE_RESIZE) {
                double newHeight = stage.getY() + stage.getHeight() - y;
                if (newHeight > stage.getMinHeight()) { stage.setY(y); stage.setHeight(newHeight); }
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // BACKGROUND PARTICLES
    // ═══════════════════════════════════════════════════════════════
    private void startParticleBackground() {
        Canvas bgCanvas = new Canvas();
        bgCanvas.widthProperty().bind(mainBGID.widthProperty());
        bgCanvas.heightProperty().bind(mainBGID.heightProperty());
        mainBGID.getChildren().add(0, bgCanvas);

        GraphicsContext gc = bgCanvas.getGraphicsContext2D();

        int COUNT = 25;
        double[] px = new double[COUNT], py = new double[COUNT], pr = new double[COUNT];
        double[] pa = new double[COUNT], pd = new double[COUNT], pspd = new double[COUNT];
        double[] popa = new double[COUNT], ppls = new double[COUNT], pplsS = new double[COUNT];
        double[] pglw = new double[COUNT];
        int[][] pcol = new int[COUNT][3];

        int[][] palette = {
                {203, 68, 62}, {139, 45, 47}, {180, 50, 40}, {220, 80, 60}, {100, 30, 35}
        };

        Random rng = new Random();
        for (int i = 0; i < COUNT; i++) {
            px[i] = rng.nextDouble(); py[i] = rng.nextDouble();
            pr[i] = 2 + rng.nextDouble() * 3.5;
            pa[i] = rng.nextDouble() * Math.PI * 2;
            pd[i] = (rng.nextDouble() - 0.5) * 0.0006;
            pspd[i] = 0.00008 + rng.nextDouble() * 0.00014;
            popa[i] = 0.12 + rng.nextDouble() * 0.43;
            ppls[i] = rng.nextDouble() * Math.PI * 2;
            pplsS[i] = 0.008 + rng.nextDouble() * 0.017;
            pglw[i] = 18 + rng.nextDouble() * 34;
            pcol[i] = palette[rng.nextInt(palette.length)];
        }

        int BLOBS = 5;
        double[] bx = new double[BLOBS], by = new double[BLOBS], br = new double[BLOBS];
        double[] ba = new double[BLOBS], bd = new double[BLOBS], bspd = new double[BLOBS];
        double[] bopa = new double[BLOBS];
        int[][] bcol = new int[BLOBS][3];

        for (int i = 0; i < BLOBS; i++) {
            bx[i] = 0.05 + rng.nextDouble() * 0.9;
            by[i] = 0.05 + rng.nextDouble() * 0.9;
            br[i] = 60 + rng.nextDouble() * 70;
            ba[i] = rng.nextDouble() * Math.PI * 2;
            bd[i] = (rng.nextDouble() - 0.5) * 0.0002;
            bspd[i] = 0.00004 + rng.nextDouble() * 0.00006;
            bopa[i] = 0.04 + rng.nextDouble() * 0.06;
            bcol[i] = palette[rng.nextInt(palette.length)];
        }

        particleTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double W = bgCanvas.getWidth(), H = bgCanvas.getHeight();
                if (W == 0 || H == 0) return;

                gc.setFill(Color.web("#0D1117"));
                gc.fillRect(0, 0, W, H);

                for (int i = 0; i < BLOBS; i++) {
                    ba[i] += bd[i];
                    bx[i] += Math.cos(ba[i]) * bspd[i];
                    by[i] += Math.sin(ba[i]) * bspd[i];
                    if (bx[i] < 0) bx[i] = 1; if (bx[i] > 1) bx[i] = 0;
                    if (by[i] < 0) by[i] = 1; if (by[i] > 1) by[i] = 0;
                    double cx = bx[i] * W, cy = by[i] * H, rr = br[i];
                    gc.setFill(new RadialGradient(0, 0, cx, cy, rr, false, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.rgb(bcol[i][0], bcol[i][1], bcol[i][2], bopa[i])),
                            new Stop(1, Color.TRANSPARENT)));
                    gc.fillOval(cx - rr, cy - rr, rr * 2, rr * 2);
                }

                for (int i = 0; i < COUNT; i++) {
                    pa[i] += pd[i];
                    px[i] += Math.cos(pa[i]) * pspd[i];
                    py[i] += Math.sin(pa[i]) * pspd[i];
                    if (px[i] < -0.05) px[i] = 1.05; if (px[i] > 1.05) px[i] = -0.05;
                    if (py[i] < -0.05) py[i] = 1.05; if (py[i] > 1.05) py[i] = -0.05;
                    ppls[i] += pplsS[i];
                    double pulse = 0.8 + 0.2 * Math.sin(ppls[i]);
                    double alpha = popa[i] * pulse;
                    double cx = px[i] * W, cy = py[i] * H, gr = pglw[i];
                    gc.setFill(new RadialGradient(0, 0, cx, cy, gr, false, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.rgb(pcol[i][0], pcol[i][1], pcol[i][2], alpha * 0.35)),
                            new Stop(1, Color.TRANSPARENT)));
                    gc.fillOval(cx - gr, cy - gr, gr * 2, gr * 2);
                    double r = pr[i] * pulse;
                    gc.setFill(Color.rgb(pcol[i][0], pcol[i][1], pcol[i][2], alpha));
                    gc.fillOval(cx - r, cy - r, r * 2, r * 2);
                }
            }
        };
        particleTimer.start();
    }

    // ═══════════════════════════════════════════════════════════════
    // WAVE + HEARTBEAT
    // ═══════════════════════════════════════════════════════════════
    private ParallelTransition createWaveAnimation(Circle circle, double delayMs) {
        if (circle == null) return null;
        Duration speed = Duration.millis(4000);
        ScaleTransition scale = new ScaleTransition(speed, circle);
        scale.setFromX(1.0); scale.setFromY(1.0);
        scale.setToX(1.4);   scale.setToY(1.4);
        scale.setInterpolator(Interpolator.LINEAR);
        FadeTransition fade = new FadeTransition(speed, circle);
        fade.setFromValue(0.4); fade.setToValue(0.0);
        fade.setInterpolator(Interpolator.LINEAR);
        ParallelTransition wave = new ParallelTransition(circle, scale, fade);
        wave.setCycleCount(Animation.INDEFINITE);
        wave.setDelay(Duration.millis(delayMs));
        wave.play();
        return wave;
    }

    private ScaleTransition applyHeartbeat(Node target) {
        if (target == null) return null;
        ScaleTransition pulse = new ScaleTransition(Duration.millis(2000), target);
        pulse.setByX(0.05); pulse.setByY(0.05);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.setInterpolator(Interpolator.EASE_BOTH);
        pulse.play();
        return pulse;
    }
}