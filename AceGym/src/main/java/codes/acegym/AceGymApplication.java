package codes.acegym;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AceGymApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(AceGymApplication.class.getResource("Login.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.setTitle("Ace Fitness Gym System");
        stage.initStyle(StageStyle.UNDECORATED);
        stage.centerOnScreen();
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.setWidth(1200);
        stage.setHeight(800);
        stage.setMaximized(true);
        stage.show();
    }

}
