package codes.acegym.Application_Launcher;

import codes.acegym.DB.DBConnector;
import codes.acegym.Session;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Objects;
import java.util.prefs.Preferences;
import javafx.scene.text.Font;
public class AceGymApplication extends Application {

    // ── Preferences keys ────────────────────────────────────────────────────
    public static final String PREF_NODE      = "codes/acegym";
    public static final String KEY_REMEMBER   = "rememberMe";
    public static final String KEY_USERNAME   = "savedUsername";
    public static final String KEY_PASSWORD   = "savedPassword";

    @Override
    public void start(Stage stage) throws Exception {


        Font.loadFont(getClass().getResourceAsStream("/Font/Bebas_Neue/BebasNeue-Regular.ttf"), 12);
        Font.loadFont(getClass().getResourceAsStream("/Font/Inter/Inter-VariableFont_opsz,wght.ttf"), 12);

        stage.getIcons().add(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/image/logoo.png"))));
        stage.setTitle("Ace Fitness Gym System");
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setMaximized(true);

        // ── Check if Remember Me was previously saved ────────────────────────
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        boolean remember  = prefs.getBoolean(KEY_REMEMBER, false);

        if (remember) {
            String savedUser = prefs.get(KEY_USERNAME, "");
            String savedPass = prefs.get(KEY_PASSWORD, "");

            if (!savedUser.isBlank() && !savedPass.isBlank()) {
                boolean valid = DBConnector.login(savedUser, savedPass);

                if (valid) {
                    // ✅ Credentials still valid — go straight to HomePage
                    Session.getInstance().setLoggedInUsername(savedUser);
                    FXMLLoader loader = new FXMLLoader(
                            AceGymApplication.class.getResource("/codes/acegym/HomePage.fxml"));
                    Parent root = loader.load();
                    stage.setScene(new Scene(root));
                    stage.show();
                    return; // skip login screen entirely
                } else {
                    // Saved credentials no longer valid — clear them and show login
                    prefs.putBoolean(KEY_REMEMBER, false);
                    prefs.remove(KEY_USERNAME);
                    prefs.remove(KEY_PASSWORD);
                }
            }
        }

        // ── Default: show login screen ───────────────────────────────────────
        FXMLLoader fxmlLoader = new FXMLLoader(
                AceGymApplication.class.getResource("/codes/acegym/Login.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.show();
    }
}