package codes.acegym.Controllers;

import codes.acegym.DB.*;
import codes.acegym.Objects.Client;
import codes.acegym.Objects.UpgradeResult;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class PaymentController implements Initializable {

    // ── Step 1: Client ───────────────────────────────────────────────────────
    @FXML private ComboBox<Client> clientSearchCombo;
    @FXML private TextField        clientIdDisplay;
    @FXML private TextField        clientNameDisplay;
    @FXML private TextField        clientContactDisplay;
    @FXML private TextField        clientEmailDisplay;
    @FXML private Label            clientTypeTag;

    // ── Step 2: Training ─────────────────────────────────────────────────────
    @FXML private ComboBox<String> periodCombo;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> coachCombo;
    @FXML private Label            coachFeeHint;

    // ── Step 3: Payment method ───────────────────────────────────────────────
    @FXML private ComboBox<String> methodCombo;

    // ── Summary labels ───────────────────────────────────────────────────────
    @FXML private Label sumClientLabel;
    @FXML private Label sumTrainingLabel;
    @FXML private Label sumPeriodLabel;
    @FXML private Label priceLabel;
    @FXML private Label coachingFeeLabel;
    @FXML private Label discountLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label totalPriceLabel;
    @FXML private Label sumMethodLabel;

    // ── Membership fee label (optional — add fx:id="membershipFeeLabel" in FXML) ─
    @FXML private Label membershipFeeLabel;

    // ── Upgrade info label ───────────────────────────────────────────────────
    @FXML private Label upgradeBadge;

    // ── Validation area ──────────────────────────────────────────────────────
    @FXML private VBox  validationBox;
    @FXML private Label validationLabel;

    // ── Buttons ──────────────────────────────────────────────────────────────
    @FXML private Button AddPaymentBtn;
    @FXML private Button ClearBtn;

    // ── In-memory look-up maps ────────────────────────────────────────────────
    private final Map<String, int[]>               trainingTypeMap   = new LinkedHashMap<>();
    private final Map<String, Map<String, long[]>> rateMap           = new LinkedHashMap<>();
    private final Map<String, long[]>              coachMap          = new LinkedHashMap<>();
    private final Map<String, Integer>             paymentTypeMap    = new LinkedHashMap<>();
    private final Map<Integer, Double>             clientDiscountMap = new LinkedHashMap<>();

    // ── State ────────────────────────────────────────────────────────────────
    private Client        selectedClient  = null;
    private UpgradeResult currentUpgrade  = null;

    /**
     * TRUE when this payment comes from the "Avail Membership" flow in
     * RegistrationController.  In this mode:
     *  - The membership fee (₱1000) is added to the total.
     *  - The coach combo is always enabled (client is about to become a Member).
     *  - If the client already has an active Monthly/Yearly plan, the period and
     *    type combos are locked (they keep their plan, just pay the membership fee).
     */
    private boolean isFromRegistration = false;

    /**
     * TRUE when the client coming from registration already has an active
     * Monthly or Yearly plan.  In that case we hide/lock the plan pickers and
     * only charge the membership fee.
     */
    private boolean clientHasActivePlan = false;

    private boolean suppressSearch = false;
    private boolean pendingFilter  = false;
    private String  pendingQuery   = "";

    private ObservableList<Client> allClients;
    private final ObservableList<Client> displayedClients =
            FXCollections.observableArrayList();

    private static final String NO_COACH = "No Coach (walk-in)";

    // ── Static registry ──────────────────────────────────────────────────────
    private static PaymentController instance;
    public  static PaymentController getInstance() { return instance; }

    // ════════════════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        instance = this;
        loadFonts();
        loadClients();
        loadTrainingData();
        loadPaymentMethods();
        setupClientSearch();
        setupCombos();
        setupButtons();
        resetSummary();
        hideValidation();
        hideUpgradeBadge();
        ExpiryResetDAO.runExpiryResets();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  FONTS
    // ════════════════════════════════════════════════════════════════════════
    private void loadFonts() {
        Font.loadFont(getClass().getResourceAsStream(
                "/Font/Inter/Inter-VariableFont_opsz,wght.ttf"), 13);
        Font.loadFont(getClass().getResourceAsStream(
                "/Font/Inter/Inter-Italic-VariableFont_opsz,wght.ttf"), 13);
        Font.loadFont(getClass().getResourceAsStream(
                "/Font/Bebas_Neue/BebasNeue-Regular.ttf"), 26);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DATA LOADERS
    // ════════════════════════════════════════════════════════════════════════
    private void loadClients() {
        allClients = ClientDAO.getAllClients();
        displayedClients.setAll(allClients);
        clientSearchCombo.setItems(displayedClients);

        clientSearchCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Client c) {
                return c == null ? "" : c.getFullName() + "  (#" + c.getClientID() + ")";
            }
            @Override public Client fromString(String s) {
                if (s == null || s.isBlank()) return null;
                return allClients.stream()
                        .filter(c -> toString(c).equalsIgnoreCase(s.trim())
                                || String.valueOf(c.getClientID()).equals(s.trim()))
                        .findFirst().orElse(null);
            }
        });
    }

    private void loadTrainingData() {
        clientDiscountMap.clear();
        PlanDAO.getAllClientTypes().forEach(ct -> {
            String raw = ct.getDiscount().replace("%", "").trim();
            try {
                double val = Double.parseDouble(raw) / 100.0;
                clientDiscountMap.put(ct.getId(), val);
            } catch (NumberFormatException e) {
                clientDiscountMap.put(ct.getId(), 0.0);
            }
        });

        String sql =
                "SELECT tt.TrainingTypeID, tt.TrainingCategory, tt.Coaching_Fee, " +
                        "       pp.PaymentPeriodID, pp.PaymentPeriod, pp.Days, " +
                        "       r.FinalPrice, r.ClientTypeID " +
                        "FROM TrainingTypeTable tt " +
                        "JOIN RateTable r           ON r.TrainingTypeID   = tt.TrainingTypeID " +
                        "JOIN PaymentPeriodTable pp ON pp.PaymentPeriodID = r.PaymentPeriodID " +
                        "ORDER BY tt.TrainingTypeID, pp.PaymentPeriodID, r.ClientTypeID";

        try (Connection con = DBConnector.connect();
             Statement st   = con.createStatement();
             ResultSet rs   = st.executeQuery(sql)) {

            while (rs.next()) {
                String cat  = rs.getString("TrainingCategory");
                int    ttID = rs.getInt("TrainingTypeID");
                long   cfee = (long)(rs.getDouble("Coaching_Fee") * 100);

                String per   = rs.getString("PaymentPeriod");
                int    ppID  = rs.getInt("PaymentPeriodID");
                long   price = (long)(rs.getDouble("FinalPrice") * 100);
                int    ctID  = rs.getInt("ClientTypeID");

                trainingTypeMap.putIfAbsent(cat, new int[]{ttID, 0, (int) cfee});
                rateMap.putIfAbsent(cat, new LinkedHashMap<>());
                rateMap.get(cat).put(per + "_" + ctID,
                        new long[]{ppID, rs.getInt("Days"), price});
            }
        } catch (SQLException e) { e.printStackTrace(); }

        typeCombo.setItems(FXCollections.observableArrayList(trainingTypeMap.keySet()));
    }

    private void loadPaymentMethods() {
        String sql = "SELECT PaymentTypeID, PaymentType FROM PaymentTypeTable ORDER BY PaymentTypeID";
        try (Connection con = DBConnector.connect();
             Statement st   = con.createStatement();
             ResultSet rs   = st.executeQuery(sql)) {
            while (rs.next())
                paymentTypeMap.put(rs.getString("PaymentType"), rs.getInt("PaymentTypeID"));
        } catch (SQLException e) { e.printStackTrace(); }
        methodCombo.setItems(FXCollections.observableArrayList(paymentTypeMap.keySet()));
    }

    /**
     * Loads coaches filtered by the selected training category.
     * Always loads coaches when isFromRegistration=true (client is becoming a Member).
     */
    private void loadCoachesForClient(Client client) {
        coachMap.clear();
        coachMap.put(NO_COACH, new long[]{-1, -1, 0});

        String selectedCat = typeCombo.getValue();
        String categoryFilter = (selectedCat != null && !selectedCat.isBlank())
                ? "AND (" +
                "  LOWER(COALESCE(tt.TrainingCategory,'')) = 'both' " +
                "  OR LOWER(COALESCE(tt.TrainingCategory,'')) = LOWER('" +
                selectedCat.replace("'", "''") + "')" +
                ")"
                : "";

        String sql =
                "SELECT s.StaffID, " +
                        "       CONCAT(s.StaffFirstName, ' ', s.StaffLastName) AS CoachName, " +
                        "       COALESCE(tt.TrainingCategory, 'None') AS TrainingCategory, " +
                        "       COALESCE(csa.ClientStaffAssignmentID, -1) AS AssignmentID, " +
                        "       COALESCE(csa.Applied_Coaching_Price, tt.Coaching_Fee, 0) AS CoachingPrice " +
                        "FROM StaffTable s " +
                        "LEFT JOIN TrainingTypeTable tt ON s.TrainingTypeID = tt.TrainingTypeID " +
                        "LEFT JOIN ClientStaffAssignmentTable csa " +
                        "       ON csa.StaffID = s.StaffID AND csa.ClientID = ? " +
                        "WHERE LOWER(s.SystemRole) = 'staff' " +
                        "  AND s.StaffID != 1 " +
                        categoryFilter +
                        " ORDER BY s.StaffFirstName";

        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, client.getClientID());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String display = rs.getString("CoachName")
                            + " (" + rs.getString("TrainingCategory") + ")";
                    coachMap.put(display, new long[]{
                            rs.getLong("StaffID"),
                            rs.getLong("AssignmentID"),
                            (long)(rs.getDouble("CoachingPrice") * 100)
                    });
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        coachCombo.setItems(FXCollections.observableArrayList(coachMap.keySet()));
        coachCombo.getSelectionModel().selectFirst();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CLIENT SEARCH
    // ════════════════════════════════════════════════════════════════════════
    private void setupClientSearch() {
        clientSearchCombo.getEditor().textProperty().addListener((obs, old, text) -> {
            if (suppressSearch) return;
            if (clientSearchCombo.isShowing()) {
                pendingQuery = text == null ? "" : text.trim().toLowerCase();
                if (!pendingFilter) {
                    pendingFilter = true;
                    Platform.runLater(this::applyPendingFilter);
                }
                return;
            }
            applyFilter(text == null ? "" : text.trim().toLowerCase());
        });

        clientSearchCombo.setOnAction(e -> {
            Client c = clientSearchCombo.getValue();
            if (c == null) return;
            // BUG FIX #4: set suppressSearch BEFORE populateClientDetails so the
            // text-change listener (fired by setValue inside preSelectClient) doesn't
            // trigger applyFilter("") → clearClientDetails() while we are populating.
            suppressSearch = true;
            populateClientDetails(c);
            Platform.runLater(() -> suppressSearch = false);
        });
    }

    private void applyPendingFilter() {
        pendingFilter = false;
        applyFilter(pendingQuery);
    }

    private void applyFilter(String q) {
        // BUG FIX #4: Never clear client details from inside applyFilter.
        // The text listener fires while suppressSearch is still true during
        // preSelectClient — we must guard here so we don't wipe the just-selected client.
        if (suppressSearch) return;

        final List<Client> next = new ArrayList<>();
        if (q == null || q.isBlank()) {
            next.addAll(allClients);
        } else {
            for (Client c : allClients) {
                if (c.getFullName().toLowerCase().contains(q)
                        || String.valueOf(c.getClientID()).contains(q)) {
                    next.add(c);
                }
            }
        }

        if (clientSearchCombo.isShowing()) clientSearchCombo.hide();

        Platform.runLater(() -> {
            displayedClients.setAll(next);
            // Only clear details if the user explicitly cleared the search box
            // AND no client is selected yet.
            if ((q == null || q.isBlank()) && selectedClient == null) {
                clearClientDetails();
            } else if (!next.isEmpty() && !suppressSearch && !q.isBlank()) {
                clientSearchCombo.show();
            }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SETUP — COMBOS & BUTTONS
    // ════════════════════════════════════════════════════════════════════════
    private void setupCombos() {
        // Load period names directly from PaymentPeriodTable — same pattern as
        // typeCombo (trainingTypeMap) and methodCombo (loadPaymentMethods).
        // This guarantees the displayed names match the keys in rateMap exactly.
        ObservableList<String> periodNames = FXCollections.observableArrayList();
        String periodSQL = "SELECT PaymentPeriod FROM PaymentPeriodTable ORDER BY Days";
        try (Connection con = DBConnector.connect();
             Statement st   = con.createStatement();
             ResultSet rs   = st.executeQuery(periodSQL)) {
            while (rs.next()) periodNames.add(rs.getString("PaymentPeriod"));
        } catch (SQLException e) { e.printStackTrace(); }
        periodCombo.setItems(periodNames);

        typeCombo.setOnAction(e -> {
            if (selectedClient != null) {
                // BUG FIX #3: When coming from registration, always enable coach combo.
                // The client is about to become a Member — they should be able to pick a coach now.
                boolean canSelectCoach = isFromRegistration || UpgradeDAO.hasActiveMembership(selectedClient.getClientID());
                coachCombo.setDisable(!canSelectCoach);
                if (canSelectCoach) {
                    loadCoachesForClient(selectedClient);
                } else {
                    coachMap.clear();
                    coachCombo.setItems(FXCollections.observableArrayList());
                    coachFeeHint.setText("Active membership required for coach assignment.");
                }
            }
            recalculate();
        });

        periodCombo.setOnAction(e -> recalculate());

        methodCombo.setOnAction(e -> sumMethodLabel.setText(
                methodCombo.getValue() != null ? methodCombo.getValue() : "—"));

        coachCombo.setOnAction(e -> {
            String sel = coachCombo.getValue();
            if (sel == null || sel.equals(NO_COACH)) {
                coachFeeHint.setText("");
            } else {
                long[] data = coachMap.get(sel);
                if (data != null)
                    coachFeeHint.setText("Coaching fee: ₱" + fmt(data[2] / 100.0));
            }
            recalculate();
        });
    }

    private void setupButtons() {
        AddPaymentBtn.setOnAction(e -> handleSavePayment());
        ClearBtn.setOnAction(e -> handleClear());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CLIENT POPULATION
    // ════════════════════════════════════════════════════════════════════════
    private void populateClientDetails(Client c) {
        selectedClient = c;

        String email = PaymentDAO.getClientEmail(c.getClientID());

        clientIdDisplay.setText(String.valueOf(c.getClientID()));
        clientNameDisplay.setText(c.getFullName());
        clientContactDisplay.setText(c.getContact());
        clientEmailDisplay.setText(email);
        clientTypeTag.setText(isFromRegistration ? "Non Member → Member" : c.getClientType());

        // BUG FIX #3: Coach combo enabled when:
        //   a) client has active membership already, OR
        //   b) coming from registration (about to become member on save)
        boolean hasMembership = UpgradeDAO.hasActiveMembership(c.getClientID());
        boolean canSelectCoach = isFromRegistration || hasMembership;
        coachCombo.setDisable(!canSelectCoach);

        if (!canSelectCoach) {
            coachFeeHint.setText("Select a Membership plan to enable coach assignment.");
        } else {
            coachFeeHint.setText("");
        }

        // BUG FIX #1: Check if client already has an active Monthly/Yearly plan.
        // If yes and coming from registration → lock plan combos, only charge membership fee.
        if (isFromRegistration) {
            clientHasActivePlan = PaymentDAO.hasActiveNonDailyPlan(c.getClientID());
            if (clientHasActivePlan) {
                // Lock plan pickers — client keeps their existing plan
                typeCombo.setDisable(true);
                periodCombo.setDisable(true);
                showUpgradeBadge(
                        "ℹ This client already has an active Monthly/Yearly plan. " +
                                "Only the membership fee (₱" + fmt(PaymentDAO.getMembershipFee(2)) + ") will be charged.",
                        "#60a5fa", false);
            } else {
                // Let them choose a plan (Daily is OK to upgrade)
                typeCombo.setDisable(false);
                periodCombo.setDisable(false);
            }
        } else {
            clientHasActivePlan = false;
            typeCombo.setDisable(false);
            periodCombo.setDisable(false);
        }

        loadCoachesForClient(c);
        recalculate();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PRE-SELECT CLIENT  (called from RegistrationController)
    // ════════════════════════════════════════════════════════════════════════
    /**
     * Called by RegistrationController when "Avail Membership" is confirmed.
     * Sets isFromRegistration=true so the membership fee is added and the
     * coach combo is unlocked.
     */
    public void preSelectClient(Client client) {
        if (client == null) return;

        // Mark this as a registration-origin payment BEFORE populateClientDetails
        isFromRegistration = true;

        loadClients();

        Client match = allClients.stream()
                .filter(c -> c.getClientID() == client.getClientID())
                .findFirst()
                .orElse(client);

        suppressSearch = true;
        clientSearchCombo.setValue(match);
        populateClientDetails(match);
        Platform.runLater(() -> suppressSearch = false);
    }

    private void clearClientDetails() {
        selectedClient      = null;
        currentUpgrade      = null;
        isFromRegistration  = false;
        clientHasActivePlan = false;

        clientIdDisplay.setText("");
        clientNameDisplay.setText("");
        clientContactDisplay.setText("");
        clientEmailDisplay.setText("");
        clientTypeTag.setText("");
        coachMap.clear();
        coachCombo.setItems(FXCollections.observableArrayList());
        coachCombo.setDisable(false);
        coachFeeHint.setText("");
        typeCombo.setDisable(false);
        periodCombo.setDisable(false);
        hideUpgradeBadge();
        resetSummary();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  RECALCULATE SUMMARY
    // ════════════════════════════════════════════════════════════════════════
    private void recalculate() {
        if (selectedClient == null) {
            hideUpgradeBadge();
            return;
        }

        // ── Registration flow with existing active plan ───────────────────────
        // Client keeps their plan — only charge the membership fee.
        if (isFromRegistration && clientHasActivePlan) {
            double memFee = PaymentDAO.getMembershipFee(2); // 2 = Member
            sumClientLabel.setText(selectedClient.getFullName());
            sumTrainingLabel.setText("Existing Plan (kept)");
            sumPeriodLabel.setText("—");
            priceLabel.setText("₱0.00");
            coachingFeeLabel.setText("₱0.00");
            discountLabel.setText("— ₱0.00");
            if (membershipFeeLabel != null) membershipFeeLabel.setText("₱" + fmt(memFee));
            subtotalLabel.setText("₱" + fmt(memFee));
            totalPriceLabel.setText("₱" + fmt(memFee));
            return;
        }

        String cat    = typeCombo.getValue();
        String period = periodCombo.getValue();
        if (cat == null || period == null) {
            hideUpgradeBadge();
            return;
        }

        sumClientLabel.setText(selectedClient.getFullName());
        sumTrainingLabel.setText(cat);
        sumPeriodLabel.setText(period);

        // When coming from registration, the client will become a Member → use Member rates.
        // Otherwise, use their current type.
        boolean isMember = isFromRegistration || "Member".equalsIgnoreCase(selectedClient.getClientType());
        int clientTypeID = isMember ? 2 : 1;

        Map<String, long[]> periods = rateMap.get(cat);
        if (periods == null) { hideUpgradeBadge(); return; }

        long[] rate = periods.get(period + "_" + clientTypeID);
        // Fallback: if no Member rate, try Non-Member rate
        if (rate == null) rate = periods.get(period + "_1");
        if (rate == null) { hideUpgradeBadge(); return; }

        double basePrice       = rate[2] / 100.0;
        double discountPercent = clientDiscountMap.getOrDefault(clientTypeID, 0.0);
        double discount        = basePrice * discountPercent;

        double coachFee = 0.0;
        String coachSel = coachCombo.getValue();
        if (coachSel != null && !coachSel.equals(NO_COACH)) {
            long[] cd = coachMap.get(coachSel);
            if (cd != null) coachFee = cd[2] / 100.0;
        }

        // ── Membership fee (NEW) ─────────────────────────────────────────────
        // Added when coming from registration — this is the ₱1000 enrollment fee.
        double membershipFee = isFromRegistration ? PaymentDAO.getMembershipFee(2) : 0.0;

        // ── Upgrade eligibility check (skip for registration flow) ───────────
        double effectiveTotal;

        if (isFromRegistration) {
            // Registration flow: no upgrade check — this is a fresh membership enrollment.
            // Total = plan price (discounted) + coach fee + membership fee
            effectiveTotal = basePrice - discount + coachFee + membershipFee;
            hideUpgradeBadge();
            priceLabel.setText("₱" + fmt(basePrice));
            currentUpgrade = null;
        } else {
            int trainingTypeID = trainingTypeMap.get(cat)[0];
            int ppID           = (int) rate[0];
            currentUpgrade = UpgradeDAO.checkUpgrade(
                    selectedClient.getClientID(), trainingTypeID, ppID, clientTypeID);

            switch (currentUpgrade.getStatus()) {

                case UPGRADE_ALLOWED -> {
                    double topUp   = currentUpgrade.getTopUpAmount();
                    effectiveTotal = topUp + coachFee;
                    showUpgradeBadge(String.format(
                                    "⬆ Upgrade: current plan ₱%s → new plan ₱%s  |  Top-up: ₱%s",
                                    fmt(currentUpgrade.getCurrentPrice()),
                                    fmt(currentUpgrade.getNewPrice()),
                                    fmt(topUp)),
                            "#4ade80", false);
                    priceLabel.setText("₱" + fmt(currentUpgrade.getTopUpAmount()));
                }

                case DOWNGRADE_BLOCKED -> {
                    effectiveTotal = basePrice - discount + coachFee;
                    showUpgradeBadge(
                            "⬇ Downgrade not allowed until current plan expires on "
                                    + currentUpgrade.getCurrentPlanExpiry()
                                    + ". Please wait or choose a higher-tier plan.",
                            "#ef4444", true);
                    priceLabel.setText("₱" + fmt(basePrice));
                }

                case SAME_PLAN_BLOCKED -> {
                    effectiveTotal = basePrice - discount + coachFee;
                    showUpgradeBadge(
                            "⚠ This plan (" + currentUpgrade.getCurrentCategory()
                                    + " / " + currentUpgrade.getCurrentPeriod()
                                    + ") is already active until "
                                    + currentUpgrade.getCurrentPlanExpiry() + ".",
                            "#f59e0b", true);
                    priceLabel.setText("₱" + fmt(basePrice));
                }

                default -> { // NO_ACTIVE_PLAN — normal fresh payment
                    effectiveTotal = basePrice - discount + coachFee;
                    hideUpgradeBadge();
                    priceLabel.setText("₱" + fmt(basePrice));
                }
            }
        }

        coachingFeeLabel.setText("₱" + fmt(coachFee));
        discountLabel.setText(String.format("— ₱%s (%.0f%%)", fmt(discount), discountPercent * 100));

        // Show membership fee line if applicable
        if (membershipFeeLabel != null) {
            if (membershipFee > 0) {
                membershipFeeLabel.setText("₱" + fmt(membershipFee));
                membershipFeeLabel.setVisible(true);
                membershipFeeLabel.setManaged(true);
            } else {
                membershipFeeLabel.setVisible(false);
                membershipFeeLabel.setManaged(false);
            }
        }

        if (currentUpgrade != null && currentUpgrade.getStatus() == UpgradeResult.Status.UPGRADE_ALLOWED) {
            subtotalLabel.setText("₱" + fmt(currentUpgrade.getTopUpAmount() + coachFee));
            totalPriceLabel.setText("₱" + fmt(currentUpgrade.getTopUpAmount() + coachFee));
        } else {
            subtotalLabel.setText("₱" + fmt(effectiveTotal));
            totalPriceLabel.setText("₱" + fmt(effectiveTotal));
        }
    }

    private String fmt(double v) { return String.format("%,.2f", v); }

    // ════════════════════════════════════════════════════════════════════════
    //  UPGRADE BADGE UI
    // ════════════════════════════════════════════════════════════════════════
    private void showUpgradeBadge(String message, String color, boolean isBlock) {
        if (upgradeBadge == null) return;
        upgradeBadge.setText(message);
        upgradeBadge.setStyle(
                "-fx-text-fill: " + color + ";" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-family: 'Inter';" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 6 12 6 12;" +
                        "-fx-background-color: " + color + "22;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: " + color + "66;" +
                        "-fx-border-radius: 8;" +
                        "-fx-border-width: 1;");
        upgradeBadge.setWrapText(true);
        upgradeBadge.setVisible(true);
        upgradeBadge.setManaged(true);
        AddPaymentBtn.setDisable(isBlock);
    }

    private void hideUpgradeBadge() {
        if (upgradeBadge != null) {
            upgradeBadge.setVisible(false);
            upgradeBadge.setManaged(false);
        }
        AddPaymentBtn.setDisable(false);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  VALIDATION
    // ════════════════════════════════════════════════════════════════════════
    private boolean validate() {
        List<String> errors = new ArrayList<>();
        if (selectedClient == null) errors.add("Please select a client");
        if (methodCombo.getValue() == null) errors.add("Please select a payment method");

        // When registration + existing plan: skip plan validation — no plan change needed
        if (!(isFromRegistration && clientHasActivePlan)) {
            if (periodCombo.getValue() == null) errors.add("Please select a payment period");
            if (typeCombo.getValue() == null)   errors.add("Please select a training type");
        }

        if (currentUpgrade != null && currentUpgrade.isBlocked()) {
            if (currentUpgrade.getStatus() == UpgradeResult.Status.DOWNGRADE_BLOCKED) {
                errors.add("Downgrade not allowed until current plan expires ("
                        + currentUpgrade.getCurrentPlanExpiry() + ").");
            } else {
                errors.add("This plan is already active until "
                        + currentUpgrade.getCurrentPlanExpiry() + ".");
            }
        }

        if (!errors.isEmpty()) {
            showErrors(errors);
            return false;
        }
        hideValidation();
        return true;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SAVE PAYMENT
    // ════════════════════════════════════════════════════════════════════════
    private void handleSavePayment() {
        if (!validate()) return;

        boolean isUpgrade = currentUpgrade != null
                && currentUpgrade.getStatus() == UpgradeResult.Status.UPGRADE_ALLOWED;

        String confirmMsg;
        if (isFromRegistration) {
            double memFee = PaymentDAO.getMembershipFee(2);
            if (clientHasActivePlan) {
                confirmMsg = "Confirm membership enrollment for " + selectedClient.getFullName() + "?\n\n"
                        + "Membership fee: ₱" + fmt(memFee) + "\n"
                        + "Their existing plan will be kept.\n\n"
                        + "Client will become a Member on Save.";
            } else {
                confirmMsg = "Confirm membership enrollment + plan payment for "
                        + selectedClient.getFullName() + "?\n\n"
                        + "Total: " + totalPriceLabel.getText() + " (includes ₱" + fmt(memFee) + " membership fee)\n\n"
                        + "Client will become a Member on Save.";
            }
        } else if (isUpgrade) {
            String oldCat = currentUpgrade.getCurrentCategory();
            String newCat = typeCombo.getValue();
            confirmMsg = "⚠  Plan Upgrade Confirmation\n\n"
                    + "Switching from [" + oldCat + "] to [" + newCat + "].\n"
                    + "Top-up amount due: ₱" + fmt(currentUpgrade.getTopUpAmount()) + "\n\n"
                    + "Continue?";
        } else {
            confirmMsg = "Confirm payment of " + totalPriceLabel.getText()
                    + "\nfor " + selectedClient.getFullName() + "?";
        }

        boolean finalIsUpgrade = isUpgrade;
        showConfirmModal(confirmMsg, () -> {
            try {
                if (finalIsUpgrade) {
                    saveUpgradeToDatabase();
                } else {
                    saveToDatabase();
                }
                showSuccessAndClear();
            } catch (SQLException ex) {
                ex.printStackTrace();
                showErrors(List.of("Database error: " + ex.getMessage()));
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NORMAL (FRESH) PAYMENT  — now writes MembershipTable too
    // ─────────────────────────────────────────────────────────────────────────
    private void saveToDatabase() throws SQLException {
        // ── Special registration path: existing monthly/yearly plan, only charge membership fee ──
        if (isFromRegistration && clientHasActivePlan) {
            saveMembershipFeeOnly();
            return;
        }

        String cat    = typeCombo.getValue();
        String period = periodCombo.getValue();

        // BUG FIX #2 / #1: When from registration, force Member (clientTypeID=2) so
        // they get Member discount rates and become a Member in MembershipTable.
        boolean isMember = isFromRegistration || "Member".equalsIgnoreCase(selectedClient.getClientType());
        int clientTypeID = isMember ? 2 : 1;

        Map<String, long[]> periodRates = rateMap.get(cat);
        long[] rate = periodRates.get(period + "_" + clientTypeID);
        if (rate == null) rate = periodRates.get(period + "_1"); // fallback
        if (rate == null) throw new SQLException("Rate not found for selected options.");

        int trainingTypeID = trainingTypeMap.get(cat)[0];
        int ppID           = (int) rate[0];
        int periodDays     = (int) rate[1];

        int rateIDfromDB = resolveRateID(trainingTypeID, ppID, clientTypeID);

        String  coachSel = coachCombo.getValue();
        boolean hasCoach = coachSel != null && !coachSel.equals(NO_COACH);
        int     assignmentID;

        if (hasCoach) {
            long[] cd = coachMap.get(coachSel);
            assignmentID = (cd[1] >= 0)
                    ? (int) cd[1]
                    : createAssignment(selectedClient.getClientID(),
                    (int) cd[0], cd[2] / 100.0);
        } else {
            assignmentID = getOrCreateNoneAssignment(selectedClient.getClientID());
        }

        int    paymentTypeID   = paymentTypeMap.get(methodCombo.getValue());
        double basePrice       = rate[2] / 100.0;
        double coachFee        = hasCoach ? coachMap.get(coachSel)[2] / 100.0 : 0.0;
        double discountPct     = clientDiscountMap.getOrDefault(clientTypeID, 0.0);
        double discount        = basePrice * discountPct;
        // BUG FIX #2: Add membership fee when coming from registration
        double membershipFee   = isFromRegistration ? PaymentDAO.getMembershipFee(2) : 0.0;
        double total           = basePrice - discount + coachFee + membershipFee;

        String snapshotMembership = isFromRegistration ? "Member" : selectedClient.getClientType();

        // BUG FIX #5 (the major one): pass clientTypeID and periodDays so
        // insertReceipt() also upserts MembershipTable in the same transaction.
        PaymentDAO.insertReceipt(
                selectedClient.getClientID(),
                paymentTypeID,
                rateIDfromDB,
                total,
                assignmentID > 0 ? assignmentID : null,
                cat,
                period,
                hasCoach ? coachSel : null,
                snapshotMembership,
                clientTypeID,   // NEW param — drives MembershipTable update
                periodDays      // NEW param — days for DateExpired
        );
    }

    /**
     * Registration path where the client already has an active Monthly/Yearly plan.
     * We only charge the membership fee and write a "Membership Enrollment" receipt.
     * MembershipTable is still updated to flip them to Member.
     */
    private void saveMembershipFeeOnly() throws SQLException {
        // Use the "None / Daily / Non-Member" rate (RateID with TrainingTypeID=4) as
        // the receipt's rate, with TotalPayment = membership fee.
        // This keeps the plan history intact and only adds a membership fee receipt.
        int noneRateID = resolveRateID(4, 1, 1); // TrainingTypeID=4(None), Daily, Non-Member

        int paymentTypeID = paymentTypeMap.get(methodCombo.getValue());
        double membershipFee = PaymentDAO.getMembershipFee(2);

        String  coachSel = coachCombo.getValue();
        boolean hasCoach = coachSel != null && !coachSel.equals(NO_COACH);
        int     assignmentID;
        if (hasCoach) {
            long[] cd = coachMap.get(coachSel);
            assignmentID = (cd[1] >= 0)
                    ? (int) cd[1]
                    : createAssignment(selectedClient.getClientID(), (int) cd[0], cd[2] / 100.0);
        } else {
            assignmentID = getOrCreateNoneAssignment(selectedClient.getClientID());
        }

        // For MembershipTable update: use the existing plan's remaining days.
        // We look up how many days remain in the current plan and preserve that.
        int remainingDays = getActivePlanRemainingDays(selectedClient.getClientID());
        // If we can't find it, use 365 as a safe default (Yearly membership)
        if (remainingDays <= 0) remainingDays = 365;

        PaymentDAO.insertReceipt(
                selectedClient.getClientID(),
                paymentTypeID,
                noneRateID,
                membershipFee,
                assignmentID > 0 ? assignmentID : null,
                "Membership Enrollment",   // snapshot category
                "Membership Fee",          // snapshot period label
                hasCoach ? coachSel : null,
                "Member",
                2,               // clientTypeID = Member
                remainingDays    // preserve remaining plan duration
        );
    }

    /**
     * Returns how many days remain in the client's current active plan.
     * Used when enrolling a member who already has a Monthly/Yearly plan.
     */
    private int getActivePlanRemainingDays(int clientID) {
        String sql =
                "SELECT DATEDIFF(DATE_ADD(DATE(r.PaymentDate), INTERVAL pp.Days DAY), CURDATE()) AS DaysLeft " +
                        "FROM ReceiptTable r " +
                        "JOIN RateTable ra          ON r.RateID          = ra.RateID " +
                        "JOIN PaymentPeriodTable pp ON ra.PaymentPeriodID = pp.PaymentPeriodID " +
                        "JOIN TrainingTypeTable tt  ON ra.TrainingTypeID  = tt.TrainingTypeID " +
                        "WHERE r.ClientID = ? " +
                        "  AND tt.TrainingTypeID != 4 " +
                        "  AND pp.Days >= 30 " +
                        "  AND DATE_ADD(DATE(r.PaymentDate), INTERVAL pp.Days DAY) >= CURDATE() " +
                        "ORDER BY r.PaymentDate DESC LIMIT 1";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clientID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("DaysLeft");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UPGRADE PAYMENT
    // ─────────────────────────────────────────────────────────────────────────
    private void saveUpgradeToDatabase() throws SQLException {
        String cat    = typeCombo.getValue();
        String period = periodCombo.getValue();
        boolean isMember = "Member".equalsIgnoreCase(selectedClient.getClientType());
        int clientTypeID = isMember ? 2 : 1;

        long[] rate           = rateMap.get(cat).get(period + "_" + clientTypeID);
        int    trainingTypeID = trainingTypeMap.get(cat)[0];
        int    ppID           = (int) rate[0];
        int    periodDays     = (int) rate[1];

        int rateIDfromDB = resolveRateID(trainingTypeID, ppID, clientTypeID);

        String  coachSel = coachCombo.getValue();
        boolean hasCoach = coachSel != null && !coachSel.equals(NO_COACH);
        int     assignmentID;

        if (hasCoach) {
            long[] cd = coachMap.get(coachSel);
            assignmentID = (cd[1] >= 0)
                    ? (int) cd[1]
                    : createAssignment(selectedClient.getClientID(),
                    (int) cd[0], cd[2] / 100.0);
        } else {
            assignmentID = getOrCreateNoneAssignment(selectedClient.getClientID());
        }

        double coachFee   = hasCoach ? coachMap.get(coachSel)[2] / 100.0 : 0.0;
        double topUp      = currentUpgrade.getTopUpAmount();
        double totalCharge = topUp + coachFee;

        int paymentTypeID = paymentTypeMap.get(methodCombo.getValue());

        int receiptID = UpgradeDAO.executeUpgradeTransaction(
                selectedClient.getClientID(),
                rateIDfromDB,
                clientTypeID,
                assignmentID,
                paymentTypeID,
                totalCharge,
                periodDays,
                cat,
                period,
                hasCoach ? coachSel : null,
                selectedClient.getClientType(),
                true
        );

        if (receiptID < 0) throw new SQLException("Upgrade transaction failed — receipt not generated.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DB HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private int resolveRateID(int trainingTypeID, int ppID, int clientTypeID)
            throws SQLException {
        String rateSQL =
                "SELECT RateID FROM RateTable " +
                        "WHERE TrainingTypeID=? AND PaymentPeriodID=? AND ClientTypeID=?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(rateSQL)) {
            ps.setInt(1, trainingTypeID);
            ps.setInt(2, ppID);
            ps.setInt(3, clientTypeID);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Rate not found for selected options.");
                return rs.getInt("RateID");
            }
        }
    }

    private int getOrCreateNoneAssignment(int clientID) throws SQLException {
        String sql = "SELECT ClientStaffAssignmentID FROM ClientStaffAssignmentTable " +
                "WHERE ClientID=? AND StaffID=1 LIMIT 1";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clientID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("ClientStaffAssignmentID");
            }
        }
        return createAssignment(clientID, 1, 0.0);
    }

    private int createAssignment(int clientID, int staffID, double coachPrice)
            throws SQLException {
        String sql = "INSERT INTO ClientStaffAssignmentTable " +
                "(ClientID, StaffID, Applied_Coaching_Price) VALUES (?,?,?)";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, clientID);
            ps.setInt(2, staffID);
            ps.setDouble(3, coachPrice);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Failed to create assignment.");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  POST-SAVE
    // ════════════════════════════════════════════════════════════════════════
    private void showSuccessAndClear() {
        showSuccess("Payment saved successfully!");
        javafx.animation.PauseTransition pause =
                new javafx.animation.PauseTransition(Duration.seconds(2.5));
        pause.setOnFinished(e -> handleClear());
        pause.play();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CLEAR / RESET
    // ════════════════════════════════════════════════════════════════════════
    private void handleClear() {
        suppressSearch = true;
        isFromRegistration  = false;
        clientHasActivePlan = false;
        clientSearchCombo.getSelectionModel().clearSelection();
        clientSearchCombo.getEditor().clear();
        displayedClients.setAll(allClients);
        Platform.runLater(() -> suppressSearch = false);

        clearClientDetails();
        periodCombo.getSelectionModel().clearSelection();
        typeCombo.getSelectionModel().clearSelection();
        coachCombo.getSelectionModel().clearSelection();
        methodCombo.getSelectionModel().clearSelection();
        coachFeeHint.setText("");
        hideValidation();
        hideUpgradeBadge();
        resetSummary();
    }

    private void resetSummary() {
        sumClientLabel.setText("—");
        sumTrainingLabel.setText("—");
        sumPeriodLabel.setText("—");
        priceLabel.setText("₱0.00");
        coachingFeeLabel.setText("₱0.00");
        discountLabel.setText("— ₱0.00");
        subtotalLabel.setText("₱0.00");
        totalPriceLabel.setText("₱0.00");
        sumMethodLabel.setText("—");
        if (membershipFeeLabel != null) {
            membershipFeeLabel.setText("₱0.00");
            membershipFeeLabel.setVisible(false);
            membershipFeeLabel.setManaged(false);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  VALIDATION UI
    // ════════════════════════════════════════════════════════════════════════
    private void showErrors(List<String> errors) {
        if (validationBox != null) {
            validationBox.getChildren().clear();
            validationBox.setStyle(
                    "-fx-background-color: rgba(229,57,53,0.10);" +
                            "-fx-background-radius: 12;" +
                            "-fx-border-color: rgba(229,57,53,0.50);" +
                            "-fx-border-width: 1.5;" +
                            "-fx-border-radius: 12;" +
                            "-fx-padding: 12 16 12 16;");
            HBox header = new HBox(8);
            header.setAlignment(Pos.CENTER_LEFT);
            Label warningIcon = new Label("⚠");
            warningIcon.setStyle("-fx-text-fill:#e53935;-fx-font-size:16px;");
            Label headerText = new Label("Please fix the following before saving:");
            headerText.setStyle(
                    "-fx-text-fill:#e53935;-fx-font-size:13px;" +
                            "-fx-font-weight:bold;-fx-font-family:'Inter';");
            header.getChildren().addAll(warningIcon, headerText);
            validationBox.getChildren().add(header);

            for (String err : errors) {
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                VBox.setMargin(row, new Insets(5, 0, 0, 4));
                Label bullet = new Label("•");
                bullet.setStyle("-fx-text-fill:#ff6b6b;-fx-font-size:16px;");
                Label msgLbl = new Label(err);
                msgLbl.setStyle(
                        "-fx-text-fill:#fca5a5;-fx-font-size:13px;-fx-font-family:'Inter';");
                msgLbl.setWrapText(true);
                row.getChildren().addAll(bullet, msgLbl);
                validationBox.getChildren().add(row);
            }
            validationBox.setVisible(true);
            validationBox.setManaged(true);
            FadeTransition ft = new FadeTransition(Duration.millis(180), validationBox);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        } else if (validationLabel != null) {
            StringBuilder sb = new StringBuilder();
            for (String e : errors) sb.append("⚠  ").append(e).append("\n");
            validationLabel.setText(sb.toString().trim());
            validationLabel.setStyle(
                    "-fx-text-fill:#ff6b6b;-fx-font-size:13px;" +
                            "-fx-font-family:'Inter';-fx-font-weight:bold;");
            validationLabel.setVisible(true);
            validationLabel.setManaged(true);
        }
    }

    private void showSuccess(String msg) {
        if (validationBox != null) {
            validationBox.getChildren().clear();
            validationBox.setStyle(
                    "-fx-background-color: rgba(74,222,128,0.10);" +
                            "-fx-background-radius: 12;" +
                            "-fx-border-color: rgba(74,222,128,0.45);" +
                            "-fx-border-width: 1.5;" +
                            "-fx-border-radius: 12;" +
                            "-fx-padding: 12 16 12 16;");
            Label lbl = new Label("✅  " + msg);
            lbl.setStyle(
                    "-fx-text-fill:#4ade80;-fx-font-size:13px;" +
                            "-fx-font-weight:bold;-fx-font-family:'Inter';");
            validationBox.getChildren().add(lbl);
            validationBox.setVisible(true);
            validationBox.setManaged(true);
        } else if (validationLabel != null) {
            validationLabel.setStyle("-fx-text-fill:#4ade80;-fx-font-size:13px;-fx-font-weight:bold;");
            validationLabel.setText("✅ " + msg);
            validationLabel.setVisible(true);
            validationLabel.setManaged(true);
        }
    }

    private void hideValidation() {
        if (validationBox != null) {
            validationBox.setVisible(false);
            validationBox.setManaged(false);
            validationBox.getChildren().clear();
        }
        if (validationLabel != null) {
            validationLabel.setText("");
            validationLabel.setStyle("");
            validationLabel.setVisible(false);
            validationLabel.setManaged(false);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CONFIRM MODAL
    // ════════════════════════════════════════════════════════════════════════
    private void showConfirmModal(String message, Runnable onConfirm) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/codes/acegym/ConfirmationPopup.fxml"));
            Parent root = loader.load();

            ConfirmationController popup = loader.getController();
            popup.setMessage(message);
            popup.setOnConfirm(onConfirm);

            Stage confirmStage = new Stage();
            confirmStage.initStyle(StageStyle.TRANSPARENT);
            confirmStage.initModality(Modality.APPLICATION_MODAL);

            Stage owner = (Stage) AddPaymentBtn.getScene().getWindow();
            confirmStage.initOwner(owner);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            confirmStage.setScene(scene);

            GaussianBlur blur = new GaussianBlur(10);
            owner.getScene().getRoot().setEffect(blur);
            confirmStage.setOnHidden(e -> owner.getScene().getRoot().setEffect(null));

            confirmStage.show();
            centerStage(confirmStage, owner);

            root.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(250), root);
            ft.setToValue(1);
            ft.play();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void centerStage(Stage stage, Stage owner) {
        stage.setX(owner.getX() + owner.getWidth()  / 2 - stage.getWidth()  / 2);
        stage.setY(owner.getY() + owner.getHeight() / 2 - stage.getHeight() / 2);
    }
}