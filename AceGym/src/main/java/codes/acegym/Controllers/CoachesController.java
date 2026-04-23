package codes.acegym.Controllers;

import codes.acegym.DB.CoachDAO;
import codes.acegym.Objects.Coach;
import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.TilePane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.io.IOException;

public class CoachesController {

    @FXML private TilePane  coachContainer;
    @FXML private Button    addCoachBtn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCombo;   // add fx:id="filterCombo" in Coaches.fxml

    private ObservableList<Coach> allCoaches;

    @FXML
    public void initialize() {
        setupFilterCombo();
        loadCoachCards();

        if (addCoachBtn != null)
            addCoachBtn.setOnAction(e -> openAddCoachModal());

        if (searchField != null)
            searchField.textProperty().addListener((obs, old, val) -> filterCards());
    }

    // ── Filter combo setup — loads real training categories from DB ─────────
    private void setupFilterCombo() {
        if (filterCombo == null) return;

        filterCombo.getItems().add("All");
        filterCombo.getItems().add("First Name");
        filterCombo.getItems().add("Last Name");
        // Separator label to distinguish name filters from category filters
        filterCombo.getItems().add("── Categories ──");
        // Load real training categories from DB
        for (String[] type : CoachDAO.getTrainingTypes()) {
            filterCombo.getItems().add(type[1]); // e.g. "Lifting", "Both"
        }

        filterCombo.setValue("All");
        filterCombo.setStyle(
                "-fx-background-color: #1c2237;" +
                        "-fx-border-color: #2e3349;" +
                        "-fx-border-radius: 8;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-text-fill: #e8e8f0;" +
                        "-fx-font-size: 12px;");
        filterCombo.setOnAction(e -> {
            // Prevent selecting the separator
            String val = filterCombo.getValue();
            if (val != null && val.startsWith("──")) {
                filterCombo.setValue("All");
                return;
            }
            filterCards();
        });
    }

    // ── Load all coach cards from DB ─────────────────────────────────────────
    public void loadCoachCards() {
        coachContainer.getChildren().clear();
        allCoaches = CoachDAO.getAllCoaches();
        renderCoaches(allCoaches);
    }

    private void renderCoaches(ObservableList<Coach> coaches) {
        coachContainer.getChildren().clear();

        if (coaches.isEmpty()) {
            Label empty = new Label("No coaches found.");
            empty.setStyle(
                    "-fx-text-fill: #6b7280; -fx-font-size: 14px;" +
                            "-fx-font-style: italic; -fx-padding: 40 0 0 0;");
            coachContainer.getChildren().add(empty);
            return;
        }

        for (Coach coach : coaches) {
            addCoachCard(coach);
        }
    }

    private void addCoachCard(Coach coach) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/codes/acegym/CoachCard.fxml"));
            Node card = loader.load();

            CoachCardController ctrl = loader.getController();
            ctrl.setCoach(coach);
            ctrl.setOnDeleteCallback(this::loadCoachCards);

            coachContainer.getChildren().add(card);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Live search + filter ─────────────────────────────────────────────────
    private void filterCards() {
        if (allCoaches == null) return;

        String query  = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        String filter = filterCombo != null ? filterCombo.getValue() : "All";
        if (filter == null) filter = "All";

        ObservableList<Coach> filtered = javafx.collections.FXCollections.observableArrayList();

        for (Coach c : allCoaches) {
            // ── Category filter (anything not All/First Name/Last Name) ──
            boolean categoryMatch = true;
            if (!"All".equals(filter) && !"First Name".equals(filter) && !"Last Name".equals(filter)) {
                String cat = c.getTrainingCategory();
                categoryMatch = cat != null && cat.equalsIgnoreCase(filter);
            }
            if (!categoryMatch) continue;

            // ── Text search ──
            if (query.isEmpty()) {
                filtered.add(c);
                continue;
            }

            String cat = c.getTrainingCategory() != null ? c.getTrainingCategory() : "";
            boolean textMatch;
            switch (filter) {
                case "First Name" -> textMatch = c.getFirstName().toLowerCase().contains(query);
                case "Last Name"  -> textMatch = c.getLastName().toLowerCase().contains(query);
                default           -> textMatch = c.getFullName().toLowerCase().contains(query)
                        || cat.toLowerCase().contains(query);
            }
            if (textMatch) filtered.add(c);
        }

        renderCoaches(filtered);
    }

    // ── Open AddCoach modal ──────────────────────────────────────────────────
    private void openAddCoachModal() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/codes/acegym/AddCoach.fxml"));
            Parent root = loader.load();

            AddCoachController ctrl = loader.getController();
            ctrl.setOnCoachAdded(this::loadCoachCards);

            Stage modalStage = new Stage();
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.initModality(Modality.APPLICATION_MODAL);

            Stage owner = (Stage) addCoachBtn.getScene().getWindow();
            modalStage.initOwner(owner);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            modalStage.setScene(scene);

            owner.getScene().getRoot().setEffect(new GaussianBlur(15));
            modalStage.show();

            // Center AFTER show() so width/height are known
            Platform.runLater(() -> centerStage(modalStage, owner));

            root.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(300), root);
            ft.setFromValue(0); ft.setToValue(1); ft.play();

            modalStage.setOnHidden(e -> owner.getScene().getRoot().setEffect(null));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void centerStage(Stage stage, Stage owner) {
        stage.setX(owner.getX() + (owner.getWidth()  / 2) - (stage.getWidth()  / 2));
        stage.setY(owner.getY() + (owner.getHeight() / 2) - (stage.getHeight() / 2));
    }
}