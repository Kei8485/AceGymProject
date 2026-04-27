package codes.acegym.Controllers;

import codes.acegym.DB.CoachDAO;
import codes.acegym.ModalHelper;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
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

        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }

        // Clear red border on typing
        firstNameField.textProperty().addListener((o, oldV, newV) -> clearError(firstNameField));
        lastNameField.textProperty().addListener((o, oldV, newV)  -> clearError(lastNameField));
        trainingTypeCombo.valueProperty().addListener((o, oldV, newV) -> clearError(trainingTypeCombo));

        // DB call off the FX thread — getTrainingTypes() hits MySQL and was
        // blocking the modal from appearing until the query finished.
        Task<ObservableList<String[]>> task = new Task<>() {
            @Override protected ObservableList<String[]> call() {
                return CoachDAO.getTrainingTypes();
            }
        };
        task.setOnSucceeded(e -> {
            for (String[] type : task.getValue())
                trainingTypeCombo.getItems().add(type[0] + " | " + type[1]);
        });
        task.setOnFailed(e -> task.getException().printStackTrace());
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
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

    // ── Success popup — delegates to ModalHelper (Stage built once, reused) ──
    private void showSuccessPopup(String message) {
        Stage owner = (Stage) btnCancel.getScene().getWindow();
        // Re-use ModalHelper's confirm popup: Confirm = OK, which closes the form.
        // The "cancel" path on the popup does nothing, which is fine for a success notice.
        ModalHelper.get().showConfirm(message, this::closeWindow, owner);
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