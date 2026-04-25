package codes.acegym.Controllers;

import codes.acegym.DB.CoachDAO;
import codes.acegym.Objects.Coach;
import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
import java.util.ArrayList;
import java.util.List;

public class CoachesController implements Refreshable {

    @FXML private TilePane  coachContainer;
    @FXML private Button    addCoachBtn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCombo;

    private ObservableList<Coach> allCoaches;

    @Override
    public void refreshData() {
        coachContainer.getChildren().clear();

        javafx.animation.PauseTransition delay =
                new javafx.animation.PauseTransition(Duration.millis(350));

        delay.setOnFinished(e -> loadCoachesAsync());
        delay.play();
    }

    private void loadCoachesAsync() {
        Task<List<Node>> loadTask = new Task<>() {
            @Override
            protected List<Node> call() throws Exception {
                allCoaches = CoachDAO.getAllCoaches();

                List<Node> cards = new ArrayList<>(allCoaches.size());
                for (Coach coach : allCoaches) {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/codes/acegym/CoachCard.fxml"));
                    Node card = loader.load();
                    CoachCardController ctrl = loader.getController();
                    ctrl.setCoach(coach);
                    ctrl.setOnDeleteCallback(
                            () -> Platform.runLater(CoachesController.this::refreshData));
                    cards.add(card);
                }
                return cards;
            }
        };

        loadTask.setOnSucceeded(e -> animateCardsIn(loadTask.getValue()));
        loadTask.setOnFailed(e -> loadTask.getException().printStackTrace());

        Thread t = new Thread(loadTask);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void initialize() {
        setupFilterCombo();
        refreshData();

        if (addCoachBtn != null)
            addCoachBtn.setOnAction(e -> openAddCoachModal());

        if (searchField != null)
            searchField.textProperty().addListener((obs, old, val) -> filterCards());
    }

    private void setupFilterCombo() {
        if (filterCombo == null) return;
        filterCombo.getItems().clear();
        filterCombo.getItems().add("All");
        filterCombo.getItems().add("First Name");
        filterCombo.getItems().add("Last Name");
        filterCombo.getItems().add("── Categories ──");

        for (String[] type : CoachDAO.getTrainingTypes()) {
            filterCombo.getItems().add(type[1]);
        }

        filterCombo.setValue("All");
        filterCombo.setOnAction(e -> {
            String val = filterCombo.getValue();
            if (val != null && val.startsWith("──")) {
                filterCombo.setValue("All");
                return;
            }
            filterCards();
        });
    }

    private void renderCoaches(ObservableList<Coach> coaches) {
        coachContainer.getChildren().clear();

        if (coaches.isEmpty()) {
            Label empty = new Label("No coaches found.");
            empty.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 14px;" +
                    "-fx-font-style: italic; -fx-padding: 40 0 0 0;");
            coachContainer.getChildren().add(empty);
            return;
        }

        Task<List<Node>> task = new Task<>() {
            @Override
            protected List<Node> call() throws Exception {
                List<Node> cards = new ArrayList<>(coaches.size());
                for (Coach coach : coaches) {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/codes/acegym/CoachCard.fxml"));
                    Node card = loader.load();
                    CoachCardController ctrl = loader.getController();
                    ctrl.setCoach(coach);
                    ctrl.setOnDeleteCallback(
                            () -> Platform.runLater(CoachesController.this::refreshData));
                    cards.add(card);
                }
                return cards;
            }
        };

        task.setOnSucceeded(e -> animateCardsIn(task.getValue()));
        task.setOnFailed(e -> task.getException().printStackTrace());

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void filterCards() {
        if (allCoaches == null) return;

        String query  = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        String filter = filterCombo != null ? filterCombo.getValue() : "All";
        if (filter == null) filter = "All";

        ObservableList<Coach> filtered =
                javafx.collections.FXCollections.observableArrayList();

        for (Coach c : allCoaches) {
            boolean categoryMatch = true;
            if (!"All".equals(filter) && !"First Name".equals(filter)
                    && !"Last Name".equals(filter)) {
                String cat = c.getTrainingCategory();
                categoryMatch = cat != null && cat.equalsIgnoreCase(filter);
            }
            if (!categoryMatch) continue;

            if (query.isEmpty()) { filtered.add(c); continue; }

            String cat = c.getTrainingCategory() != null ? c.getTrainingCategory() : "";
            boolean textMatch = switch (filter) {
                case "First Name" -> c.getFirstName().toLowerCase().contains(query);
                case "Last Name"  -> c.getLastName().toLowerCase().contains(query);
                default -> c.getFullName().toLowerCase().contains(query)
                        || cat.toLowerCase().contains(query);
            };
            if (textMatch) filtered.add(c);
        }

        renderCoaches(filtered);
    }

    private void animateCardsIn(List<Node> cards) {
        coachContainer.getChildren().clear();

        for (Node card : cards) {
            card.setOpacity(0);
            card.setTranslateY(20);
        }

        coachContainer.getChildren().addAll(cards);

        for (int i = 0; i < cards.size(); i++) {
            Node card = cards.get(i);
            int index = i;

            javafx.animation.PauseTransition delay =
                    new javafx.animation.PauseTransition(Duration.millis(index * 40L));

            delay.setOnFinished(e -> {
                FadeTransition fade = new FadeTransition(Duration.millis(250), card);
                fade.setFromValue(0);
                fade.setToValue(1);

                javafx.animation.TranslateTransition slide =
                        new javafx.animation.TranslateTransition(Duration.millis(250), card);
                slide.setFromY(20);
                slide.setToY(0);
                slide.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

                javafx.animation.ParallelTransition anim =
                        new javafx.animation.ParallelTransition(fade, slide);
                anim.play();
            });

            delay.play();
        }
    }

    private void openAddCoachModal() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/codes/acegym/AddCoach.fxml"));
            Parent root = loader.load();
            AddCoachController ctrl = loader.getController();
            ctrl.setOnCoachAdded(this::refreshData);

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