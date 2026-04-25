package codes.acegym.Controllers;

import codes.acegym.DB.ClientDAO;
import codes.acegym.DB.DBConnector;
import codes.acegym.Objects.Client;
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

    // ── Validation area ──────────────────────────────────────────────────────
    @FXML private VBox  validationBox;
    @FXML private Label validationLabel;

    // ── Buttons ──────────────────────────────────────────────────────────────
    @FXML private Button AddPaymentBtn;
    @FXML private Button ClearBtn;

    // ── In-memory look-up maps ────────────────────────────────────────────────
    private final Map<String, int[]>               trainingTypeMap = new LinkedHashMap<>();
    private final Map<String, Map<String, long[]>> rateMap         = new LinkedHashMap<>();
    private final Map<String, long[]>              coachMap        = new LinkedHashMap<>();
    private final Map<String, Integer>             paymentTypeMap  = new LinkedHashMap<>();
    private final Map<Integer, Double>             clientDiscountMap = new LinkedHashMap<>();

    private Client selectedClient = null;

    private boolean suppressSearch = false;
    private boolean pendingFilter  = false;
    private String  pendingQuery   = "";

    private ObservableList<Client> allClients;

    private static final String NO_COACH = "No Coach (walk-in)";

    // ════════════════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadFonts();
        loadClients();
        loadTrainingData();
        loadPaymentMethods();
        setupClientSearch();
        setupCombos();
        setupButtons();
        resetSummary();
        hideValidation();
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
        clientSearchCombo.setItems(FXCollections.observableArrayList(allClients));
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
        codes.acegym.DB.PlanDAO.getAllClientTypes().forEach(ct -> {
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
                String cat   = rs.getString("TrainingCategory");
                int    ttID  = rs.getInt("TrainingTypeID");
                long   cfee  = (long)(rs.getDouble("Coaching_Fee") * 100);

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

    private void loadCoachesForClient(Client client) {
        coachMap.clear();
        coachMap.put(NO_COACH, new long[]{-1, -1, 0});

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
                        "ORDER BY s.StaffFirstName";

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
        if (q == null || q.isBlank()) {
            clientSearchCombo.setItems(FXCollections.observableArrayList(allClients));
            clearClientDetails();
            return;
        }
        ObservableList<Client> filtered = FXCollections.observableArrayList();
        for (Client c : allClients) {
            if (c.getFullName().toLowerCase().contains(q)
                    || String.valueOf(c.getClientID()).contains(q)) {
                filtered.add(c);
            }
        }
        clientSearchCombo.setItems(filtered);
        if (!clientSearchCombo.isShowing() && !filtered.isEmpty())
            clientSearchCombo.show();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SETUP — COMBOS & BUTTONS
    // ════════════════════════════════════════════════════════════════════════
    private void setupCombos() {
        ObservableList<String> periodNames = FXCollections.observableArrayList();
        codes.acegym.DB.PlanDAO.getAllPaymentPeriods().forEach(p -> periodNames.add(p.getName()));
        periodCombo.setItems(periodNames);

        periodCombo.setOnAction(e -> recalculate());
        typeCombo.setOnAction(e -> recalculate());

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

        String email = "";
        String sql = "SELECT COALESCE(ClientEmail,'') AS e FROM ClientTable WHERE ClientID=?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, c.getClientID());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) email = rs.getString("e");
            }
        } catch (SQLException ex) { ex.printStackTrace(); }

        clientIdDisplay.setText(String.valueOf(c.getClientID()));
        clientNameDisplay.setText(c.getFullName());
        clientContactDisplay.setText(c.getContact());
        clientEmailDisplay.setText(email);
        clientTypeTag.setText(c.getClientType());

        loadCoachesForClient(c);
        recalculate();
    }

    private void clearClientDetails() {
        selectedClient = null;
        clientIdDisplay.setText("");
        clientNameDisplay.setText("");
        clientContactDisplay.setText("");
        clientEmailDisplay.setText("");
        clientTypeTag.setText("");
        coachMap.clear();
        coachCombo.setItems(FXCollections.observableArrayList());
        coachFeeHint.setText("");
        resetSummary();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  RECALCULATE SUMMARY
    // ════════════════════════════════════════════════════════════════════════
    private void recalculate() {
        String cat    = typeCombo.getValue();
        String period = periodCombo.getValue();
        if (selectedClient == null || cat == null || period == null) return;

        sumClientLabel.setText(selectedClient.getFullName());
        sumTrainingLabel.setText(cat);
        sumPeriodLabel.setText(period);

        boolean isMember  = "Member".equalsIgnoreCase(selectedClient.getClientType());
        int clientTypeID  = isMember ? 2 : 1;

        Map<String, long[]> periods = rateMap.get(cat);
        if (periods == null) return;

        long[] rate = periods.get(period + "_" + clientTypeID);
        if (rate == null) return;

        double basePrice = rate[2] / 100.0;

        double discountPercent = clientDiscountMap.getOrDefault(clientTypeID, 0.0);
        double discount = basePrice * discountPercent;

        double coachFee = 0.0;
        String coachSel = coachCombo.getValue();
        if (coachSel != null && !coachSel.equals(NO_COACH)) {
            long[] cd = coachMap.get(coachSel);
            if (cd != null) coachFee = cd[2] / 100.0;
        }

        double subtotal = basePrice - discount + coachFee;

        priceLabel.setText("₱" + fmt(basePrice));
        coachingFeeLabel.setText("₱" + fmt(coachFee));
        discountLabel.setText(String.format("— ₱%s (%.0f%%)", fmt(discount), discountPercent * 100));
        subtotalLabel.setText("₱" + fmt(subtotal));
        totalPriceLabel.setText("₱" + fmt(subtotal));
    }

    private String fmt(double v) { return String.format("%,.2f", v); }

    // ════════════════════════════════════════════════════════════════════════
    //  VALIDATION
    // ════════════════════════════════════════════════════════════════════════
    private boolean validate() {
        List<String> errors = new ArrayList<>();
        if (selectedClient == null)         errors.add("Please select a client");
        if (periodCombo.getValue() == null) errors.add("Please select a payment period");
        if (typeCombo.getValue() == null)   errors.add("Please select a training type");
        if (methodCombo.getValue() == null) errors.add("Please select a payment method");

        if (!errors.isEmpty()) {
            showErrors(errors);
            return false;
        }
        hideValidation();
        return true;
    }

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
    //  SAVE PAYMENT
    // ════════════════════════════════════════════════════════════════════════
    private void handleSavePayment() {
        if (!validate()) return;

        String confirmMsg = "Confirm payment of " + totalPriceLabel.getText()
                + "\nfor " + selectedClient.getFullName() + "?";

        showConfirmModal(confirmMsg, () -> {
            try {
                saveToDatabase();
                showSuccessAndClear();
            } catch (SQLException ex) {
                ex.printStackTrace();
                showErrors(List.of("Database error: " + ex.getMessage()));
            }
        });
    }

    private void saveToDatabase() throws SQLException {
        String cat    = typeCombo.getValue();
        String period = periodCombo.getValue();
        boolean isMember  = "Member".equalsIgnoreCase(selectedClient.getClientType());
        int clientTypeID  = isMember ? 2 : 1;

        long[] rate = rateMap.get(cat).get(period + "_" + clientTypeID);
        int    trainingTypeID = trainingTypeMap.get(cat)[0];
        int    ppID           = (int) rate[0];

        // Resolve RateID from DB
        int rateIDfromDB;
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
                rateIDfromDB = rs.getInt("RateID");
            }
        }

        // Coach / assignment
        String  coachSel  = coachCombo.getValue();
        boolean hasCoach  = coachSel != null && !coachSel.equals(NO_COACH);
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

        int    paymentTypeID = paymentTypeMap.get(methodCombo.getValue());
        double basePrice     = rate[2] / 100.0;
        double coachFee      = hasCoach ? coachMap.get(coachSel)[2] / 100.0 : 0.0;
        double discountPct   = clientDiscountMap.getOrDefault(clientTypeID, 0.0);
        double discount      = basePrice * discountPct;
        double total         = basePrice - discount + coachFee;

        // ── Snapshot values — frozen at the time of this payment ─────────────
        String snapshotTrainingCategory = cat;
        String snapshotPaymentPeriod    = period;
        String snapshotCoachName        = hasCoach ? coachSel : null;
        String snapshotMembershipType   = selectedClient.getClientType();

        String insertSQL =
                "INSERT INTO ReceiptTable " +
                        "(ClientID, RateID, ClientStaffAssignmentID, PaymentTypeID, TotalPayment, PaymentDate, " +
                        " SnapshotTrainingCategory, SnapshotPaymentPeriod, SnapshotCoachName, SnapshotMembershipType) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), ?, ?, ?, ?)";

        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(insertSQL)) {
            ps.setInt(1, selectedClient.getClientID());
            ps.setInt(2, rateIDfromDB);
            ps.setInt(3, assignmentID);
            ps.setInt(4, paymentTypeID);
            ps.setDouble(5, total);
            ps.setString(6, snapshotTrainingCategory);
            ps.setString(7, snapshotPaymentPeriod);
            ps.setString(8, snapshotCoachName);   // NULL if no coach
            ps.setString(9, snapshotMembershipType);
            ps.executeUpdate();
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
        clientSearchCombo.getSelectionModel().clearSelection();
        clientSearchCombo.getEditor().clear();
        clientSearchCombo.setItems(FXCollections.observableArrayList(allClients));
        Platform.runLater(() -> suppressSearch = false);

        clearClientDetails();
        periodCombo.getSelectionModel().clearSelection();
        typeCombo.getSelectionModel().clearSelection();
        coachCombo.getSelectionModel().clearSelection();
        methodCombo.getSelectionModel().clearSelection();
        coachFeeHint.setText("");
        hideValidation();
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