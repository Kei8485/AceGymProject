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
        coachContainer.getChildren().clear();

        try {

            Node card = FXMLLoader.load(getClass().getResource("CoachCard.fxml"));

            coachContainer.getChildren().add(card);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Check if CoachCard.fxml is in the same folder as this class.");
        }
    }
}