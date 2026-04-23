package codes.acegym.Controllers;

import codes.acegym.DB.CoachDAO;
import codes.acegym.Objects.Coach;
import javafx.animation.FadeTransition;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

public class CoachCardController {

    @FXML private Button    manageClientBtn;
    @FXML private Button    editCoachBtn;
    @FXML private ImageView removeCoachBtn;
    @FXML private ImageView coachImage;
    @FXML private Label     nameLabel;
    @FXML private Label     idLabel;
    @FXML private Label     trainingTypeLabel;   // add fx:id="trainingTypeLabel" in CoachCard.fxml
    @FXML private VBox      clientsContainer;

    private Coach    coach;
    private Runnable onDeleteCallback;

    public void setCoach(Coach coach) {
        this.coach = coach;
        refreshCard();
    }

    public void setOnDeleteCallback(Runnable r) {
        this.onDeleteCallback = r;
    }

    @FXML
    public void initialize() {
        if (manageClientBtn != null)
            manageClientBtn.setOnAction(e -> openModal("/codes/acegym/AddClientInCoach.fxml"));

        if (editCoachBtn != null)
            editCoachBtn.setOnAction(e -> openModal("/codes/acegym/EditCoach.fxml"));

        // Remove coach → inline confirm dialog
        if (removeCoachBtn != null) {
            removeCoachBtn.setOnMouseClicked(e -> showInlineConfirm(
                    "Are you sure you want to remove this coach\nand all their client assignments?",
                    () -> {
                        if (coach != null && CoachDAO.deleteCoach(coach.getStaffID())) {
                            if (onDeleteCallback != null) onDeleteCallback.run();
                        }
                    }
            ));
        }
    }

    // ── Refresh the card UI from the current coach object ───────────────────
    public void refreshCard() {
        if (coach == null) return;

        nameLabel.setText(coach.getFullName());
        // Format: ST001, ST002, etc.
        idLabel.setText("ST" + String.format("%03d", coach.getStaffID()));
        // Show training category under the ID
        if (trainingTypeLabel != null) {
            String cat = coach.getTrainingCategory();
            trainingTypeLabel.setText(
                    (cat == null || cat.isBlank() || cat.equals("Unassigned"))
                            ? "No Training Type"
                            : cat);
        }

        String imgPath = coach.getStaffImage();
        if (imgPath != null && !imgPath.isBlank()) {
            try {
                // Try as a local file path first, then as URI/URL
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
            coachImage.setImage(new Image(
                    getClass().getResourceAsStream("/image/admin-user.png")));
        } catch (Exception ignored) {}
    }

    public void refreshClientsList() {
        if (clientsContainer == null || coach == null) return;
        clientsContainer.getChildren().clear();

        ObservableList<String> names = CoachDAO.getClientNamesForCoach(coach.getStaffID());
        if (names.isEmpty()) {
            Label none = new Label("No clients assigned");
            none.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px; -fx-font-style: italic;");
            clientsContainer.getChildren().add(none);
        } else {
            for (String name : names) {
                Label lbl = new Label("• " + name);
                lbl.setStyle("-fx-text-fill: #c9cdd6; -fx-font-size: 12px;");
                lbl.setWrapText(true);
                clientsContainer.getChildren().add(lbl);
            }
        }
    }

    // ── Open FXML modals and inject coach data ───────────────────────────────
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
            ft.setFromValue(0); ft.setToValue(1); ft.play();

            modalStage.setOnHidden(e -> {
                owner.getScene().getRoot().setEffect(null);
                refreshCard();
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Inline confirm popup — used for delete coach ─────────────────────────
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
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void centerStage(Stage stage) {
        Stage owner = (Stage) editCoachBtn.getScene().getWindow();
        stage.setX(owner.getX() + (owner.getWidth()  / 2) - (stage.getWidth()  / 2));
        stage.setY(owner.getY() + (owner.getHeight() / 2) - (stage.getHeight() / 2));
    }
}