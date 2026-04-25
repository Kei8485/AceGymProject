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

// FIX #5: Implements Refreshable so HomePageController calls refreshData() automatically
//          every time the user switches back to the Registration tab — this keeps the
//          non-member search list in sync after a payment converts a client to Member.
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
    @FXML private TextField        memDateField;   // may be null if not in FXML — always null-checked
    @FXML private TextField        memEmailField;
    @FXML private TextField        memContactField;
    @FXML private Button           AvailMembershipBtn;
    @FXML private Button           resetBtn;
    @FXML private VBox             memValidationBox;

    // ── Bridge to PaymentController (optional — kept for backward compat) ────
    private PaymentController paymentControllerRef;
    private Tab               paymentTabRef;

    // ── State ────────────────────────────────────────────────────────────────
    // Master list loaded from DB — never modified after load
    private final ObservableList<Client> nonMemberClients = FXCollections.observableArrayList();
    // The list actually bound to the ComboBox — mutated in place, NEVER replaced via setItems()
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

    // ════════════════════════════════════════════════════════════════════════
    //  REFRESHABLE  (FIX #5)
    //  HomePageController calls this every time the user switches to this tab.
    //  It reloads the non-member list from DB so any client who just paid and
    //  became a Member is excluded from the search.
    // ════════════════════════════════════════════════════════════════════════
    @Override
    public void refreshData() {
        reloadNonMemberClients();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  BRIDGE SETTER  (call from MainController after loading both FXMLs)
    // ════════════════════════════════════════════════════════════════════════
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
        if (!contact.matches("^[0-9+\\-\\s]{7,15}$"))
            errors.add("Contact number must be 7-15 digits (numbers, +, - allowed).");

        if (!errors.isEmpty()) return errors;

        if (isEmailTaken(email))
            errors.add("Email address is already registered to another client.");
        if (isContactTaken(contact))
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
                "-fx-background-color:rgba(74,222,128,0.10);" +
                        "-fx-background-radius:12;" +
                        "-fx-border-color:rgba(74,222,128,0.45);" +
                        "-fx-border-width:1.5;-fx-border-radius:12;" +
                        "-fx-padding:12 16 12 16;");
        Label lbl = new Label("✅  " + msg);
        lbl.setStyle("-fx-text-fill:#4ade80;-fx-font-size:13px;" +
                "-fx-font-weight:bold;-fx-font-family:'Inter';");
        regValidationBox.getChildren().add(lbl);
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
    //
    //  CRITICAL: clientSearchBox.setItems() is called EXACTLY ONCE here.
    //  All filtering is done via displayedClients.setAll() — mutating the
    //  existing list in place, deferred via Platform.runLater() to avoid the
    //  JavaFX 21 IndexOutOfBoundsException that fires when the list is mutated
    //  while a click-selection event is still in-flight.
    // ════════════════════════════════════════════════════════════════════════
    private void setupMembershipSearch() {

        // Step 1: Load master data into nonMemberClients
        loadNonMemberClientsFromDB();

        // Step 2: Converter — controls what text appears in the editor/dropdown
        clientSearchBox.setConverter(new StringConverter<Client>() {
            @Override
            public String toString(Client c) {
                return c == null ? "" : c.getFirstName() + " " + c.getLastName();
            }
            @Override
            public Client fromString(String s) {
                return null; // we handle selection via selectedItemProperty
            }
        });

        // Step 3: Bind combo to the stable displayedClients list — ONE TIME ONLY
        displayedClients.setAll(nonMemberClients);
        clientSearchBox.setItems(displayedClients);

        // Step 4: Text listener — only filters, never calls setItems()
        clientSearchBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (suppressSearch) return;
            applyFilter(newVal == null ? "" : newVal.trim());
        });

        // Step 5: Selection listener — fires when user clicks a row
        clientSearchBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (suppressSearch || newVal == null) return;
                    suppressSearch = true;
                    selectMembershipClient(newVal);
                    // Restore the selected name into the editor cleanly
                    Platform.runLater(() -> {
                        String name = newVal.getFirstName() + " " + newVal.getLastName();
                        clientSearchBox.getEditor().setText(name);
                        clientSearchBox.getEditor().positionCaret(name.length());
                        suppressSearch = false;
                    });
                });
    }

    // ── DB load ───────────────────────────────────────────────────────────
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

    // FIX #2: Reload defers the setAll() to the next UI pulse so it never
    // races with an in-flight click event — prevents JavaFX 21 crash.
    private void reloadNonMemberClients() {
        loadNonMemberClientsFromDB();
        Platform.runLater(() -> displayedClients.setAll(nonMemberClients));
    }

    // ── FIX #1: Filter — hide popup first, then defer the setAll() ────────
    // The popup must be hidden BEFORE the backing list is mutated.
    // If the popup is open and we mutate the list, JavaFX 21's
    // ListViewBehavior tries to call getAddedSubList(0, 1) on a now-empty
    // list → IndexOutOfBoundsException: [fromIndex: 0, toIndex: 1, size: 0]
    private void applyFilter(String query) {
        // Build the result list first — pure logic, no UI mutation yet
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

        // Hide the popup before touching the list — no event in-flight
        if (clientSearchBox.isShowing()) clientSearchBox.hide();

        // Defer the actual mutation to the next pulse
        Platform.runLater(() -> {
            displayedClients.setAll(matched);
            if (!query.isBlank() && !matched.isEmpty() && !clientSearchBox.isShowing()) {
                clientSearchBox.show();
            }
        });
    }

    // ── Populate read-only fields ─────────────────────────────────────────
    private void selectMembershipClient(Client c) {
        selectedMember = c;
        String[] extra = getClientExtraInfo(c.getClientID());

        memFirstNameField.setText(c.getFirstName());
        memLastNameField.setText(c.getLastName());
        memEmailField.setText(extra[0] != null ? extra[0] : "");
        memContactField.setText(c.getContact() != null ? c.getContact() : "");
        // FIX #4: memDateField may be null if not wired in the FXML
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
            // CreatedAt column doesn't exist — fallback
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
        // FIX #4: null-check — memDateField is not in the current FXML
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

    // FIX #3: The old code searched for a TabPane with a "payment" tab — but
    // HomePageController uses a StackPane + ToggleButton system, not a TabPane.
    // There are ZERO TabPanes in this app, so paymentTabRef was always null and
    // navigation never happened. Now we delegate directly to HomePageController.
    private void navigateToPayment(Client client) {
        // Clear the membership form immediately so the UI looks responsive
        resetMembershipSection();

        // Delegate navigation to HomePageController — it owns the page switcher
        HomePageController hpc = HomePageController.getInstance();
        if (hpc != null) {
            hpc.navigateToPaymentTab(client);
            return;
        }

        // Fallback: if HomePageController isn't available (e.g. embedded in a
        // different host), try to pre-select the client in PaymentController directly
        // without switching pages.
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
    //  SHARED ERROR BOX BUILDER
    // ════════════════════════════════════════════════════════════════════════
    private void buildErrorBox(VBox box, List<String> errors) {
        if (box == null) return;
        box.getChildren().clear();
        box.setStyle(
                "-fx-background-color:rgba(229,57,53,0.10);" +
                        "-fx-background-radius:12;" +
                        "-fx-border-color:rgba(229,57,53,0.50);" +
                        "-fx-border-width:1.5;-fx-border-radius:12;" +
                        "-fx-padding:12 16 12 16;");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon  = new Label("⚠");
        icon.setStyle("-fx-text-fill:#e53935;-fx-font-size:16px;");
        Label title = new Label("Please fix the following:");
        title.setStyle("-fx-text-fill:#e53935;-fx-font-size:13px;" +
                "-fx-font-weight:bold;-fx-font-family:'Inter';");
        header.getChildren().addAll(icon, title);
        box.getChildren().add(header);

        for (String err : errors) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            VBox.setMargin(row, new Insets(5, 0, 0, 4));
            Label bullet = new Label("•");
            bullet.setStyle("-fx-text-fill:#ff6b6b;-fx-font-size:16px;");
            Label msg = new Label(err);
            msg.setStyle("-fx-text-fill:#fca5a5;-fx-font-size:13px;-fx-font-family:'Inter';");
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
    //  CONFIRMATION MODAL
    // ════════════════════════════════════════════════════════════════════════
    private void showModal(String message, Button ownerBtn, Runnable onConfirm) {
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

            Stage owner = (Stage) ownerBtn.getScene().getWindow();
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
            FadeTransition ft = new FadeTransition(Duration.millis(300), root);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void centerStage(Stage stage, Stage owner) {
        stage.setX(owner.getX() + owner.getWidth()  / 2 - stage.getWidth()  / 2);
        stage.setY(owner.getY() + owner.getHeight() / 2 - stage.getHeight() / 2);
    }
}