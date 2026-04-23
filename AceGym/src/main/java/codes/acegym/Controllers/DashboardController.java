package codes.acegym.Controllers;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DashboardController {

    // ── Stat Cards ──
    @FXML private Label totalMembersLabel;
    @FXML private Label newMembersLabel;
    @FXML private Label renewalsDueLabel;
    @FXML private Label coachesLabel;

    // ── Revenue ──
    @FXML private Label monthlySalesLabel;
    @FXML private Label cashSalesLabel;
    @FXML private Label digitalSalesLabel;
    @FXML private Label totalTransactionsLabel;
    @FXML private Label totalSalesLabel;

    // ── Lists ──
    @FXML private VBox renewalListContainer;
    @FXML private VBox memberListContainer;
    @FXML private VBox coachListContainer;

    // ── Clock ──
    @FXML private Label clockLabel;
    @FXML private Label dateLabel;

    public void initialize() {
        startClock();
        loadDashboardData();
    }

    private void startClock() {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d");

        dateLabel.setText(LocalDate.now().format(dateFormatter));

        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            clockLabel.setText(LocalTime.now().format(timeFormatter));
        }));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void loadDashboardData() {
        // TODO: Replace with actual DB calls

        // ── Stat cards ──
        totalMembersLabel.setText("0");
        newMembersLabel.setText("0");
        renewalsDueLabel.setText("0");
        coachesLabel.setText("0");

        // ── Revenue ──
        monthlySalesLabel.setText("₱0.00");
        cashSalesLabel.setText("₱0.00");
        digitalSalesLabel.setText("₱0.00");
        totalTransactionsLabel.setText("0");
        totalSalesLabel.setText("₱0.00");

        // ── Renewals Due List ──
        // Example of how to add a renewal row programmatically:
        // addRenewalRow("Juan Dela Cruz", "Expires Apr 25", "3 days");

        // ── Members List ──
        // Example of how to add a member row:
        // addMemberRow("Juan Dela Cruz", "Apr 10, 2025", "Active");

        // ── Coaches List ──
        // Example:
        // addCoachRow("Coach Mike", "C-001", "5");
    }

    // ── Renewal Row Builder ──
    public void addRenewalRow(String name, String date, String daysLeft) {
        renewalListContainer.getChildren().clear(); // remove empty state on first add

        HBox row = new HBox(10);
        row.getStyleClass().add("renewal-row");
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox info = new VBox(2);
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("renewal-name");
        Label dateLabel = new Label(date);
        dateLabel.getStyleClass().add("renewal-date");
        info.getChildren().addAll(nameLabel, dateLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label badge = new Label(daysLeft);
        badge.getStyleClass().add("renewal-badge");

        row.getChildren().addAll(info, spacer, badge);
        VBox.setMargin(row, new Insets(0, 0, 6, 0));
        renewalListContainer.getChildren().add(row);
    }

    // ── Member Row Builder ──
    public void addMemberRow(String name, String enrolled, String status) {
        memberListContainer.getChildren().clear();

        HBox row = new HBox();
        row.getStyleClass().add("mini-table-row");
        row.setPadding(new Insets(10, 10, 10, 10));
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("mini-row-name");
        HBox.setHgrow(nameLabel, javafx.scene.layout.Priority.ALWAYS);

        Label enrolledLabel = new Label(enrolled);
        enrolledLabel.getStyleClass().add("mini-row-value");
        enrolledLabel.setPrefWidth(100);

        Label statusBadge = new Label(status);
        statusBadge.getStyleClass().add(status.equalsIgnoreCase("Active") ? "badge-active" : "badge-expired");
        statusBadge.setPrefWidth(80);

        row.getChildren().addAll(nameLabel, enrolledLabel, statusBadge);
        memberListContainer.getChildren().add(row);
    }

    // ── Coach Row Builder ──
    public void addCoachRow(String name, String coachId, String clientCount) {
        coachListContainer.getChildren().clear();

        HBox row = new HBox();
        row.getStyleClass().add("mini-table-row");
        row.setPadding(new Insets(10, 10, 10, 10));
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("mini-row-name");
        HBox.setHgrow(nameLabel, javafx.scene.layout.Priority.ALWAYS);

        Label idLabel = new Label(coachId);
        idLabel.getStyleClass().add("mini-row-value");
        idLabel.setPrefWidth(90);

        Label clientLabel = new Label(clientCount);
        clientLabel.getStyleClass().add("mini-row-value");
        clientLabel.setPrefWidth(70);

        row.getChildren().addAll(nameLabel, idLabel, clientLabel);
        coachListContainer.getChildren().add(row);
    }
}