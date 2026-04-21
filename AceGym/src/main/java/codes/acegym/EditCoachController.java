package codes.acegym;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;

public class EditCoachController {

    @FXML private ImageView coachImageView;
    @FXML private TextField editUsername, editContact, editEmail;
    @FXML private Button btnSave, btnCancel;

    @FXML
    public void initialize() {
        // Create a circular clip to keep the image round
        Circle clip = new Circle(55, 55, 55);
        coachImageView.setClip(clip);

        // Dummy initial data
        editUsername.setText("JuanDelaCruz");
        editContact.setText("09123456789");
        editEmail.setText("juan@email.com");
    }

    @FXML
    private void handleChangeProfile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Change Profile Picture");
        fileChooser.getExtensionFilters().add(
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
    private void handleSave() {
        // 1. Capture the data from the underlined fields
        String newUsername = editUsername.getText();
        String newContact = editContact.getText();
        String newEmail = editEmail.getText();

        // 2. Here you would normally update your database
        System.out.println("Saving Profile Updates...");
        System.out.println("Username: " + newUsername);
        System.out.println("Contact: " + newContact);

        // 3. Close the modal after saving
        closeStage();
    }

    @FXML
    private void handleCancel() {
        // Simply close the window without saving changes
        closeStage();
    }

    private void closeStage() {
        // Helper method to get the current stage and close it
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}