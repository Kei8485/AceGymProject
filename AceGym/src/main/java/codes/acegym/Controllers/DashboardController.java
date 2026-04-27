package codes.acegym.Controllers;

import codes.acegym.DB.DashboardDAO;
import codes.acegym.DB.RenewalNotificationService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
    // MAIN LOAD
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
                renewalListContainer.getChildren().add(
                        buildRenewalRow(row.name(), row.email(), row.expiryDate(),
                                row.daysLeftRaw(), row.daysLeft())
                );
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
    // RENEWAL ROW — with notify button
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds one renewal row containing:
     *  • Member name + expiry date (left)
     *  • Days-left badge (center-right)
     *  • ✉ Notify button (far right)
     *
     * The button runs the email send on a background thread so the UI
     * never freezes. States: default → "Sending…" → "✔ Sent" or "✘ Failed"
     */
    private HBox buildRenewalRow(String name, String email,
                                 String expiryDate, int daysLeftRaw,
                                 String daysLeftLabel) {
        HBox row = new HBox(10);
        row.getStyleClass().add("renewal-row");
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setAlignment(Pos.CENTER_LEFT);

        // ── Left: name + date ────────────────────────────────────────────────
        VBox info = new VBox(2);
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("renewal-name");
        Label dateLbl = new Label(expiryDate);
        dateLbl.getStyleClass().add("renewal-date");
        info.getChildren().addAll(nameLabel, dateLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // ── Days badge ───────────────────────────────────────────────────────
        Label badge = new Label(daysLeftLabel);
        badge.getStyleClass().add("renewal-badge");

        // ── Notify button ────────────────────────────────────────────────────
        Button notifyBtn = new Button("✉ Notify");
        notifyBtn.getStyleClass().add("notify-btn");

        boolean noEmail = (email == null || email.isBlank() || email.equals("—"));
        if (noEmail) {
            notifyBtn.setText("No Email");
            notifyBtn.getStyleClass().add("notify-btn-disabled");
            notifyBtn.setDisable(true);
        } else {
            notifyBtn.setOnAction(e -> sendEmailAsync(notifyBtn, name, email, expiryDate, daysLeftRaw));
        }

        row.getChildren().addAll(info, spacer, badge, notifyBtn);
        VBox.setMargin(row, new Insets(0, 0, 6, 0));
        return row;
    }

    /**
     * Sends the email on a daemon thread. Updates button text on the FX thread.
     * Button is disabled during send to prevent double-clicks.
     */
    private void sendEmailAsync(Button btn, String name, String email,
                                String expiryDate, int daysLeftRaw) {
        btn.setDisable(true);
        btn.setText("Sending…");
        btn.getStyleClass().removeAll("notify-btn-success", "notify-btn-fail");

        Thread worker = new Thread(() -> {
            boolean ok = RenewalNotificationService.sendToOne(name, email, expiryDate, daysLeftRaw);
            Platform.runLater(() -> {
                if (ok) {
                    btn.setText("✔ Sent");
                    btn.getStyleClass().add("notify-btn-success");
                } else {
                    btn.setText("✘ Failed");
                    btn.getStyleClass().add("notify-btn-fail");
                    btn.setDisable(false); // allow retry on failure
                }
            });
        });
        worker.setDaemon(true);
        worker.start();
    }

    // ── kept for backward compatibility ──────────────────────────────────────
    public void addRenewalRow(String name, String date, String daysLeft) {
        renewalListContainer.getChildren().add(
                buildRenewalRow(name, null, date, 0, daysLeft)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MEMBER ROW
    // ─────────────────────────────────────────────────────────────────────────
    public void addMemberRow(String name, String enrolled, String status) {
        HBox row = new HBox();
        row.getStyleClass().add("mini-table-row");
        row.setPadding(new Insets(10, 10, 10, 10));
        row.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("mini-row-name");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

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

    // ─────────────────────────────────────────────────────────────────────────
    // COACH ROW
    // ─────────────────────────────────────────────────────────────────────────
    public void addCoachRow(String name, String coachId, String clientCount) {
        HBox row = new HBox();
        row.getStyleClass().add("mini-table-row");
        row.setPadding(new Insets(10, 10, 10, 10));
        row.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("mini-row-name");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

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
    private String formatPeso(double amount) {
        return String.format("₱%,.2f", amount);
    }

    private Label emptyStateLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("empty-state-label");
        lbl.setPadding(new Insets(12));
        return lbl;
    }
}