package codes.acegym.Controllers;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.TilePane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.io.IOException;
import java.util.Objects;

public class CoachesController {

    @FXML private TilePane coachContainer;
    @FXML private Button addCoachBtn;

    @FXML
    public void initialize() {
        // Load initial cards
        loadCoachCards();

        // Setup Add Coach Button
        if (addCoachBtn != null) {
            addCoachBtn.setOnAction(e -> openAddCoachModal());
        }
    }

    private void loadCoachCards() {
        coachContainer.getChildren().clear();
        try {
            // Loading a single card as a placeholder
            Node card = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/codes/acegym/CoachCard.fxml")));
            coachContainer.getChildren().add(card);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openAddCoachModal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/codes/acegym/AddCoach.fxml"));
            Parent root = loader.load();

            Stage modalStage = new Stage();
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.initModality(Modality.APPLICATION_MODAL);

            // Get the main window stage to apply the blur
            Stage owner = (Stage) addCoachBtn.getScene().getWindow();
            modalStage.initOwner(owner);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            modalStage.setScene(scene);

            // Apply Background Blur
            GaussianBlur blur = new GaussianBlur(15);
            owner.getScene().getRoot().setEffect(blur);

            // Center the modal
            centerStage(modalStage, owner);

            // Fade-in Animation
            root.setOpacity(0);
            modalStage.show();

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            // Remove blur when closed
            modalStage.setOnHidden(e -> owner.getScene().getRoot().setEffect(null));

        } catch (IOException e) {
            System.err.println("Error loading AddCoach.fxml");
            e.printStackTrace();
        }
    }

    private void centerStage(Stage stage, Stage owner) {
        stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            stage.setX(owner.getX() + (owner.getWidth() / 2) - (newVal.doubleValue() / 2));
        });
        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            stage.setY(owner.getY() + (owner.getHeight() / 2) - (newVal.doubleValue() / 2));
        });
    }
}