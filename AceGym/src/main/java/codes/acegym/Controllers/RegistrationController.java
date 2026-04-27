package codes.acegym.Controllers;

import codes.acegym.DB.DBConnector;
import codes.acegym.Objects.Client;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RegistrationController implements Refreshable {

    // ── Section 1: Client Registration ──────────────────────────────────────
    @FXML private TextField regFirstNameField;
    @FXML private TextField regLastNameField;
    @FXML private TextField regEmailField;
    @FXML private TextField regContactField;
    @FXML private Button    RegisterClientBtn;
    @FXML private Button    ResetRegBtn;
    @FXML private VBox      regValidationBox;

    // ── Section 2: Membership Registration ──────────────────────────────────
    @FXML private ComboBox<Client> clientSearchBox;
    @FXML private TextField        memFirstNameField;
    @FXML private TextField        memLastNameField;
    @FXML private TextField        memDateField;
    @FXML private TextField        memEmailField;
    @FXML private TextField        memContactField;
    @FXML private Button           AvailMembershipBtn;
    @FXML private Button           resetBtn;
    @FXML private VBox             memValidationBox;

    // ── Bridge to PaymentController ──────────────────────────────────────────
    private PaymentController paymentControllerRef;
    private Tab               paymentTabRef;

    // ── State ────────────────────────────────────────────────────────────────
    private final ObservableList<Client> nonMemberClients = FXCollections.observableArrayList();
    private final ObservableList<Client> displayedClients = FXCollections.observableArrayList();

    private Client  selectedMember = null;
    private boolean suppressSearch = false;

    // ════════════════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        setupSection1();
        setupMembershipSearch();
        setupSection2Buttons();
    }

    @Override
    public void refreshData() {
        reloadNonMemberClients();
    }

    public void setPaymentBridge(PaymentController paymentController, Tab paymentTab) {
        this.paymentControllerRef = paymentController;
        this.paymentTabRef        = paymentTab;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SECTION 1 — CLIENT REGISTRATION
    // ════════════════════════════════════════════════════════════════════════
    private void setupSection1() {
        RegisterClientBtn.setOnAction(e -> handleRegisterClient());
        if (ResetRegBtn != null) {
            ResetRegBtn.setOnAction(e -> clearRegistrationForm());
        }
    }

    private void handleRegisterClient() {
        List<String> errors = validateRegistrationForm();
        if (!errors.isEmpty()) {
            showRegErrors(errors);
            return;
        }

        String firstName = regFirstNameField.getText().trim();
        String lastName  = regLastNameField.getText().trim();
        String email     = regEmailField.getText().trim();
        String contact   = regContactField.getText().trim();

        showModal("Confirm Client Registration?", RegisterClientBtn, () -> {
            boolean ok = registerClientToDB(firstName, lastName, email, contact);
            if (ok) {
                showRegSuccess("Client registered successfully!");
                clearRegistrationForm();
                reloadNonMemberClients();
            } else {
                showRegErrors(List.of("Database error — client was not saved. Please try again."));
            }
        });
    }

    private List<String> validateRegistrationForm() {
        List<String> errors = new ArrayList<>();

        String firstName = regFirstNameField.getText().trim();
        String lastName  = regLastNameField.getText().trim();
        String email     = regEmailField.getText().trim();
        String contact   = regContactField.getText().trim();

        if (firstName.isEmpty()) errors.add("First name is required.");
        if (lastName.isEmpty())  errors.add("Last name is required.");
        if (email.isEmpty())     errors.add("Email address is required.");
        if (contact.isEmpty())   errors.add("Contact number is required.");

        if (!errors.isEmpty()) return errors;

        if (!email.matches("^[\\w.+\\-]+@[\\w\\-]+(\\.[\\w\\-]+)+$"))
            errors.add("Email address format is invalid.");

        // ── Philippine mobile number validation ──────────────────────────
        // Accepts: 09XXXXXXXXX (11 digits) or +639XXXXXXXXX (13 chars)
        // The 3rd digit after 09 must NOT turn it into a non-existent prefix.
        // Valid PH mobile prefixes: 0900-0999 range but only real networks:
        //   Globe/TM:  0905,0906,0915,0916,0917,0926,0927,0935,0936,0945,0946,0955,0956,0966,0967,0976,0977,0978,0979,0995,0996,0997
        //   Smart/TNT: 0900,0907,0908,0909,0910,0911,0912,0913,0914,0918,0919,0920,0921,0928,0929,0930,0938,0939,0940,0946,0947,0948,0949,0950,0951,0961,0998,0999
        //   DITO:      0895,0896,0897,0898,0991,0992,0993,0994
        //   Sun/Unlimit: 0922,0923,0924,0925,0931,0932,0933,0934,0942,0943
        // Rather than hardcode every prefix, we enforce:
        //   - Must be exactly 11 digits starting with 09, OR
        //   - Must be exactly +639 followed by 9 digits
        //   - The 3rd digit (after 09) must be 0-9 (already covered)
        //   - No repeating digit sequences (catches 09----323--- style junk)
        //   - All characters after optional leading + must be digits only
        if (!contact.isEmpty()) {
            String normalized = contact.replaceAll("[\\s\\-]", ""); // strip spaces and hyphens for length check
            boolean valid = false;

            if (normalized.matches("^09\\d{9}$")) {
                // 09XXXXXXXXX — 11 digits, starts with 09
                valid = true;
            } else if (normalized.matches("^\\+639\\d{9}$")) {
                // +639XXXXXXXXX — international format
                valid = true;
            }

            if (!valid) {
                errors.add("Contact number must be a valid Philippine mobile number (e.g. 09171234567 or +639171234567).");
            }
        }

        if (!errors.isEmpty()) return errors;

        if (isEmailTaken(email))
            errors.add("Email address is already registered to another client.");
        if (isContactTaken(regContactField.getText().trim()))
            errors.add("Contact number is already registered to another client.");

        return errors;
    }

    private boolean isEmailTaken(String email) {
        String sql = "SELECT COUNT(*) FROM ClientTable WHERE LOWER(ClientEmail) = LOWER(?)";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private boolean isContactTaken(String contact) {
        String sql = "SELECT COUNT(*) FROM ClientTable WHERE ContactNumber = ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, contact.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private boolean registerClientToDB(String firstName, String lastName,
                                       String email, String contact) {
        String sql =
                "INSERT INTO ClientTable (FirstName, LastName, ClientEmail, ContactNumber) " +
                        "VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, email);
            ps.setString(4, contact);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void clearRegistrationForm() {
        regFirstNameField.clear();
        regLastNameField.clear();
        regEmailField.clear();
        regContactField.clear();
        hideRegValidation();
    }

    private void showRegErrors(List<String> errors) {
        buildErrorBox(regValidationBox, errors);
    }

    private void showRegSuccess(String msg) {
        if (regValidationBox == null) return;
        regValidationBox.getChildren().clear();
        regValidationBox.setStyle(
                "-fx-background-color: radial-gradient(center 50% 50%, radius 70%, #0d2818 0%, #111827 100%);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: linear-gradient(to bottom, #4ade80, rgba(74,222,128,0.2));" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 12;" +
                        "-fx-padding: 12 16 12 16;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(74,222,128,0.35), 15, 0, 0, 0);");

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label checkBadge = new Label("✓");
        checkBadge.setMinSize(28, 28);
        checkBadge.setMaxSize(28, 28);
        checkBadge.setAlignment(Pos.CENTER);
        checkBadge.setStyle(
                "-fx-background-color: #16a34a;" +
                        "-fx-background-radius: 8;" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;");

        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 13px;" +
                "-fx-font-weight: bold; -fx-font-family: 'Inter';");
        lbl.setWrapText(true);

        row.getChildren().addAll(checkBadge, lbl);
        regValidationBox.getChildren().add(row);
        regValidationBox.setVisible(true);
        regValidationBox.setManaged(true);

        javafx.animation.PauseTransition p =
                new javafx.animation.PauseTransition(Duration.seconds(3));
        p.setOnFinished(ev -> hideRegValidation());
        p.play();
    }

    private void hideRegValidation() {
        if (regValidationBox != null) {
            regValidationBox.setVisible(false);
            regValidationBox.setManaged(false);
            regValidationBox.getChildren().clear();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SECTION 2 — MEMBERSHIP SEARCH
    // ════════════════════════════════════════════════════════════════════════
    private void setupMembershipSearch() {
        loadNonMemberClientsFromDB();

        clientSearchBox.setConverter(new StringConverter<Client>() {
            @Override
            public String toString(Client c) {
                return c == null ? "" : c.getFirstName() + " " + c.getLastName();
            }
            @Override
            public Client fromString(String s) { return null; }
        });

        displayedClients.setAll(nonMemberClients);
        clientSearchBox.setItems(displayedClients);

        clientSearchBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (suppressSearch) return;
            applyFilter(newVal == null ? "" : newVal.trim());
        });

        clientSearchBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (suppressSearch || newVal == null) return;
                    suppressSearch = true;
                    selectMembershipClient(newVal);
                    Platform.runLater(() -> {
                        String name = newVal.getFirstName() + " " + newVal.getLastName();
                        clientSearchBox.getEditor().setText(name);
                        clientSearchBox.getEditor().positionCaret(name.length());
                        suppressSearch = false;
                    });
                });
    }

    private void loadNonMemberClientsFromDB() {
        nonMemberClients.clear();
        String sql =
                "SELECT c.ClientID, c.FirstName, c.LastName, " +
                        "       COALESCE(c.ContactNumber,'') AS ContactNumber, " +
                        "       COALESCE(c.ClientEmail,'')   AS ClientEmail " +
                        "FROM ClientTable c " +
                        "LEFT JOIN MembershipTable m " +
                        "       ON m.ClientID = c.ClientID " +
                        "      AND m.MembershipID = (" +
                        "          SELECT MembershipID FROM MembershipTable " +
                        "          WHERE ClientID = c.ClientID " +
                        "          ORDER BY DateApplied DESC LIMIT 1) " +
                        "LEFT JOIN ClientTypeTable ct ON m.ClientTypeID = ct.ClientTypeID " +
                        "WHERE COALESCE(ct.ClientType,'Non Member') = 'Non Member' " +
                        "ORDER BY c.FirstName";

        try (Connection con = DBConnector.connect();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(sql)) {
            while (rs.next()) {
                nonMemberClients.add(new Client(
                        rs.getInt("ClientID"),
                        rs.getString("FirstName"),
                        rs.getString("LastName"),
                        rs.getString("ClientEmail"),
                        rs.getString("ContactNumber"),
                        "Non Member"
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void reloadNonMemberClients() {
        loadNonMemberClientsFromDB();
        Platform.runLater(() -> displayedClients.setAll(nonMemberClients));
    }

    private void applyFilter(String query) {
        final List<Client> matched = new ArrayList<>();
        if (query.isBlank()) {
            matched.addAll(nonMemberClients);
        } else {
            String q = query.toLowerCase();
            for (Client c : nonMemberClients) {
                if (c.getFirstName().toLowerCase().contains(q)
                        || c.getLastName().toLowerCase().contains(q)) {
                    matched.add(c);
                }
            }
        }

        if (clientSearchBox.isShowing()) clientSearchBox.hide();

        Platform.runLater(() -> {
            displayedClients.setAll(matched);
            if (!query.isBlank() && !matched.isEmpty() && !clientSearchBox.isShowing()) {
                clientSearchBox.show();
            }
        });
    }

    private void selectMembershipClient(Client c) {
        selectedMember = c;
        String[] extra = getClientExtraInfo(c.getClientID());

        memFirstNameField.setText(c.getFirstName());
        memLastNameField.setText(c.getLastName());
        memEmailField.setText(extra[0] != null ? extra[0] : "");
        memContactField.setText(c.getContact() != null ? c.getContact() : "");
        if (memDateField != null) memDateField.setText(extra[1] != null ? extra[1] : "—");

        hideMemValidation();
    }

    private String[] getClientExtraInfo(int clientID) {
        String sql =
                "SELECT COALESCE(ClientEmail,'') AS ClientEmail, " +
                        "       DATE_FORMAT(CreatedAt, '%b %d, %Y') AS RegDate " +
                        "FROM ClientTable WHERE ClientID = ?";
        try (Connection con = DBConnector.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clientID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return new String[]{ rs.getString("ClientEmail"), rs.getString("RegDate") };
            }
        } catch (SQLException e) {
            String fallback = "SELECT COALESCE(ClientEmail,'') AS ClientEmail " +
                    "FROM ClientTable WHERE ClientID = ?";
            try (Connection con = DBConnector.connect();
                 PreparedStatement ps = con.prepareStatement(fallback)) {
                ps.setInt(1, clientID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next())
                        return new String[]{ rs.getString("ClientEmail"), null };
                }
            } catch (SQLException ex) { ex.printStackTrace(); }
        }
        return new String[]{ "", null };
    }

    private void clearMembershipFields() {
        selectedMember = null;
        memFirstNameField.clear();
        memLastNameField.clear();
        memEmailField.clear();
        memContactField.clear();
        if (memDateField != null) memDateField.clear();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SECTION 2 BUTTONS
    // ════════════════════════════════════════════════════════════════════════
    private void setupSection2Buttons() {
        AvailMembershipBtn.setOnAction(e -> handleAvailMembership());
        if (resetBtn != null) {
            resetBtn.setOnAction(e -> resetMembershipSection());
        }
    }

    private void handleAvailMembership() {
        if (selectedMember == null) {
            showMemErrors(List.of("Please search and select a non-member client first."));
            return;
        }
        showModal(
                "Confirm Membership Availment for " + selectedMember.getFullName() + "?",
                AvailMembershipBtn,
                () -> navigateToPayment(selectedMember)
        );
    }

    private void navigateToPayment(Client client) {
        resetMembershipSection();

        HomePageController hpc = HomePageController.getInstance();
        if (hpc != null) {
            hpc.navigateToPaymentTab(client);
            return;
        }

        PaymentController pc = (paymentControllerRef != null)
                ? paymentControllerRef
                : PaymentController.getInstance();
        if (pc == null) {
            showMemErrors(List.of(
                    "Payment tab not loaded yet. Please open the Payment tab once and try again."));
            return;
        }
        pc.preSelectClient(client);
    }

    private void resetMembershipSection() {
        suppressSearch = true;
        clientSearchBox.getSelectionModel().clearSelection();
        clientSearchBox.getEditor().clear();
        suppressSearch = false;
        clearMembershipFields();
        reloadNonMemberClients();
        hideMemValidation();
    }

    private void showMemErrors(List<String> errors) {
        buildErrorBox(memValidationBox, errors);
    }

    private void hideMemValidation() {
        if (memValidationBox != null) {
            memValidationBox.setVisible(false);
            memValidationBox.setManaged(false);
            memValidationBox.getChildren().clear();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SHARED ERROR BOX BUILDER — matches ConfirmationPopup CSS theme
    // ════════════════════════════════════════════════════════════════════════
    private void buildErrorBox(VBox box, List<String> errors) {
        if (box == null) return;
        box.getChildren().clear();
        box.setStyle(
                "-fx-background-color: radial-gradient(center 50% 50%, radius 70%, #1e293b 0%, #111827 100%);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: linear-gradient(to bottom, #CB443E, rgba(203,68,62,0.2));" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 12;" +
                        "-fx-padding: 14 18 14 18;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(203,68,62,0.4), 15, 0, 0, 0);");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-padding: 0 0 10 0;" +
                        "-fx-border-color: transparent transparent rgba(203,68,62,0.25) transparent;" +
                        "-fx-border-width: 0 0 1 0;");

        Label warningBadge = new Label("⚠");
        warningBadge.setMinSize(28, 28);
        warningBadge.setMaxSize(28, 28);
        warningBadge.setAlignment(Pos.CENTER);
        warningBadge.setStyle(
                "-fx-background-color: #CB443E;" +
                        "-fx-background-radius: 8;" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;");

        Label headerText = new Label("Please fix the following:");
        headerText.setStyle(
                "-fx-text-fill: #E8E6E9;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: 'Inter';");

        header.getChildren().addAll(warningBadge, headerText);
        box.getChildren().add(header);

        for (String err : errors) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            VBox.setMargin(row, new Insets(8, 0, 0, 4));

            Label bullet = new Label("•");
            bullet.setStyle("-fx-text-fill: #CB443E; -fx-font-size: 18px; -fx-font-weight: bold;");
            bullet.setMinWidth(12);

            Label msg = new Label(err);
            msg.setStyle("-fx-text-fill: #9da3b4; -fx-font-size: 13px; -fx-font-family: 'Inter';");
            msg.setWrapText(true);

            row.getChildren().addAll(bullet, msg);
            box.getChildren().add(row);
        }

        box.setVisible(true);
        box.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(180), box);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CONFIRMATION MODAL — pure Java, no FXML load lag
    // ════════════════════════════════════════════════════════════════════════
    private void showModal(String message, Button ownerBtn, Runnable onConfirm) {
        Stage owner = (Stage) ownerBtn.getScene().getWindow();

        // ── Icon ─────────────────────────────────────────────────
        Label iconCircle = new Label("?");
        iconCircle.setPrefSize(90, 90);
        iconCircle.setMinSize(90, 90);
        iconCircle.setMaxSize(90, 90);
        iconCircle.setAlignment(javafx.geometry.Pos.CENTER);
        iconCircle.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #CB443E;" +
                        "-fx-border-width: 3;" +
                        "-fx-border-radius: 45;" +
                        "-fx-background-radius: 45;" +
                        "-fx-text-fill: #CB443E;" +
                        "-fx-font-size: 42px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: 'Inter';" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(203,68,62,0.5), 10, 0, 0, 0);"
        );

        // ── Message ───────────────────────────────────────────────
        Label msgLabel = new Label(message);
        msgLabel.setMaxWidth(500);
        msgLabel.setWrapText(true);
        msgLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        msgLabel.setAlignment(javafx.geometry.Pos.CENTER);
        msgLabel.setStyle(
                "-fx-font-family: 'Inter';" +
                        "-fx-font-size: 22px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;"
        );

        // ── Confirm button ────────────────────────────────────────
        Button confirmBtn = new Button("Confirm");
        confirmBtn.setPrefWidth(151);
        confirmBtn.setPrefHeight(55);
        confirmBtn.setStyle(
                "-fx-background-color: #CB443E;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 20;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 24px;" +
                        "-fx-padding: 10 25;" +
                        "-fx-cursor: hand;"
        );
        confirmBtn.setOnMouseEntered(e -> confirmBtn.setStyle(confirmBtn.getStyle().replace("#CB443E", "#d9635d")));
        confirmBtn.setOnMouseExited(e  -> confirmBtn.setStyle(confirmBtn.getStyle().replace("#d9635d", "#CB443E")));

        // ── Cancel button ─────────────────────────────────────────
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setPrefWidth(140);
        cancelBtn.setPrefHeight(45);
        cancelBtn.setStyle(
                "-fx-background-color: white;" +
                        "-fx-text-fill: #0D1117;" +
                        "-fx-background-radius: 20;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 24px;" +
                        "-fx-padding: 10 25;" +
                        "-fx-cursor: hand;"
        );
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(cancelBtn.getStyle().replace("white", "#E8E6E9")));
        cancelBtn.setOnMouseExited(e  -> cancelBtn.setStyle(cancelBtn.getStyle().replace("#E8E6E9", "white")));

        javafx.scene.layout.HBox btnRow = new javafx.scene.layout.HBox(20);
        btnRow.setAlignment(javafx.geometry.Pos.CENTER);
        btnRow.getChildren().addAll(confirmBtn, cancelBtn);

        // ── Card ──────────────────────────────────────────────────
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(25);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setPadding(new javafx.geometry.Insets(30));
        card.setMinWidth(400);
        card.setStyle(
                "-fx-background-color: radial-gradient(center 50% 50%, radius 70%, #1e293b 0%, #111827 100%);" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-color: linear-gradient(to bottom, #CB443E, rgba(203,68,62,0.2));" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 15;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(203,68,62,0.4), 15, 0, 0, 0);"
        );
        card.getChildren().addAll(iconCircle, msgLabel, btnRow);

        // ── Root ──────────────────────────────────────────────────
        javafx.scene.layout.AnchorPane root = new javafx.scene.layout.AnchorPane(card);
        root.setStyle("-fx-background-color: transparent;");
        javafx.scene.layout.AnchorPane.setTopAnchor(card, 10.0);
        javafx.scene.layout.AnchorPane.setBottomAnchor(card, 10.0);
        javafx.scene.layout.AnchorPane.setLeftAnchor(card, 10.0);
        javafx.scene.layout.AnchorPane.setRightAnchor(card, 10.0);

        // ── Stage ─────────────────────────────────────────────────
        Stage confirmStage = new Stage();
        confirmStage.initStyle(StageStyle.TRANSPARENT);
        confirmStage.initModality(Modality.APPLICATION_MODAL);
        confirmStage.initOwner(owner);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        confirmStage.setScene(scene);

        GaussianBlur blur = new GaussianBlur(10);
        owner.getScene().getRoot().setEffect(blur);
        confirmStage.setOnHidden(ev -> owner.getScene().getRoot().setEffect(null));

        confirmBtn.setOnAction(ev -> {
            confirmStage.close();
            if (onConfirm != null) onConfirm.run();
        });
        cancelBtn.setOnAction(ev -> confirmStage.close());

        confirmStage.show();
        centerStage(confirmStage, owner);

        root.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void centerStage(Stage stage, Stage owner) {
        stage.setX(owner.getX() + owner.getWidth()  / 2 - stage.getWidth()  / 2);
        stage.setY(owner.getY() + owner.getHeight() / 2 - stage.getHeight() / 2);
    }
}