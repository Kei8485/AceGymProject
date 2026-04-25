package codes.acegym.Controllers;

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
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class HomePageController {

    @FXML ImageView closeWindowIcon, maxMinWindow, minimizeWindow;
    @FXML private ToggleButton ReportForm, CoachList, MembersForm, PaymentForm,
            planForm, registrationForm, adminProfile, dashboardBtn;
    @FXML private Button      logoutButton;
    @FXML private VBox        contentArea;
    @FXML private ToggleGroup menuGroup;
    @FXML private StackPane   homePageBgID;

    private AnimationTimer glowTimer;
    private double x = 0, y = 0;

    // All views live here permanently — switching = visibility flip, never insertion/removal
    private StackPane pageOverlay;

    private final Map<String, Parent> viewCache = new HashMap<>();
    private String   currentPagePath = null;
    private Timeline switchTimeline;

    // Thread-safe counter for background loading progress
    private final AtomicInteger pagesLoaded = new AtomicInteger(0);

    // 2-thread pool: loads pages in parallel without flooding the FX thread
    private ExecutorService loaderPool;

    private static final List<String> ALL_PAGES = List.of(
            "/codes/acegym/Dashboard.fxml",
            "/codes/acegym/AdminProfile.fxml",
            "/codes/acegym/Registration.fxml",
            "/codes/acegym/Plan.fxml",
            "/codes/acegym/Payment.fxml",
            "/codes/acegym/ViewMembers.fxml",
            "/codes/acegym/ReportPage.fxml",
            "/codes/acegym/Coaches.fxml"
    );

    // ── Logout ──
    private Parent    logoutRoot;
    private Stage     logoutStage;
    private ImageView blurSnapshot;

    // ── Loading overlay nodes ──
    private StackPane loadingOverlay;
    private Label     loadingLabel;
    private ProgressBar loadingBar; // kept as unused field for compatibility, not shown

    // ═══════════════════════════════════════════════════════════════
    // INIT
    // ═══════════════════════════════════════════════════════════════

    @FXML
    private void initialize() {
        menuGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null && oldT != null) oldT.setSelected(true);
        });

        // Permanent StackPane that holds every page forever
        pageOverlay = new StackPane();
        pageOverlay.setMaxWidth(Double.MAX_VALUE);
        pageOverlay.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(pageOverlay, Priority.ALWAYS);
        contentArea.getChildren().add(pageOverlay);

        setDashboardBackground();
        setupNavigation();

        // Disable all nav buttons until loading is complete
        setNavDisabled(true);

        showLoadingOverlay();
        startBackgroundPreload();

        Platform.runLater(() -> {
            if (!menuGroup.getToggles().isEmpty()) {
                Node node = (Node) menuGroup.getToggles().get(0);
                node.sceneProperty().addListener((obs, oldScene, newScene) -> {
                    if (newScene != null) {
                        newScene.windowProperty().addListener((obsW, oldWin, newWin) -> {
                            if (newWin instanceof Stage stage) {
                                addResizeListener(stage);
                                stage.iconifiedProperty().addListener((o, wasMin, isMin) -> {
                                    if (isMin) pauseAnimations();
                                    else resumeAnimations();
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // LOADING OVERLAY — static design, no animations, no progress bar
    // ═══════════════════════════════════════════════════════════════

    private void showLoadingOverlay() {
        loadingOverlay = new StackPane();
        loadingOverlay.setStyle("-fx-background-color: transparent; -fx-padding: 0 0 150 0;");
        loadingOverlay.setAlignment(javafx.geometry.Pos.CENTER);
        StackPane.setAlignment(loadingOverlay, javafx.geometry.Pos.CENTER);

        VBox inner = new VBox(28);
        inner.setAlignment(javafx.geometry.Pos.CENTER);
        inner.setMaxWidth(300);
        StackPane.setAlignment(inner, javafx.geometry.Pos.CENTER);

        // Static logo ring (no animations)
        StackPane logoRing = buildLogoRing();

        // ACE (red) GYM (white)
        Label nameAce = new Label("ACE");
        nameAce.setStyle(
                "-fx-text-fill: #e53935; -fx-font-family: 'Inter'; " +
                        "-fx-font-size: 28px; -fx-font-weight: bold; -fx-letter-spacing: 4px;");
        Label nameGym = new Label("GYM");
        nameGym.setStyle(
                "-fx-text-fill: #e8e8f0; -fx-font-family: 'Inter'; " +
                        "-fx-font-size: 28px; -fx-font-weight: bold; -fx-letter-spacing: 4px;");
        javafx.scene.layout.HBox nameRow = new javafx.scene.layout.HBox(6, nameAce, nameGym);
        nameRow.setAlignment(javafx.geometry.Pos.CENTER);

        // Tagline
        Label tagline = new Label("FITNESS MANAGEMENT");
        tagline.setStyle(
                "-fx-text-fill: #3a3f55; -fx-font-family: 'Inter'; " +
                        "-fx-font-size: 10px; -fx-letter-spacing: 2.5px;");

        // Simple status text — no bar, no counter
        loadingLabel = new Label("Preparing your workspace...");
        loadingLabel.setStyle(
                "-fx-text-fill: #3a3f55; -fx-font-family: 'Inter'; -fx-font-size: 11px;");

        inner.getChildren().addAll(logoRing, nameRow, tagline, loadingLabel);
        loadingOverlay.getChildren().add(inner);

        // Fill the entire contentArea and center everything
        loadingOverlay.setMaxWidth(Double.MAX_VALUE);
        loadingOverlay.setMaxHeight(Double.MAX_VALUE);
        contentArea.getChildren().add(loadingOverlay);
        VBox.setVgrow(loadingOverlay, Priority.ALWAYS);
    }

    /**
     * Builds the logo ring: static design only.
     * Two arcs, four dots, logo image — no RotateTransition, no ScaleTransition.
     *
     *  Outer radius  = 52  (outer arc track)
     *  Inner radius  = 38  (inner arc track)
     *  Logo circle   = 30  (clipped ImageView)
     *  Total canvas  = 120 x 120
     */
    private StackPane buildLogoRing() {
        double SIZE   = 120;
        double CENTER = SIZE / 2; // 60

        StackPane sp = new StackPane();
        sp.setPrefSize(SIZE, SIZE);
        sp.setMaxSize(SIZE, SIZE);

        // Subtle glow backdrop — static, no pulse
        Circle glowCircle = new Circle(CENTER, CENTER, 48);
        glowCircle.setFill(Color.web("#cb443e", 0.12));
        glowCircle.setStroke(Color.TRANSPARENT);

        // Outer track ring
        Circle outerTrack = new Circle(CENTER, CENTER, 52);
        outerTrack.setFill(Color.TRANSPARENT);
        outerTrack.setStroke(Color.web("#1a1f32"));
        outerTrack.setStrokeWidth(2.5);

        // Outer arc (bright red, 110° sweep — static)
        Arc outerArc = new Arc(CENTER, CENTER, 52, 52, 90, -110);
        outerArc.setType(ArcType.OPEN);
        outerArc.setFill(Color.TRANSPARENT);
        outerArc.setStroke(Color.web("#e53935"));
        outerArc.setStrokeWidth(2.5);
        outerArc.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        // Inner track ring
        Circle innerTrack = new Circle(CENTER, CENTER, 38);
        innerTrack.setFill(Color.TRANSPARENT);
        innerTrack.setStroke(Color.web("#1a1f32"));
        innerTrack.setStrokeWidth(2);

        // Inner arc (darker red, 70° sweep — static)
        Arc innerArc = new Arc(CENTER, CENTER, 38, 38, 270, -70);
        innerArc.setType(ArcType.OPEN);
        innerArc.setFill(Color.TRANSPARENT);
        innerArc.setStroke(Color.web("#8b2d2f"));
        innerArc.setStrokeWidth(2);
        innerArc.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        // Four static dots on the outer ring
        javafx.scene.Group dotsGroup = new javafx.scene.Group();
        double[] dotAngles = { 0, 90, 180, 270 };
        for (double angle : dotAngles) {
            double rad = Math.toRadians(angle);
            double dx  = CENTER + 52 * Math.cos(rad);
            double dy  = CENTER - 52 * Math.sin(rad);
            Circle dot = new Circle(dx, dy, 2.5);
            dot.setFill(Color.web("#cb443e", 0.6));
            dotsGroup.getChildren().add(dot);
        }

        // Logo background circle
        Circle logoCircleBg = new Circle(CENTER, CENTER, 30);
        logoCircleBg.setFill(Color.web("#161b2e"));
        logoCircleBg.setStroke(Color.web("#cb443e", 0.25));
        logoCircleBg.setStrokeWidth(1);



        // Assemble: glow → tracks → arcs → dots → logo bg
        javafx.scene.Group allShapes = new javafx.scene.Group(
                glowCircle,
                outerTrack, outerArc,
                innerTrack, innerArc,
                dotsGroup,
                logoCircleBg
        );
        // logoLetter sits on top via StackPane so it centers naturally
        sp.getChildren().addAll(allShapes);
        return sp;
    }

    // ═══════════════════════════════════════════════════════════════
    // HIDE LOADING OVERLAY
    // Sequence:
    //  1. The inner VBox content gently rises + fades out (200ms)
    //  2. The overlay itself fades to black, then scales down like a
    //     lens closing (600ms total) — feels intentional, not abrupt.
    //  3. The first page (Dashboard) scales in from 96% → 100% with
    //     an opacity rise, so the reveal feels physical.
    // ═══════════════════════════════════════════════════════════════

    private void hideLoadingOverlay() {
        if (loadingOverlay == null) return;

        StackPane overlay  = loadingOverlay;
        loadingOverlay = null;
        loadingLabel   = null;
        loadingBar     = null;

        // ── Step 1: content drifts up and disappears (200ms) ──
        // The inner VBox is the first (and only) child of the overlay StackPane
        if (!overlay.getChildren().isEmpty()) {
            javafx.scene.Node inner = overlay.getChildren().get(0);
            Timeline contentOut = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(inner.opacityProperty(),    1,    Interpolator.EASE_IN),
                            new KeyValue(inner.translateYProperty(), 0,    Interpolator.EASE_IN)),
                    new KeyFrame(Duration.millis(220),
                            new KeyValue(inner.opacityProperty(),    0,    Interpolator.EASE_IN),
                            new KeyValue(inner.translateYProperty(), -18,  Interpolator.EASE_IN))
            );
            contentOut.play();
        }

        // ── Step 2: after content is gone, collapse the overlay (lens-close) ──
        PauseTransition wait = new PauseTransition(Duration.millis(180));
        wait.setOnFinished(ev -> {
            // Fade + slight scale-down — like a shutter closing
            Timeline overlayOut = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(overlay.opacityProperty(),  1.0,  Interpolator.EASE_IN),
                            new KeyValue(overlay.scaleXProperty(),   1.0,  Interpolator.EASE_IN),
                            new KeyValue(overlay.scaleYProperty(),   1.0,  Interpolator.EASE_IN)),
                    new KeyFrame(Duration.millis(480),
                            new KeyValue(overlay.opacityProperty(),  0.0,  Interpolator.EASE_IN),
                            new KeyValue(overlay.scaleXProperty(),   1.04, Interpolator.EASE_IN),
                            new KeyValue(overlay.scaleYProperty(),   1.04, Interpolator.EASE_IN))
            );
            overlayOut.setOnFinished(e -> {
                contentArea.getChildren().remove(overlay);
                // ── Step 3: reveal the first page with a satisfying scale-up ──
                Parent firstPage = viewCache.get("/codes/acegym/Dashboard.fxml");
                if (firstPage != null) {
                    firstPage.setOpacity(0);
                    firstPage.setScaleX(0.96);
                    firstPage.setScaleY(0.96);
                    Timeline pageReveal = new Timeline(
                            new KeyFrame(Duration.ZERO,
                                    new KeyValue(firstPage.opacityProperty(), 0,    Interpolator.EASE_OUT),
                                    new KeyValue(firstPage.scaleXProperty(),  0.96, Interpolator.EASE_OUT),
                                    new KeyValue(firstPage.scaleYProperty(),  0.96, Interpolator.EASE_OUT)),
                            new KeyFrame(Duration.millis(350),
                                    new KeyValue(firstPage.opacityProperty(), 1,    Interpolator.EASE_OUT),
                                    new KeyValue(firstPage.scaleXProperty(),  1.0,  Interpolator.EASE_OUT),
                                    new KeyValue(firstPage.scaleYProperty(),  1.0,  Interpolator.EASE_OUT))
                    );
                    pageReveal.play();
                }
            });
            overlayOut.play();
        });
        wait.play();
    }

    // ═══════════════════════════════════════════════════════════════
    // BACKGROUND PRELOADING
    // ═══════════════════════════════════════════════════════════════

    private void startBackgroundPreload() {
        loaderPool = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "fxml-preloader");
            t.setDaemon(true);
            return t;
        });

        pagesLoaded.set(0);

        for (int i = 0; i < ALL_PAGES.size(); i++) {
            final String  path    = ALL_PAGES.get(i);
            final boolean isFirst = (i == 0);

            // Inside your HomePageController startBackgroundPreload loop
            loaderPool.submit(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
                    Parent view = loader.load();

                    // --- THIS IS THE KEY ADDITION ---
                    // We get the controller (e.g. ReportPageController)
                    // and hide it inside the view's properties.
                    Object controller = loader.getController();
                    if (controller != null) {
                        view.getProperties().put("controller", controller);
                    }
                    // --------------------------------

                    Platform.runLater(() -> registerPage(path, view, isFirst));
                } catch (Exception e) {
                    System.err.println("Failed to load: " + path);
                    e.printStackTrace();
                    Platform.runLater(() -> onPageLoaded(ALL_PAGES.size()));
                }
            });
        }

        loaderPool.shutdown();
    }

    /** Called on FX thread. Attaches an already-loaded view into the scene. */
    private void registerPage(String path, Parent view, boolean showImmediately) {
        if (view instanceof Region r) {
            r.setMaxWidth(Double.MAX_VALUE);
            r.setMaxHeight(Double.MAX_VALUE);
        }
        StackPane.setAlignment(view, javafx.geometry.Pos.TOP_LEFT);

        view.setVisible(false);
        view.setManaged(false);
        view.setCache(true);
        view.setCacheHint(javafx.scene.CacheHint.SPEED);

        pageOverlay.getChildren().add(view);
        viewCache.put(path, view);

        if (showImmediately) {
            view.setVisible(true);
            view.setManaged(true);
            currentPagePath = path;
            dashboardBtn.setSelected(true);
        } else {
            Platform.runLater(() -> {
                try { view.snapshot(null, new WritableImage(1, 1)); }
                catch (Exception ignored) { }
            });
        }

        onPageLoaded(ALL_PAGES.size());
    }

    /** Called on FX thread after each page lands. Finalises when all done. */
    private void onPageLoaded(int total) {
        int done = pagesLoaded.incrementAndGet();

        if (done >= total) {
            PauseTransition pause = new PauseTransition(Duration.millis(300));
            pause.setOnFinished(e -> {
                hideLoadingOverlay();
                setNavDisabled(false);
            });
            pause.play();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PAGE DISPLAY — zero scene-graph mutation after initial load
    // ═══════════════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════════════
    // PAGE TRANSITION  —  "parallax slide + depth" effect
    //
    // Outgoing page: fades out + slides left 28px + scales down to 0.97
    // Incoming page: slides in from the right 28px + fades + scales to 1.0
    // Both run in parallel on a single Timeline so they stay in sync.
    // Duration: 260ms with EASE_BOTH — fast enough to feel snappy,
    // slow enough to read as intentional motion.
    // ═══════════════════════════════════════════════════════════════

    private void showPage(String fxmlPath) {
        if (fxmlPath.equals(currentPagePath)) return;

        Parent next = viewCache.get(fxmlPath);
        if (next == null) return;

        // ── TRIGGER REFRESH ──────────────────────────────────────────
        // We look into the "pocket" of the view to find its controller
        Object controller = next.getProperties().get("controller");

        // Check if the controller implements our Refreshable interface
        if (controller instanceof Refreshable r) {
            r.refreshData(); // Run the specific reload logic for this tab
        }
        // ─────────────────────────────────────────────────────────────

        Parent prev = currentPagePath != null ? viewCache.get(currentPagePath) : null;
        currentPagePath = fxmlPath;


        // ── Reset next page to starting position (right, slightly scaled down) ──
        next.setOpacity(0);
        next.setTranslateX(28);
        next.setScaleX(0.97);
        next.setScaleY(0.97);
        next.setVisible(true);
        next.setManaged(true);
        next.toFront();

        if (switchTimeline != null) switchTimeline.stop();

        // ── Outgoing keyframes ──
        double OUT_X     = -22;   // slides left
        double OUT_SCALE = 0.97;  // shrinks slightly
        int    MS        = 260;   // total duration

        List<KeyFrame> frames = new java.util.ArrayList<>();

        // t = 0 → snapshot current state of prev
        if (prev != null) {
            frames.add(new KeyFrame(Duration.ZERO,
                    new KeyValue(prev.opacityProperty(),    1,    Interpolator.EASE_BOTH),
                    new KeyValue(prev.translateXProperty(), 0,    Interpolator.EASE_BOTH),
                    new KeyValue(prev.scaleXProperty(),     1.0,  Interpolator.EASE_BOTH),
                    new KeyValue(prev.scaleYProperty(),     1.0,  Interpolator.EASE_BOTH)
            ));
            frames.add(new KeyFrame(Duration.millis(MS),
                    new KeyValue(prev.opacityProperty(),    0,         Interpolator.EASE_BOTH),
                    new KeyValue(prev.translateXProperty(), OUT_X,     Interpolator.EASE_BOTH),
                    new KeyValue(prev.scaleXProperty(),     OUT_SCALE, Interpolator.EASE_BOTH),
                    new KeyValue(prev.scaleYProperty(),     OUT_SCALE, Interpolator.EASE_BOTH)
            ));
        }

        // t = 0 → next starts right + transparent
        frames.add(new KeyFrame(Duration.ZERO,
                new KeyValue(next.opacityProperty(),    0,    Interpolator.EASE_BOTH),
                new KeyValue(next.translateXProperty(), 28,   Interpolator.EASE_BOTH),
                new KeyValue(next.scaleXProperty(),     0.97, Interpolator.EASE_BOTH),
                new KeyValue(next.scaleYProperty(),     0.97, Interpolator.EASE_BOTH)
        ));
        frames.add(new KeyFrame(Duration.millis(MS),
                new KeyValue(next.opacityProperty(),    1,    Interpolator.EASE_BOTH),
                new KeyValue(next.translateXProperty(), 0,    Interpolator.EASE_BOTH),
                new KeyValue(next.scaleXProperty(),     1.0,  Interpolator.EASE_BOTH),
                new KeyValue(next.scaleYProperty(),     1.0,  Interpolator.EASE_BOTH)
        ));

        switchTimeline = new Timeline(frames.toArray(new KeyFrame[0]));
        switchTimeline.setOnFinished(e -> {
            if (prev != null) {
                // Clean up prev — reset transforms so it's ready if revisited
                prev.setVisible(false);
                prev.setManaged(false);
                prev.setTranslateX(0);
                prev.setScaleX(1.0);
                prev.setScaleY(1.0);
                prev.setOpacity(1);
            }
        });
        switchTimeline.play();
    }

    // ═══════════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════════

    private void setupNavigation() {
        ToggleButton[] menuButtons = {
                dashboardBtn, adminProfile, registrationForm, planForm,
                PaymentForm, MembersForm, ReportForm, CoachList
        };
        for (ToggleButton btn : menuButtons) {
            if (btn != null) addHoverAnimation(btn);
        }

        if (dashboardBtn     != null) dashboardBtn.setOnAction(e     -> handleNavigation(dashboardBtn,     "/codes/acegym/Dashboard.fxml"));
        if (adminProfile     != null) adminProfile.setOnAction(e     -> handleNavigation(adminProfile,     "/codes/acegym/AdminProfile.fxml"));
        if (ReportForm       != null) ReportForm.setOnAction(e       -> handleNavigation(ReportForm,       "/codes/acegym/ReportPage.fxml"));
        if (MembersForm      != null) MembersForm.setOnAction(e      -> handleNavigation(MembersForm,      "/codes/acegym/ViewMembers.fxml"));
        if (PaymentForm      != null) PaymentForm.setOnAction(e      -> handleNavigation(PaymentForm,      "/codes/acegym/Payment.fxml"));
        if (planForm         != null) planForm.setOnAction(e         -> handleNavigation(planForm,         "/codes/acegym/Plan.fxml"));
        if (CoachList        != null) CoachList.setOnAction(e        -> handleNavigation(CoachList,        "/codes/acegym/Coaches.fxml"));
        if (registrationForm != null) registrationForm.setOnAction(e -> handleNavigation(registrationForm, "/codes/acegym/Registration.fxml"));
    }

    private void handleNavigation(ToggleButton button, String fxmlPath) {
        if (!button.isSelected()) return;
        showPage(fxmlPath);
    }

    /** Enables or disables all nav toggle buttons with a subtle opacity fade. */
    private void setNavDisabled(boolean disabled) {
        ToggleButton[] buttons = {
                dashboardBtn, adminProfile, registrationForm, planForm,
                PaymentForm, MembersForm, ReportForm, CoachList
        };
        for (ToggleButton btn : buttons) {
            if (btn == null) continue;
            btn.setDisable(disabled);
            FadeTransition ft = new FadeTransition(Duration.millis(300), btn);
            ft.setToValue(disabled ? 0.45 : 1.0);
            ft.play();
        }
    }

    private void addHoverAnimation(ToggleButton button) {
        ScaleTransition up   = new ScaleTransition(Duration.millis(180), button);
        ScaleTransition down = new ScaleTransition(Duration.millis(180), button);
        up.setToX(1.05);  up.setToY(1.05);
        down.setToX(1.0); down.setToY(1.0);
        button.setOnMouseEntered(e -> { down.stop(); up.playFromStart(); });
        button.setOnMouseExited(e  -> { up.stop();   down.playFromStart(); });
    }

    // ═══════════════════════════════════════════════════════════════
    // LOGOUT — static snapshot blur
    // ═══════════════════════════════════════════════════════════════

    @FXML
    private void handleLogoutClick(ActionEvent event) {
        try {
            Stage owner = (Stage) logoutButton.getScene().getWindow();

            if (logoutStage == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/codes/acegym/LogoutOverlay.fxml"));
                logoutRoot = loader.load();
                LogoutOverlayController overlayController = loader.getController();
                overlayController.setHomeController(this);

                logoutStage = new Stage();
                logoutStage.initStyle(StageStyle.TRANSPARENT);
                logoutStage.initModality(Modality.APPLICATION_MODAL);
                logoutStage.initOwner(owner);
                Scene scene = new Scene(logoutRoot);
                scene.setFill(null);
                logoutStage.setScene(scene);
            }

            WritableImage snap = homePageBgID.snapshot(null, null);
            if (blurSnapshot == null) {
                blurSnapshot = new ImageView(snap);
                blurSnapshot.setFitWidth(homePageBgID.getWidth());
                blurSnapshot.setFitHeight(homePageBgID.getHeight());
                blurSnapshot.setEffect(new GaussianBlur(10));
                blurSnapshot.setMouseTransparent(true);
                blurSnapshot.setVisible(false);
                homePageBgID.getChildren().add(blurSnapshot);
            } else {
                blurSnapshot.setImage(snap);
                blurSnapshot.setFitWidth(homePageBgID.getWidth());
                blurSnapshot.setFitHeight(homePageBgID.getHeight());
            }
            blurSnapshot.setVisible(true);

            logoutStage.show();
            logoutStage.setX(owner.getX() + (owner.getWidth()  / 2) - (logoutStage.getWidth()  / 2));
            logoutStage.setY(owner.getY() + (owner.getHeight() / 2) - (logoutStage.getHeight() / 2));

            logoutRoot.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), logoutRoot);
            fadeIn.setFromValue(0); fadeIn.setToValue(1);
            fadeIn.play();

            logoutStage.setOnHidden(e -> blurSnapshot.setVisible(false));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ANIMATION CONTROLS
    // ═══════════════════════════════════════════════════════════════

    public void pauseAnimations()  { if (glowTimer != null) glowTimer.stop(); }
    public void resumeAnimations() { if (glowTimer != null) glowTimer.start(); }
    public void stopAnimations()   { if (glowTimer != null) glowTimer.stop(); }

    // ═══════════════════════════════════════════════════════════════
    // BACKGROUND GLOW
    // ═══════════════════════════════════════════════════════════════

    private void setDashboardBackground() {
        homePageBgID.setStyle("-fx-background-color: #0D1117;");

        Canvas canvas = new Canvas();
        canvas.setMouseTransparent(true);
        homePageBgID.layoutBoundsProperty().addListener((obs, o, n) -> {
            canvas.setWidth(n.getWidth());
            canvas.setHeight(n.getHeight());
        });
        homePageBgID.getChildren().add(0, canvas);

        Platform.runLater(() -> {
            canvas.setWidth(homePageBgID.getWidth());
            canvas.setHeight(homePageBgID.getHeight());
            startGlowPulse(canvas);
        });
    }

    private void startGlowPulse(Canvas canvas) {
        if (glowTimer != null) glowTimer.stop();
        long[] startTime = { System.nanoTime() };

        Stop[] blStops = new Stop[3];
        Stop[] trStops = new Stop[3];
        blStops[2] = trStops[2] = new Stop(1.0, Color.TRANSPARENT);

        glowTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double W = canvas.getWidth(), H = canvas.getHeight();
                if (W <= 0 || H <= 0) return;

                double t       = (now - startTime[0]) / 1_000_000_000.0;
                double pulseBL = 0.10 + 0.05 * (0.5 + 0.5 * Math.sin(t * 0.6));
                double pulseTR = 0.07 + 0.05 * (0.5 + 0.5 * Math.sin(t * 0.6 + Math.PI * 0.7));

                GraphicsContext gc = canvas.getGraphicsContext2D();
                gc.setFill(Color.web("#0D1117"));
                gc.fillRect(0, 0, W, H);

                blStops[0] = new Stop(0.0, Color.web("#CB443E", pulseBL));
                blStops[1] = new Stop(0.4, Color.web("#CB443E", pulseBL * 0.5));
                gc.setFill(new RadialGradient(0, 0, 0, H, W * 0.85, false, CycleMethod.NO_CYCLE, blStops));
                gc.fillRect(0, 0, W, H);

                trStops[0] = new Stop(0.0, Color.web("#8B2D2F", pulseTR));
                trStops[1] = new Stop(0.4, Color.web("#8B2D2F", pulseTR * 0.4));
                gc.setFill(new RadialGradient(0, 0, W, 0, W * 0.65, false, CycleMethod.NO_CYCLE, trStops));
                gc.fillRect(0, 0, W, H);
            }
        };
        glowTimer.start();
    }

    // ═══════════════════════════════════════════════════════════════
    // WINDOW CONTROLS
    // ═══════════════════════════════════════════════════════════════

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
            double mx = e.getX(), my = e.getY();
            double w  = stage.getWidth(), h = stage.getHeight();
            Cursor cursor = Cursor.DEFAULT;

            if      (mx < border && my < border)         cursor = Cursor.NW_RESIZE;
            else if (mx < border && my > h - border)     cursor = Cursor.SW_RESIZE;
            else if (mx > w - border && my < border)     cursor = Cursor.NE_RESIZE;
            else if (mx > w - border && my > h - border) cursor = Cursor.SE_RESIZE;
            else if (mx < border)                        cursor = Cursor.W_RESIZE;
            else if (mx > w - border)                    cursor = Cursor.E_RESIZE;
            else if (my < border)                        cursor = Cursor.N_RESIZE;
            else if (my > h - border)                    cursor = Cursor.S_RESIZE;

            stage.getScene().setCursor(cursor);
        });

        stage.getScene().setOnMouseDragged(e -> {
            if (stage.isMaximized()) return;
            double mx = e.getScreenX(), my = e.getScreenY();
            Cursor cursor = stage.getScene().getCursor();

            if (cursor == Cursor.E_RESIZE  || cursor == Cursor.SE_RESIZE || cursor == Cursor.NE_RESIZE)
                stage.setWidth(mx - stage.getX());
            if (cursor == Cursor.S_RESIZE  || cursor == Cursor.SE_RESIZE || cursor == Cursor.SW_RESIZE)
                stage.setHeight(my - stage.getY());
            if (cursor == Cursor.W_RESIZE  || cursor == Cursor.NW_RESIZE || cursor == Cursor.SW_RESIZE) {
                double newWidth = stage.getX() + stage.getWidth() - mx;
                if (newWidth > stage.getMinWidth()) { stage.setX(mx); stage.setWidth(newWidth); }
            }
            if (cursor == Cursor.N_RESIZE  || cursor == Cursor.NW_RESIZE || cursor == Cursor.NE_RESIZE) {
                double newHeight = stage.getY() + stage.getHeight() - my;
                if (newHeight > stage.getMinHeight()) { stage.setY(my); stage.setHeight(newHeight); }
            }
        });
    }
}