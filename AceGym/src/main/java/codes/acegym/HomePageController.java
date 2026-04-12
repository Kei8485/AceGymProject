package codes.acegym;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;

public class HomePageController {

    @FXML
    private ToggleButton CoachList;

    @FXML
    ImageView closeWindowIcon;

    @FXML
    ImageView maxMinWindow;

    @FXML
    ImageView minimizeWindow;

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
    private StackPane homePageBgID;

    @FXML
    private void initialize() {
        menuGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                oldToggle.setSelected(true);
            }
        });

        loadPage("/codes/acegym/Dashboard.fxml");
        setupNavigation();
        setDashboardBackground();

        // Use Platform.runLater to wait for the window to actually exist
        Platform.runLater(() -> {
            // Get the stage using the scene from ANY node in this FXML
            // I'm using the first toggle in your menuGroup as an example
            if (!menuGroup.getToggles().isEmpty()) {
                Node node = (Node) menuGroup.getToggles().get(0);
                Stage stage = (Stage) node.getScene().getWindow();
                addResizeListener(stage);
            }
        });
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



    private AnimationTimer glowTimer;

    private void setDashboardBackground() {
        homePageBgID.setStyle("-fx-background-color: #0D1117;");

        Canvas canvas = new Canvas();
        canvas.setMouseTransparent(true);

        homePageBgID.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            canvas.setWidth(newBounds.getWidth());
            canvas.setHeight(newBounds.getHeight());
        });

        homePageBgID.getChildren().add(0, canvas);

        Platform.runLater(() -> {
            canvas.setWidth(homePageBgID.getWidth());
            canvas.setHeight(homePageBgID.getHeight());
            startGlowPulse(canvas);
        });
    }

    private void dashboardMaxMin(MouseEvent event){
        System.out.println("Hello");
    }

    private void startGlowPulse(Canvas canvas) {
        if (glowTimer != null) {
            glowTimer.stop();
        }

        long[] startTime = { System.nanoTime() };

        glowTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double W = canvas.getWidth();
                double H = canvas.getHeight();
                if (W <= 0 || H <= 0) return;

                double t = (now - startTime[0]) / 1_000_000_000.0;

                double pulseBL = Math.min(1.0, Math.max(0.0, 0.10 + 0.05 * (0.5 + 0.5 * Math.sin(t * 0.6))));
                double pulseTR = Math.min(1.0, Math.max(0.0, 0.07 + 0.05 * (0.5 + 0.5 * Math.sin(t * 0.6 + Math.PI * 0.7))));

                GraphicsContext gc = canvas.getGraphicsContext2D();

                gc.setFill(Color.web("#0D1117"));
                gc.fillRect(0, 0, W, H);

                RadialGradient glowBL = new RadialGradient(
                        0, 0,
                        0, H,
                        W * 0.85,
                        false,
                        CycleMethod.NO_CYCLE,
                        new Stop(0.0, Color.web("#CB443E", pulseBL)),
                        new Stop(0.4, Color.web("#CB443E", pulseBL * 0.5)),
                        new Stop(1.0, Color.TRANSPARENT)
                );
                gc.setFill(glowBL);
                gc.fillRect(0, 0, W, H);

                RadialGradient glowTR = new RadialGradient(
                        0, 0,
                        W, 0,
                        W * 0.65,
                        false,
                        CycleMethod.NO_CYCLE,
                        new Stop(0.0, Color.web("#8B2D2F", pulseTR)),
                        new Stop(0.4, Color.web("#8B2D2F", pulseTR * 0.4)),
                        new Stop(1.0, Color.TRANSPARENT)
                );
                gc.setFill(glowTR);
                gc.fillRect(0, 0, W, H);
            }
        };

        glowTimer.start();
    }


    private void loadPage(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent view = loader.load();

            // 1. Prepare the new view (start invisible and slightly shifted)
            view.setOpacity(0);
            view.setTranslateY(10); // Subtle "slide up" effect

            contentArea.getChildren().setAll(view);
            VBox.setVgrow(view, Priority.ALWAYS);

            // 2. Create the Fade Animation
            FadeTransition fadeIn = new FadeTransition(Duration.millis(700), view);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            // 3. Create the Slide Animation
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(700), view);
            slideIn.setFromY(10);
            slideIn.setToY(0);

            // 4. Play them together
            ParallelTransition parallelTransition = new ParallelTransition(fadeIn, slideIn);
            parallelTransition.play();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupNavigation() {
        // 1. Group all buttons into an array for cleaner code
        ToggleButton[] menuButtons = {
                dashboardBtn, adminProfile, registrationForm, planForm,
                PaymentForm, MembersForm, ReportForm, CoachList
        };

        // 2. Apply your preferred 200ms animation to all
        for (ToggleButton btn : menuButtons) {
            addHoverAnimation(btn);
        }

        // 3. Set actions with a "check if already selected" guard
        dashboardBtn.setOnAction(e -> handleNavigation(dashboardBtn, "/codes/acegym/Dashboard.fxml"));
        adminProfile.setOnAction(e -> handleNavigation(adminProfile, "/codes/acegym/AdminProfile.fxml"));
        // Add the rest of your buttons here following the same pattern...
    }

    /**
     * Custom helper to prevent reloading the same page
     */
    private void handleNavigation(ToggleButton button, String fxmlPath) {
        // If the button was already selected, do nothing
        // This prevents the "flash" or "double-load" of the FXML
        if (button.isSelected() && contentArea.getUserData() != null && contentArea.getUserData().equals(fxmlPath)) {
            return;
        }

        // Store the current path so we can check it next time
        contentArea.setUserData(fxmlPath);
        loadPage(fxmlPath);
    }

    private void addHoverAnimation(ToggleButton button) {
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(200), button);
        scaleUp.setToX(1.05);
        scaleUp.setToY(1.05);

        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(200), button);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);

        button.setOnMouseEntered(e -> scaleUp.playFromStart());
        button.setOnMouseExited(e -> scaleDown.playFromStart());
    }

    // FIXED: Proper logout method
    @FXML
    private void handleLogoutClick(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("LogoutOverlay.fxml"));
            Parent root = loader.load();

            Stage confirmStage = new Stage();
            confirmStage.initStyle(StageStyle.TRANSPARENT);
            confirmStage.initModality(Modality.APPLICATION_MODAL);

            Stage owner = (Stage) logoutButton.getScene().getWindow();
            confirmStage.initOwner(owner);

            Scene scene = new Scene(root);
            scene.setFill(null);
            confirmStage.setScene(scene);

            // 1. ADD BLUR TO BACKGROUND
            ColorAdjust dim = new ColorAdjust();
            dim.setBrightness(-0.2); // Slightly darken the bg
            GaussianBlur blur = new GaussianBlur(10); // 10 is the blur strength
            owner.getScene().getRoot().setEffect(blur);

            // 2. CENTERING
            confirmStage.show();
            double centerX = owner.getX() + (owner.getWidth() / 2) - (confirmStage.getWidth() / 2);
            double centerY = owner.getY() + (owner.getHeight() / 2) - (confirmStage.getHeight() / 2);
            confirmStage.setX(centerX);
            confirmStage.setY(centerY);

            // 3. SMOOTH FADE IN ANIMATION
            root.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            // 4. REMOVE BLUR WHEN CLOSED
            confirmStage.setOnHidden(e -> owner.getScene().getRoot().setEffect(null));

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