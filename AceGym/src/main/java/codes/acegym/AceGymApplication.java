package codes.acegym;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Objects;

public class AceGymApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader fxmlLoader =
                new FXMLLoader(AceGymApplication.class.getResource("Login.fxml"));

        Scene scene = new Scene(fxmlLoader.load());

        stage.setScene(scene);

        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/logoo.png"))));


        stage.setTitle("Ace Fitness Gym System");

        stage.initStyle(StageStyle.UNDECORATED);

        stage.setMaximized(true); // key line

        stage.show();
    }
}
