package codes.acegym;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;

public class AddCoachController {

    @FXML private ImageView coachImageView;
    @FXML private TextField firstNameField, lastNameField;
    @FXML private ComboBox<String> trainingTypeCombo;
    @FXML private Button btnCancel, btnAdd;

    @FXML
    public void initialize() {
        // 1. Create circular mask for the profile image
        Circle clip = new Circle(55, 55, 55);
        coachImageView.setClip(clip);

        // 2. ComboBox is empty as requested, but here is where you'd
        // eventually load your Training Type IDs (e.g., TT001, TT002)
    }

    @FXML
    private void handleChangeProfile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) coachImageView.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            Image image = new Image(selectedFile.toURI().toString());
            coachImageView.setImage(image);
        }
    }

    @FXML
    private void handleAddCoach() {
        // Capture data from fields
        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String trainingType = trainingTypeCombo.getValue();

        // Database logic will go here later
        System.out.println("Registering: " + firstName + " " + lastName);
        System.out.println("Type: " + trainingType);

        closeWindow();
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        // Closes the modal and triggers the background blur removal
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}