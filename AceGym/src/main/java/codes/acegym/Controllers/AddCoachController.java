package codes.acegym.Controllers;

import codes.acegym.DB.CoachDAO;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import java.io.File;

public class AddCoachController {

    @FXML private ImageView        coachImageView;
    @FXML private TextField        firstNameField;
    @FXML private TextField        lastNameField;
    @FXML private ComboBox<String> trainingTypeCombo;
    @FXML private Button           btnCancel;
    @FXML private Button           btnAdd;
    @FXML private Label            errorLabel;

    private String   selectedImagePath;
    private Runnable onCoachAdded;

    public void setOnCoachAdded(Runnable callback) {
        this.onCoachAdded = callback;
    }

    @FXML
    public void initialize() {
        Circle clip = new Circle(55, 55, 55);
        coachImageView.setClip(clip);

        for (String[] type : CoachDAO.getTrainingTypes()) {
            trainingTypeCombo.getItems().add(type[0] + " | " + type[1]);
        }

        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }

        // Clear red border on typing
        firstNameField.textProperty().addListener((o, oldV, newV) -> clearError(firstNameField));
        lastNameField.textProperty().addListener((o, oldV, newV)  -> clearError(lastNameField));
        trainingTypeCombo.valueProperty().addListener((o, oldV, newV) -> clearError(trainingTypeCombo));
    }

    @FXML
    private void handleChangeProfile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Profile Image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) coachImageView.getScene().getWindow();
        File file = fc.showOpenDialog(stage);
        if (file != null) {
            // Store the absolute file path — DB saves this string so it can be reloaded later
            selectedImagePath = file.getAbsolutePath();
            coachImageView.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    private void handleAddCoach() {
        boolean valid = true;

        String fn = firstNameField.getText().trim();
        String ln = lastNameField.getText().trim();

        // ── Validation — mark each empty field with a red border ──
        StringBuilder errorMsg = new StringBuilder();
        if (fn.isEmpty()) {
            markError(firstNameField);
            errorMsg.append("First name is required. ");
            valid = false;
        }
        if (ln.isEmpty()) {
            markError(lastNameField);
            errorMsg.append("Last name is required. ");
            valid = false;
        }
        if (trainingTypeCombo.getValue() == null) {
            markError(trainingTypeCombo);
            errorMsg.append("Training type is required.");
            valid = false;
        }

        if (!valid) {
            showError(errorMsg.toString().trim());
            return;
        }

        int typeID = 0;
        String val = trainingTypeCombo.getValue();
        if (val != null && val.contains("|")) {
            try { typeID = Integer.parseInt(val.split("\\|")[0].trim()); }
            catch (NumberFormatException ignored) {}
        }

        boolean ok = CoachDAO.addCoach(fn, ln, typeID, selectedImagePath);
        if (ok) {
            if (onCoachAdded != null) onCoachAdded.run();
            showSuccessPopup("New coach added successfully!");
            // closeWindow() is now called from inside the popup's OK button
            // so the popup's owner stage is still alive when the popup opens
        } else {
            showError("Failed to add coach. Please try again.");
        }
    }

    // ── Success popup — small floating notification ──────────────────────────
    private void showSuccessPopup(String message) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner((Stage) btnCancel.getScene().getWindow());

        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(28, 36, 28, 36));
        box.setStyle(
                "-fx-background-color: #1c2237;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #2e3349;" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.85), 40, 0, 0, 0);");

        Label icon = new Label("✅");
        icon.setStyle("-fx-font-size: 30px;");

        Label msg = new Label(message);
        msg.setStyle("-fx-text-fill: #e8e8f0; -fx-font-size: 14px; -fx-font-weight: bold;");
        msg.setWrapText(true);
        msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button okBtn = new Button("OK");
        okBtn.setStyle(
                "-fx-background-color: #e53935; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-font-size: 13px;" +
                        "-fx-background-radius: 8; -fx-padding: 8 28 8 28; -fx-cursor: hand;");
        okBtn.setOnAction(e -> {
            popup.close();
            closeWindow();  // close the AddCoach form AFTER the popup is dismissed
        });

        box.getChildren().addAll(icon, msg, okBtn);

        Scene scene = new Scene(box);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);

        popup.show();

        // Centre over owner
        Stage owner = (Stage) btnCancel.getScene().getWindow();
        popup.setX(owner.getX() + (owner.getWidth()  / 2) - (popup.getWidth()  / 2));
        popup.setY(owner.getY() + (owner.getHeight() / 2) - (popup.getHeight() / 2));

        box.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(200), box);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    @FXML
    private void handleCancel() { closeWindow(); }

    // ── Border helpers ───────────────────────────────────────────────────────
    private void markError(Control control) {
        // Set a clean error border — don't append to existing style to avoid conflicts
        control.setStyle(
                "-fx-border-color: #e53935;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-color: rgba(229,57,53,0.06);");
    }

    private void clearError(Control control) {
        if (control.getStyle() != null && control.getStyle().contains("#e53935")) {
            control.setStyle("");
        }
        // Only hide error label if ALL fields are now valid
        boolean allClear = !firstNameField.getStyle().contains("#e53935")
                && !lastNameField.getStyle().contains("#e53935")
                && !trainingTypeCombo.getStyle().contains("#e53935");
        if (allClear) hideError();
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}