package codes.acegym.Controllers;

import codes.acegym.DB.CoachDAO;
import codes.acegym.Objects.Coach;
import javafx.animation.FadeTransition;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;

/**
 * Card controller — no longer tied to CoachCard.fxml.
 * Call createCard() to get the fully built node, then setAdminMode() + setCoach().
 * This eliminates one FXMLLoader.load() call per card, which was the primary lag source.
 */
public class CoachCardController {

    // ── UI nodes (previously @FXML injected) ────────────────────────────────
    private Button    manageClientBtn;
    private Button    editCoachBtn;
    private ImageView removeCoachBtn;
    private ImageView coachImage;
    private Label     nameLabel;
    private Label     idLabel;
    private Label     trainingTypeLabel;
    private VBox      clientsContainer;

    private Coach    coach;
    private Runnable onDeleteCallback;
    private boolean  adminMode = false;

    // ════════════════════════════════════════════════════════════════════════
    // FACTORY — replaces FXMLLoader.load() + controller wiring
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Builds the full card node tree programmatically.
     * Mirrors CoachCard.fxml exactly — same style classes, same layout.
     * Must be called before setAdminMode() / setCoach().
     */
    public AnchorPane createCard() {

        // ── Avatar ───────────────────────────────────────────────────────────
        coachImage = new ImageView();
        coachImage.setFitHeight(60);
        coachImage.setFitWidth(60);
        coachImage.setPickOnBounds(true);
        coachImage.setPreserveRatio(true);

        StackPane avatarFrame = new StackPane(coachImage);
        avatarFrame.getStyleClass().add("avatar-frame");
        avatarFrame.setMinSize(56, 56);
        avatarFrame.setMaxSize(60, 60);
        avatarFrame.setPrefSize(60, 60);

        // ── Name / ID / badge column ─────────────────────────────────────────
        nameLabel = new Label("Coach Name");
        nameLabel.getStyleClass().add("coach-name");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        idLabel = new Label("ST000");
        idLabel.getStyleClass().add("coach-id");

        trainingTypeLabel = new Label("No Training Type");
        trainingTypeLabel.getStyleClass().add("training-type-badge");
        trainingTypeLabel.setWrapText(true);

        HBox idRow = new HBox(8, idLabel, trainingTypeLabel);
        idRow.setAlignment(Pos.CENTER_LEFT);

        VBox nameBlock = new VBox(4, nameLabel, idRow);
        nameBlock.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameBlock, Priority.ALWAYS);

        // ── Remove X button ──────────────────────────────────────────────────
        removeCoachBtn = new ImageView();
        removeCoachBtn.setFitHeight(29);
        removeCoachBtn.setFitWidth(29);
        removeCoachBtn.setPickOnBounds(true);
        removeCoachBtn.setPreserveRatio(true);
        loadIcon(removeCoachBtn, "/image/xmark-solid.png");

        StackPane removeBtnBg = new StackPane(removeCoachBtn);
        removeBtnBg.getStyleClass().add("remove-btn-bg");
        removeBtnBg.setAlignment(Pos.CENTER);
        removeBtnBg.setMinSize(26, 26);
        removeBtnBg.setMaxSize(38, 38);
        removeBtnBg.setPrefWidth(38);

        // ── Header row ───────────────────────────────────────────────────────
        HBox header = new HBox(14, avatarFrame, nameBlock, removeBtnBg);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPrefWidth(337);
        header.getStyleClass().add("card-header");
        header.setPadding(new Insets(16));

        // ── Divider ──────────────────────────────────────────────────────────
        Region divider1 = divider();

        // ── Clients section ──────────────────────────────────────────────────
        Region dot = new Region();
        dot.getStyleClass().add("section-dot");
        dot.setMinSize(6, 6);
        dot.setMaxSize(6, 6);

        Label sectionTitle = new Label("CLIENTS HANDLING");
        sectionTitle.getStyleClass().add("section-title");

