package codes.acegym.Controllers;

import codes.acegym.DB.CoachDAO;
import codes.acegym.Objects.Coach;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;

public class EditCoachController {

    @FXML private ImageView        coachImageView;
    @FXML private TextField        editFirstName;
    @FXML private TextField        editLastName;
    @FXML private ComboBox<String> editTrainingType;
    @FXML private Button           btnSave;
    @FXML private Button           btnCancel;
    @FXML private Label            errorLabel;

    private Coach               coach;
    private CoachCardController cardController;
    private String              selectedImagePath;

    public void setCoach(Coach coach, CoachCardController cardController) {
        this.coach          = coach;
        this.cardController = cardController;
        populateFields();
    }

    @FXML
    public void initialize() {
        Circle clip = new Circle(55, 55, 55);
        coachImageView.setClip(clip);

        // Load training types from DB into combo
        if (editTrainingType != null) {
            for (String[] type : CoachDAO.getTrainingTypes()) {
                editTrainingType.getItems().add(type[0] + " | " + type[1]);
            }
        }

        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }

        // Clear red border on typing / selection
        if (editFirstName    != null) editFirstName.textProperty().addListener((o, ov, nv) -> clearError(editFirstName));
        if (editLastName     != null) editLastName.textProperty().addListener((o, ov, nv)  -> clearError(editLastName));
        if (editTrainingType != null) editTrainingType.valueProperty().addListener((o, ov, nv) -> clearError(editTrainingType));
    }

    // ── Pre-populate all fields with the coach's existing data ───────────────
    private void populateFields() {
        if (coach == null) return;

        if (editFirstName != null) editFirstName.setText(coach.getFirstName());
        if (editLastName  != null) editLastName.setText(coach.getLastName());

        // Match the training type in the combo by ID prefix
        if (editTrainingType != null) {
            for (String item : editTrainingType.getItems()) {
                if (item.startsWith(coach.getTrainingTypeID() + " |")) {
                    editTrainingType.setValue(item);
                    break;
                }
            }
        }

        // Load the existing image using the stored file path
        String imgPath = coach.getStaffImage();
        if (imgPath != null && !imgPath.isBlank()) {
            try {
                // Try as a file path first, then as URI
                File imgFile = new File(imgPath);
                if (imgFile.exists()) {
                    coachImageView.setImage(new Image(imgFile.toURI().toString()));
                } else {
                    coachImageView.setImage(new Image(imgPath, true));
                }
                selectedImagePath = imgPath;
            } catch (Exception ignored) {
                loadDefaultImage();
            }
        } else {
            loadDefaultImage();
        }
    }

    private void loadDefaultImage() {
        try {
            coachImageView.setImage(new Image(
                    getClass().getResourceAsStream("/image/admin-user.png")));
        } catch (Exception ignored) {}
    }

    @FXML
    private void handleChangeProfile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Change Profile Picture");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) coachImageView.getScene().getWindow();
        File file = fc.showOpenDialog(stage);
        if (file != null) {
            // Store absolute path so DB can reload it next launch
            selectedImagePath = file.getAbsolutePath();
            coachImageView.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    private void handleSave() {
        if (coach == null) return;

        String fn = editFirstName != null ? editFirstName.getText().trim() : "";
        String ln = editLastName  != null ? editLastName.getText().trim()  : "";
        boolean valid = true;

        // ── Validation ──
        if (fn.isEmpty()) { markError(editFirstName); valid = false; }
        if (ln.isEmpty()) { markError(editLastName);  valid = false; }
        if (editTrainingType != null && editTrainingType.getValue() == null) {
            markError(editTrainingType);
            valid = false;
        }

        if (!valid) {
            showError("Please fill in all required fields.");
            return;
        }

        int typeID = 0;
        if (editTrainingType != null && editTrainingType.getValue() != null) {
            String val = editTrainingType.getValue();
            if (val.contains("|")) {
                try { typeID = Integer.parseInt(val.split("\\|")[0].trim()); }
                catch (NumberFormatException ignored) {}
            }
        }

        boolean ok = CoachDAO.updateCoach(
                coach.getStaffID(), fn, ln, typeID, selectedImagePath);

        if (ok) {
            // Update the in-memory coach so card refreshes correctly without re-query
            coach.setFirstName(fn);
            coach.setLastName(ln);
            coach.setTrainingTypeID(typeID);
            if (selectedImagePath != null) coach.setStaffImage(selectedImagePath);

            if (cardController != null) cardController.refreshCard();
            closeStage();
        } else {
            showError("Failed to save changes. Please try again.");
        }
    }

    @FXML
    private void handleCancel() { closeStage(); }

    // ── Border helpers ───────────────────────────────────────────────────────
    private void markError(Control control) {
        control.setStyle(
                "-fx-border-color: #e53935; -fx-border-width: 2; -fx-border-radius: 6;");
    }

    private void clearError(Control control) {
        control.setStyle("");
        hideError();
    }

    private void showError(String msg) {
        if (errorLabel != null) {
            errorLabel.setText(msg);
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

    private void closeStage() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}