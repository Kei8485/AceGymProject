package codes.acegym.Controllers;

import codes.acegym.DB.DashboardDAO;
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
import java.util.List;

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

    // ── Call this from any other controller to force a refresh ──────────────
    public void refresh() {
        loadDashboardData();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLOCK
    // ─────────────────────────────────────────────────────────────────────────
    private void startClock() {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d");

        dateLabel.setText(LocalDate.now().format(dateFormatter));

        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                clockLabel.setText(LocalTime.now().format(timeFormatter))
        ));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAIN LOAD — calls DB and populates every widget
    // ─────────────────────────────────────────────────────────────────────────
    private void loadDashboardData() {

        // ── Stat cards ───────────────────────────────────────────────────────
        totalMembersLabel.setText(String.valueOf(DashboardDAO.getTotalMembers()));
        newMembersLabel.setText(String.valueOf(DashboardDAO.getNewMembersThisMonth()));
        renewalsDueLabel.setText(String.valueOf(DashboardDAO.getRenewalsDueCount()));
        coachesLabel.setText(String.valueOf(DashboardDAO.getTotalCoaches()));

        // ── Revenue ──────────────────────────────────────────────────────────
        monthlySalesLabel.setText(formatPeso(DashboardDAO.getMonthlySales()));
        cashSalesLabel.setText(formatPeso(DashboardDAO.getCashSalesThisMonth()));
        digitalSalesLabel.setText(formatPeso(DashboardDAO.getDigitalSalesThisMonth()));
        totalTransactionsLabel.setText(String.valueOf(DashboardDAO.getTransactionCountThisMonth()));
        totalSalesLabel.setText(formatPeso(DashboardDAO.getTotalSalesAllTime()));

        // ── Renewals Due list ────────────────────────────────────────────────
        renewalListContainer.getChildren().clear();
        List<DashboardDAO.RenewalRow> renewals = DashboardDAO.getRenewalsDue();
        if (renewals.isEmpty()) {
            renewalListContainer.getChildren().add(emptyStateLabel("No renewals due in the next 7 days."));
        } else {
            for (DashboardDAO.RenewalRow row : renewals) {
                addRenewalRow(row.name(), row.expiryDate(), row.daysLeft());
            }
        }

        // ── Recent Members list ───────────────────────────────────────────────
        memberListContainer.getChildren().clear();
        List<DashboardDAO.MemberSummaryRow> members = DashboardDAO.getRecentMembers();
        if (members.isEmpty()) {
            memberListContainer.getChildren().add(emptyStateLabel("No members found."));
        } else {
            for (DashboardDAO.MemberSummaryRow row : members) {
                addMemberRow(row.name(), row.enrolled(), row.status());
            }
        }

        // ── Coaches list ──────────────────────────────────────────────────────
        coachListContainer.getChildren().clear();
        List<DashboardDAO.CoachSummaryRow> coaches = DashboardDAO.getCoachSummaries();
        if (coaches.isEmpty()) {
            coachListContainer.getChildren().add(emptyStateLabel("No coaches found."));
        } else {
            for (DashboardDAO.CoachSummaryRow row : coaches) {
                addCoachRow(row.name(), row.coachId(), row.clientCount());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ROW BUILDERS  (unchanged from original — keep your existing CSS classes)
    // ─────────────────────────────────────────────────────────────────────────

    public void addRenewalRow(String name, String date, String daysLeft) {
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

    public void addMemberRow(String name, String enrolled, String status) {
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
        statusBadge.getStyleClass().add(
                "Active".equalsIgnoreCase(status) ? "badge-active" : "badge-expired");
        statusBadge.setPrefWidth(80);

        row.getChildren().addAll(nameLabel, enrolledLabel, statusBadge);
        memberListContainer.getChildren().add(row);
    }

    public void addCoachRow(String name, String coachId, String clientCount) {
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

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /** Formats a double as ₱1,234.50 */
    private String formatPeso(double amount) {
        return String.format("₱%,.2f", amount);
    }

    /** Placeholder label shown when a list is empty. */
    private Label emptyStateLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("empty-state-label");
        lbl.setPadding(new Insets(12));
        return lbl;
    }
}