        HBox sectionHeader = new HBox(6, dot, sectionTitle);
        sectionHeader.setAlignment(Pos.CENTER_LEFT);

        clientsContainer = new VBox(4);

        ScrollPane clientsScroll = new ScrollPane(clientsContainer);
        clientsScroll.setFitToWidth(true);
        clientsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        clientsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        clientsScroll.setMaxHeight(72);
        clientsScroll.setPrefHeight(72);
        clientsScroll.getStyleClass().add("clients-scroll");

        VBox clientsSection = new VBox(8, sectionHeader, clientsScroll);
        clientsSection.setPadding(new Insets(14, 16, 14, 16));
        VBox.setVgrow(clientsSection, Priority.ALWAYS);

        // ── Divider ──────────────────────────────────────────────────────────
        Region divider2 = divider();

        // ── Action buttons ───────────────────────────────────────────────────
        editCoachBtn = new Button("Edit Coach");
        editCoachBtn.getStyleClass().add("btn-edit");
        editCoachBtn.setMaxWidth(Double.MAX_VALUE);
        editCoachBtn.setMnemonicParsing(false);
        HBox.setHgrow(editCoachBtn, Priority.ALWAYS);

        manageClientBtn = new Button("Manage Clients");
        manageClientBtn.getStyleClass().add("btn-manage");
        manageClientBtn.setMaxWidth(Double.MAX_VALUE);
        manageClientBtn.setMnemonicParsing(false);
        HBox.setHgrow(manageClientBtn, Priority.ALWAYS);

        HBox btnRow = new HBox(8, editCoachBtn, manageClientBtn);
        btnRow.setPadding(new Insets(12, 16, 12, 16));

        // ── Assemble ─────────────────────────────────────────────────────────
        VBox body = new VBox(0, header, divider1, clientsSection, divider2, btnRow);

        AnchorPane root = new AnchorPane(body);
        root.setPrefWidth(340);
        root.getStyleClass().add("card-container");
        AnchorPane.setTopAnchor(body, 0.0);
        AnchorPane.setBottomAnchor(body, 0.0);
        AnchorPane.setLeftAnchor(body, 0.0);
        AnchorPane.setRightAnchor(body, 0.0);

        // All card styles now live in Coaches.css — CoachCard.css deleted
        String css = resolveStylesheet("/codes/acegym/Css/Coaches.css");
        if (css != null) root.getStylesheets().add(css);

        // Wire events now that all nodes exist
        wireEvents();
        applyRoleVisibility();

