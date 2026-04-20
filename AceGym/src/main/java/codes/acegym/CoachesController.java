package codes.acegym;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.TilePane;
import java.io.IOException;

public class CoachesController {

    @FXML private TilePane coachContainer;

    @FXML
    public void initialize() {
        // Clear the container to start fresh
        coachContainer.getChildren().clear();

        try {
            // Just load the file and add it.
            // It will show exactly what you see in Scene Builder.
            Node card = FXMLLoader.load(getClass().getResource("CoachCard.fxml"));

            coachContainer.getChildren().add(card);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Check if CoachCard.fxml is in the same folder as this class.");
        }
    }
}