        return root;
    }

    // ════════════════════════════════════════════════════════════════════════
    // PUBLIC API (unchanged from before)
    // ════════════════════════════════════════════════════════════════════════

    public void setAdminMode(boolean isAdmin) {
        this.adminMode = isAdmin;
        applyRoleVisibility();
    }

    public void setCoach(Coach coach) {
        this.coach = coach;
        refreshCard();
    }

    public void setOnDeleteCallback(Runnable r) {
        this.onDeleteCallback = r;
    }

    // ════════════════════════════════════════════════════════════════════════
    // ROLE VISIBILITY
    // ════════════════════════════════════════════════════════════════════════

    private void applyRoleVisibility() {
        if (editCoachBtn != null) {
            editCoachBtn.setVisible(adminMode);
            editCoachBtn.setManaged(adminMode);
        }
        if (removeCoachBtn != null) {
            removeCoachBtn.setVisible(adminMode);
            removeCoachBtn.setManaged(adminMode);
        }
        if (manageClientBtn != null) {
            manageClientBtn.setVisible(adminMode);
            manageClientBtn.setManaged(adminMode);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // CARD REFRESH
    // ════════════════════════════════════════════════════════════════════════

    public void refreshCard() {
        if (coach == null) return;

        nameLabel.setText(coach.getFullName());
        idLabel.setText("ST" + String.format("%03d", coach.getStaffID()));

        if (trainingTypeLabel != null) {
            String cat = coach.getTrainingCategory();
            trainingTypeLabel.setText(
                    (cat == null || cat.isBlank() || cat.equals("Unassigned"))
                            ? "No Training Type" : cat);
        }

        String imgPath = coach.getStaffImage();
        if (imgPath != null && !imgPath.isBlank()) {
            try {
                File imgFile = new File(imgPath);
                if (imgFile.exists()) {
                    coachImage.setImage(new Image(imgFile.toURI().toString()));
                } else {
                    coachImage.setImage(new Image(imgPath, true));
                }
            } catch (Exception ex) {
                loadDefaultImage();
            }
        } else {
            loadDefaultImage();
        }

        refreshClientsList();
    }

    private void loadDefaultImage() {
        try {
            coachImage.setImage(
                    new Image(getClass().getResourceAsStream("/image/admin-user.png")));
        } catch (Exception ignored) {}
    }

    public void refreshClientsList() {
        if (clientsContainer == null || coach == null) return;
        clientsContainer.getChildren().clear();

        ObservableList<String> names = CoachDAO.getClientNamesForCoach(coach.getStaffID());
        if (names.isEmpty()) {
            Label none = new Label("No clients assigned");
            none.setStyle("-fx-text-fill: #4a5068; -fx-font-size: 13px; -fx-font-style: italic;");
            clientsContainer.getChildren().add(none);
        } else {
            for (String name : names) {
                Label lbl = new Label("• " + name);
                lbl.setStyle("-fx-text-fill: #c9cdd6; -fx-font-size: 13px;");
                lbl.setWrapText(true);
                clientsContainer.getChildren().add(lbl);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // EVENT WIRING  (was initialize())
    // ════════════════════════════════════════════════════════════════════════

    private void wireEvents() {
        manageClientBtn.setOnAction(e -> openModal("/codes/acegym/AddClientInCoach.fxml"));
        editCoachBtn.setOnAction(e   -> openModal("/codes/acegym/EditCoach.fxml"));

        removeCoachBtn.setOnMouseClicked(e -> {
            if (coach == null) return;
            if (CoachDAO.hasClients(coach.getStaffID())) {
                showToast("Cannot delete — coach still has assigned clients.\n" +
                        "Remove all clients first via Manage Clients.");
            } else {
                showInlineConfirm(
                        "Are you sure you want to remove this coach?",
                        () -> {
                            if (CoachDAO.deleteCoach(coach.getStaffID())) {
                                if (onDeleteCallback != null) onDeleteCallback.run();
                            }
                        });
            }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // MODALS
    // ════════════════════════════════════════════════════════════════════════

    private void openModal(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object ctrl = loader.getController();
            if (ctrl instanceof EditCoachController editCtrl) {
                editCtrl.setCoach(coach, this);
            } else if (ctrl instanceof AddClientInCoachController addCtrl) {
                addCtrl.setCoach(coach, this);
            }

            Stage modalStage = new Stage();
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.initModality(Modality.APPLICATION_MODAL);

            Stage owner = (Stage) manageClientBtn.getScene().getWindow();
            modalStage.initOwner(owner);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            modalStage.setScene(scene);

            owner.getScene().getRoot().setEffect(new GaussianBlur(10));
            modalStage.show();
            centerStage(modalStage);

            root.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(300), root);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

            modalStage.setOnHidden(e -> {
                owner.getScene().getRoot().setEffect(null);
                refreshCard();
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // CONFIRM POPUP
    // ════════════════════════════════════════════════════════════════════════

    private void showInlineConfirm(String message, Runnable onConfirm) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);

        Stage owner = (Stage) removeCoachBtn.getScene().getWindow();
        popup.initOwner(owner);

        VBox root = new VBox(20);
        root.setPadding(new Insets(30, 28, 24, 28));
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(380);
        root.setStyle(
                "-fx-background-color: #1c2237;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #2e3349;" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.85), 40, 0, 0, 0);");

        Label icon = new Label("⚠");
        icon.setStyle("-fx-font-size: 34px; -fx-text-fill: #e53935;");

        Label msg = new Label(message);
        msg.setStyle("-fx-text-fill: #c9cdd6; -fx-font-size: 14px;");
        msg.setWrapText(true);
        msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #4b5563;" +
                        "-fx-border-radius: 8;" +
                        "-fx-text-fill: #9ca3af;" +
                        "-fx-font-size: 13px;" +
                        "-fx-padding: 8 20 8 20;" +
                        "-fx-cursor: hand;");

        Button confirmBtn = new Button("Remove");
        confirmBtn.setStyle(
                "-fx-background-color: #e53935;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13px;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 8 20 8 20;" +
                        "-fx-cursor: hand;");

        cancelBtn.setOnAction(e  -> popup.close());
        confirmBtn.setOnAction(e -> { popup.close(); onConfirm.run(); });

        btnRow.getChildren().addAll(cancelBtn, confirmBtn);
        root.getChildren().addAll(icon, msg, btnRow);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);

        owner.getScene().getRoot().setEffect(new GaussianBlur(10));
        popup.setOnHidden(e -> owner.getScene().getRoot().setEffect(null));
        popup.show();
        centerStage(popup);

        root.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(200), root);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    // ════════════════════════════════════════════════════════════════════════
    // TOAST
    // ════════════════════════════════════════════════════════════════════════

    private void showToast(String message) {
        Stage owner = (Stage) removeCoachBtn.getScene().getWindow();

        Label lbl = new Label(message);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-text-fill: #f9fafb; -fx-font-size: 13px;");

        HBox toast = new HBox(10);
        toast.setPadding(new Insets(14, 18, 14, 18));
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setMaxWidth(360);
        toast.setStyle(
                "-fx-background-color: #1c2237;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #e53935;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.80), 30, 0, 0, 0);");

        Label warnIcon = new Label("⚠");
        warnIcon.setStyle("-fx-font-size: 18px; -fx-text-fill: #e53935;");
        toast.getChildren().addAll(warnIcon, lbl);

        Stage toastStage = new Stage();
        toastStage.initStyle(StageStyle.TRANSPARENT);
        toastStage.initOwner(owner);

        Scene scene = new Scene(toast);
        scene.setFill(Color.TRANSPARENT);
        toastStage.setScene(scene);
        toastStage.show();

        toastStage.setX(owner.getX() + owner.getWidth()  - toastStage.getWidth()  - 24);
        toastStage.setY(owner.getY() + owner.getHeight() - toastStage.getHeight() - 24);

        toast.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        javafx.animation.PauseTransition hold =
                new javafx.animation.PauseTransition(Duration.millis(3000));
        hold.setOnFinished(ev -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toast);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev2 -> toastStage.close());
            fadeOut.play();
        });
        hold.play();
    }

    // ════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════════════════

    private Region divider() {
        Region r = new Region();
        r.getStyleClass().add("card-divider");
        r.setMaxWidth(Double.MAX_VALUE);
        return r;
    }

    private void loadIcon(ImageView iv, String resourcePath) {
        try {
            iv.setImage(new Image(getClass().getResourceAsStream(resourcePath)));
        } catch (Exception ignored) {}
    }

    private String resolveStylesheet(String path) {
        try {
            java.net.URL url = getClass().getResource(path);
            return url != null ? url.toExternalForm() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void centerStage(Stage stage) {
        Stage owner = (Stage) nameLabel.getScene().getWindow();
        stage.setX(owner.getX() + (owner.getWidth()  / 2) - (stage.getWidth()  / 2));
        stage.setY(owner.getY() + (owner.getHeight() / 2) - (stage.getHeight() / 2));
    }
}