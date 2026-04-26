package codes.acegym.Controllers;

import codes.acegym.DB.AdminDAO;
import codes.acegym.DB.AdminDAO.StaffProfile;
import codes.acegym.DB.AdminDAO.StaffBasicInfo;
import codes.acegym.Session;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Consumer;

public class AdminProfileController implements Refreshable {

    // ── Theme constants ──────────────────────────────────────────────────────
    private static final String BG_MAIN      = "#0E1223";
    private static final String BG_CARD      = "#1c2237";
    private static final String BG_INPUT     = "#141928";
    private static final String COLOR_BORDER = "#2e3349";
    private static final String COLOR_RED    = "#e53935";
    private static final String COLOR_RED_DK = "#c62828";
    private static final String COLOR_TEXT   = "#ffffff";
    private static final String COLOR_MUTED  = "#7a7f94";
    private static final String COLOR_ERROR  = "#ff5252";

    // ── LEFT CARD ────────────────────────────────────────────────────────────
    @FXML private ImageView profileImage;
    @FXML private StackPane avatarStackPane;
    @FXML private Label     adminNameLabel;
    @FXML private Label     adminRoleLabel;
    @FXML private Label     adminUsernameLabel;

    // ── Account Information ──────────────────────────────────────────────────
    @FXML private TextField usernameField;
    @FXML private Label     usernameError;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;

    // ── Password fields ──────────────────────────────────────────────────────
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField retypePasswordField;

    // ── Password eye icons ───────────────────────────────────────────────────
    @FXML private ImageView toggleCurrentPass;
    @FXML private ImageView toggleNewPass;
    @FXML private ImageView toggleRetypePass;

    // ── Password error labels ────────────────────────────────────────────────
    @FXML private Label currentPassError;
    @FXML private Label newPassError;
    @FXML private Label retypePassError;

    // ── Register buttons ─────────────────────────────────────────────────────
    @FXML private Button registerAdminBtn;
    @FXML private Button registerStaffBtn;

    // ── Internal state ───────────────────────────────────────────────────────
    private StaffProfile currentProfile;
    private String       pendingImagePath;

    // ════════════════════════════════════════════════════════════════════════
    // INIT
    // ════════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        // Fix password show/hide — must run after FXML injects the nodes
        setupEyeToggle(toggleCurrentPass, currentPasswordField);
        setupEyeToggle(toggleNewPass,     newPasswordField);
        setupEyeToggle(toggleRetypePass,  retypePasswordField);
        avatarStackPane.setOnMouseClicked(e -> pickProfileImage());
        refreshData();
    }

    // ── Refreshable ──────────────────────────────────────────────────────────
    @Override
    public void refreshData() {
        String username = Session.getInstance().getLoggedInUsername();
        if (username == null) return;
        currentProfile   = AdminDAO.getProfileByUsername(username);
        pendingImagePath = null;
        if (currentProfile == null) return;


        firstNameField.setText(currentProfile.firstName());
        lastNameField.setText(currentProfile.lastName());

        adminNameLabel.setText(currentProfile.firstName() + " " + currentProfile.lastName());
        adminRoleLabel.setText(currentProfile.systemRole());
        adminUsernameLabel.setText(currentProfile.username());
        loadProfileImage(currentProfile.staffImage());
        usernameField.setText(currentProfile.username());
        clearAllErrors();

        boolean isAdmin = "Admin".equalsIgnoreCase(currentProfile.systemRole());
        registerAdminBtn.setVisible(isAdmin);
        registerAdminBtn.setManaged(isAdmin);
        registerStaffBtn.setVisible(isAdmin);
        registerStaffBtn.setManaged(isAdmin);
    }

    // ════════════════════════════════════════════════════════════════════════
    // ACCOUNT INFO — Save / Reset
    // ════════════════════════════════════════════════════════════════════════
    @FXML
    private void handleSaveChanges() {
        String fName = firstNameField.getText().trim();
        String lName = lastNameField.getText().trim();
        String uName = usernameField.getText().trim();

        if (fName.isEmpty() || lName.isEmpty() || uName.isEmpty()) {
            showError("Validation Error", "Please fill in all fields.");
            return;
        }

        // Use pendingImagePath if the user picked a new image; otherwise keep existing
        String imageToSave = (pendingImagePath != null && !pendingImagePath.isBlank())
                ? pendingImagePath
                : currentProfile.staffImage();

        boolean success = AdminDAO.updateAdminProfile(
                currentProfile.staffID(),
                fName,
                lName,
                uName,
                imageToSave
        );

        if (success) {
            Session.getInstance().setLoggedInUsername(uName);
            pendingImagePath = null;   // clear pending after successful save
            refreshData();
            showInfo("Success", "Profile updated successfully!");
        } else {
            showError("Update Failed", "Could not save changes to the database.");
        }
    }

    @FXML
    private void handleResetChanges() {
        showConfirm("Reset", "Discard all unsaved changes?", confirmed -> {
            if (confirmed) refreshData();
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // CHANGE PASSWORD
    // ════════════════════════════════════════════════════════════════════════
    @FXML
    private void handleChangePassword() {
        clearPasswordErrors();
        String current = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String retype  = retypePasswordField.getText();
        boolean valid  = true;

        if (current.isBlank()) {
            currentPassError.setText("Current password is required."); valid = false;
        }
        if (newPass.isBlank()) {
            newPassError.setText("New password is required."); valid = false;
        } else if (newPass.length() < 6) {
            newPassError.setText("Password must be at least 6 characters."); valid = false;
        }
        if (!newPass.equals(retype)) {
            retypePassError.setText("Passwords do not match."); valid = false;
        }
        if (!valid) return;

        showConfirm("Change Password", "Are you sure you want to change your password?", confirmed -> {
            if (!confirmed) return;
            boolean ok = AdminDAO.changePassword(currentProfile.staffID(), current, newPass);
            if (ok) {
                showInfo("Success", "Password changed successfully.");
                handleClearPassword();
            } else {
                currentPassError.setText("Current password is incorrect.");
            }
        });
    }



    @FXML
    private void handleClearPassword() {
        currentPasswordField.clear();
        newPasswordField.clear();
        retypePasswordField.clear();
        clearPasswordErrors();
    }

    // ════════════════════════════════════════════════════════════════════════
    // REGISTER DIALOG
    // ════════════════════════════════════════════════════════════════════════
    @FXML private void handleRegisterAdmin() { openRegisterDialog("Admin"); }
    @FXML private void handleRegisterStaff() { openRegisterDialog("Staff"); }



    private void openRegisterDialog(String role) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(registerAdminBtn.getScene().getWindow());
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setResizable(false);

        // ── Root shell ────────────────────────────────────────────────────
        VBox root = new VBox(0);
        root.setStyle(
                "-fx-background-color: #1c2237;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #2e3349;" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.85), 40, 0, 0, 0);");
        root.setPrefWidth(520);

        // ── Header ────────────────────────────────────────────────────────
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(22, 26, 22, 26));
        header.setStyle(
                "-fx-border-color: transparent transparent #2e3349 transparent;" +
                        "-fx-border-width: 0 0 1.5 0;");

        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setMinSize(46, 46); iconBox.setMaxSize(46, 46);
        iconBox.setStyle("-fx-background-color: #e53935; -fx-background-radius: 12;");
        Label iconLbl = new Label("👤");
        iconLbl.setStyle("-fx-font-size: 20px;");
        iconBox.getChildren().add(iconLbl);

        VBox titleBlock = new VBox(4);
        Label titleLbl = new Label("Register New " + role + " Account");
        titleLbl.setStyle(
                "-fx-text-fill: #ffffff; -fx-font-family: 'Inter';" +
                        "-fx-font-size: 22px; -fx-font-weight: bold;");
        Label subtitleLbl = new Label("Fill in the details below to create the account.");
        subtitleLbl.setStyle(
                "-fx-text-fill: #7a7f94; -fx-font-family: 'Inter'; -fx-font-size: 14px;");
        titleBlock.getChildren().addAll(titleLbl, subtitleLbl);

        Region hdrSpacer = new Region();
        HBox.setHgrow(hdrSpacer, Priority.ALWAYS);

        Label closeX = new Label("✕");
        closeX.setStyle("-fx-font-size: 18px; -fx-text-fill: #7a7f94; -fx-cursor: hand;");
        closeX.setOnMouseEntered(e -> closeX.setStyle("-fx-font-size: 18px; -fx-text-fill: #e53935; -fx-cursor: hand;"));
        closeX.setOnMouseExited(e  -> closeX.setStyle("-fx-font-size: 18px; -fx-text-fill: #7a7f94; -fx-cursor: hand;"));
        closeX.setOnMouseClicked(e -> dialog.close());

        header.getChildren().addAll(iconBox, titleBlock, hdrSpacer, closeX);

        // ── Body ──────────────────────────────────────────────────────────
        VBox body = new VBox();
        body.setPadding(new Insets(26, 30, 30, 30));
        body.setSpacing(0);

        // ── Step 1 header ─────────────────────────────────────────────────
        VBox step1Header = buildStepHeader("1", "Select Staff Member");
        body.getChildren().addAll(step1Header, spacer(16));

        // ── ComboBox ──────────────────────────────────────────────────────
        List<StaffBasicInfo> staffList = AdminDAO.getAllStaff();
        ObservableList<StaffBasicInfo> allStaff = FXCollections.observableArrayList(staffList);
        FilteredList<StaffBasicInfo>   filtered  = new FilteredList<>(allStaff, p -> true);

        ComboBox<StaffBasicInfo> staffCombo = new ComboBox<>(filtered);
        staffCombo.setEditable(true);
        staffCombo.setPrefWidth(Double.MAX_VALUE);
        staffCombo.setPrefHeight(50);
        staffCombo.setPromptText("Type a name or ID — select from the dropdown");
        staffCombo.setConverter(new StringConverter<>() {
            @Override public String toString(StaffBasicInfo s) {
                return s == null ? "" : s.firstName() + " " + s.lastName() + "  (#" + s.staffID() + ")";
            }
            @Override public StaffBasicInfo fromString(String s) {
                if (s == null || s.isBlank()) return null;
                return allStaff.stream()
                        .filter(st -> (st.firstName() + " " + st.lastName()).equalsIgnoreCase(s.trim())
                                || s.trim().equals(String.valueOf(st.staffID())))
                        .findFirst().orElse(null);
            }
        });

        final boolean[] settingText = {false};


        staffCombo.getEditor().textProperty().addListener((obs, old, val) -> {
            if (settingText[0]) return;  // programmatic change — ignore
            StaffBasicInfo selected = staffCombo.getSelectionModel().getSelectedItem();
            if (selected != null) {
                String expectedText = staffCombo.getConverter().toString(selected);
                if (expectedText.equals(val)) return;
            }
            String lower = val == null ? "" : val.toLowerCase();
            filtered.setPredicate(s ->
                    lower.isBlank() ||
                            (s.firstName() + " " + s.lastName()).toLowerCase().contains(lower) ||
                            String.valueOf(s.staffID()).contains(lower));
            if (!filtered.isEmpty() && !lower.isBlank()) staffCombo.show();
        });

        Label errStaff = buildErrLabel();
        body.getChildren().addAll(staffCombo, errStaff, spacer(14));

        // ── Staff details card ────────────────────────────────────────────
        VBox staffCard = buildStaffDetailsCard();
        staffCard.setVisible(false);
        staffCard.setManaged(false);

        HBox nameRow = (HBox) staffCard.getChildren().get(2);
        HBox roleRow = (HBox) staffCard.getChildren().get(3);
        HBox idRow   = (HBox) staffCard.getChildren().get(4);
        Label nameVal = (Label) nameRow.getChildren().get(1);
        Label roleVal = (Label) roleRow.getChildren().get(1);
        Label idVal   = (Label) idRow.getChildren().get(1);

        body.getChildren().addAll(staffCard, spacer(12));

        // ── Already-registered warning ────────────────────────────────────
        Label warnMsgLabel = new Label("");
        warnMsgLabel.setWrapText(true);
        warnMsgLabel.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-text-fill: #ffb74d;");

        StackPane warnIcon = new StackPane();
        warnIcon.setMinSize(40, 40); warnIcon.setMaxSize(40, 40);
        warnIcon.setStyle(
                "-fx-background-color: #7c4a0022;" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: #ffb74d55;" +
                        "-fx-border-radius: 20; -fx-border-width: 1.5;");
        Label warnIconLbl = new Label("⚠");
        warnIconLbl.setStyle("-fx-font-size: 18px; -fx-text-fill: #ffb74d;");
        warnIcon.getChildren().add(warnIconLbl);

        Label warnTitle = new Label("Already Registered");
        warnTitle.setStyle(
                "-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-text-fill: #ffb74d;");

        VBox warnTextBox = new VBox(4, warnTitle, warnMsgLabel);
        warnTextBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(warnTextBox, Priority.ALWAYS);

        HBox warnRowBox = new HBox(14, warnIcon, warnTextBox);
        warnRowBox.setAlignment(Pos.CENTER_LEFT);

        VBox warningCard = new VBox(warnRowBox);
        warningCard.setPadding(new Insets(16, 18, 16, 18));
        warningCard.setStyle(
                "-fx-background-color: #1a1200;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #ffb74d55;" +
                        "-fx-border-radius: 10; -fx-border-width: 1.5;");
        warningCard.setVisible(false);
        warningCard.setManaged(false);
        body.getChildren().addAll(warningCard, spacer(4));

        // ── Step 2 wrapper ────────────────────────────────────────────────
        Region step2Sep = new Region();
        step2Sep.setPrefHeight(1.5);
        step2Sep.setStyle("-fx-background-color: #2e3349;");
        VBox.setMargin(step2Sep, new Insets(16, 0, 20, 0));

        VBox step2Header = buildStepHeader("2", "Set Up Account");

        Label userLbl         = buildFieldLabel("Username");
        TextField userF       = buildDialogTextField("Enter username for this account");
        Label errUser         = buildErrLabel();

        Label passLbl         = buildFieldLabel("Password");
        PasswordField passF   = buildDialogPassField("Minimum 6 characters");
        Label errPass         = buildErrLabel();

        Label retypeLbl       = buildFieldLabel("Re-type Password");
        PasswordField retypeF = buildDialogPassField("Re-enter password");
        Label errRetype       = buildErrLabel();

        VBox step2Wrapper = new VBox(
                step2Sep, step2Header, spacer(16),
                userLbl, userF, errUser,
                spacer(12),
                passLbl, passF, errPass,
                spacer(12),
                retypeLbl, retypeF, errRetype
        );
        step2Wrapper.setVisible(false);
        step2Wrapper.setManaged(false);
        body.getChildren().add(step2Wrapper);

        // ── Footer / Buttons ──────────────────────────────────────────────
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(16, 30, 22, 30));
        footer.setStyle(
                "-fx-border-color: #2e3349 transparent transparent transparent;" +
                        "-fx-border-width: 1.5 0 0 0;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setPrefHeight(46);
        cancelBtn.setPrefWidth(130);
        applyGhostBtnStyle(cancelBtn, false);
        cancelBtn.setOnMouseEntered(e -> applyGhostBtnStyle(cancelBtn, true));
        cancelBtn.setOnMouseExited(e  -> applyGhostBtnStyle(cancelBtn, false));
        cancelBtn.setOnMouseClicked(e -> dialog.close());

        Button registerBtn = new Button("✓   Register Account");
        registerBtn.setDefaultButton(true);
        registerBtn.setPrefHeight(46);
        applyRedBtnStyle(registerBtn, false);
        registerBtn.setOnMouseEntered(e -> applyRedBtnStyle(registerBtn, true));
        registerBtn.setOnMouseExited(e  -> applyRedBtnStyle(registerBtn, false));

        footer.getChildren().addAll(cancelBtn, registerBtn);


        staffCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel == null) return;
            errStaff.setText("");

            settingText[0] = true;
            staffCombo.getEditor().setText(staffCombo.getConverter().toString(newSel));
            staffCombo.getEditor().positionCaret(staffCombo.getEditor().getText().length());
            filtered.setPredicate(s -> true);   // ← move this INSIDE the flag guard
            settingText[0] = false;

            nameVal.setText(newSel.firstName() + " " + newSel.lastName());
            roleVal.setText(newSel.systemRole());
            idVal.setText("ID #" + newSel.staffID());
            staffCard.setVisible(true);
            staffCard.setManaged(true);

            boolean hasAccount = AdminDAO.staffHasAccount(newSel.staffID());
            if (hasAccount) {
                warnMsgLabel.setText(
                        newSel.firstName() + " " + newSel.lastName() +
                                " already has a registered " + newSel.systemRole() +
                                " account and cannot be registered again.");
                warningCard.setVisible(true);   warningCard.setManaged(true);
                step2Wrapper.setVisible(false); step2Wrapper.setManaged(false);
            } else {
                warningCard.setVisible(false);  warningCard.setManaged(false);
                step2Wrapper.setVisible(true);  step2Wrapper.setManaged(true);
            }
        });


          // ── Register action ───────────────────────────────────────────────
        registerBtn.setOnAction(e -> {
            errStaff.setText(""); errUser.setText("");
            errPass.setText("");  errRetype.setText("");

            boolean ok = true;
            StaffBasicInfo sel = staffCombo.getSelectionModel().getSelectedItem();

            if (sel == null) {
                errStaff.setText("Please select a staff member."); ok = false;
            } else if (AdminDAO.staffHasAccount(sel.staffID())) {
                errStaff.setText(sel.firstName() + " " + sel.lastName() +
                        " already has a registered account."); ok = false;
            }

            String username = userF.getText().trim();
            if (username.isBlank()) {
                errUser.setText("Username is required."); ok = false;
            } else if (username.length() < 3) {
                errUser.setText("Username must be at least 3 characters."); ok = false;
            } else if (username.contains(" ")) {
                errUser.setText("Username cannot contain spaces."); ok = false;
            } else if (AdminDAO.isUsernameTaken(username)) {
                errUser.setText("Username is already taken."); ok = false;
            }

            String pass   = passF.getText();
            String retype = retypeF.getText();
            if (pass.isBlank()) {
                errPass.setText("Password is required."); ok = false;
            } else if (pass.length() < 6) {
                errPass.setText("At least 6 characters required."); ok = false;
            }
            if (!pass.equals(retype)) {
                errRetype.setText("Passwords do not match."); ok = false;
            }

            if (!ok) return;

            assert sel != null;
            showConfirmInDialog(dialog,
                    "Confirm Registration",
                    "Register a " + role + " account for:\n\n" +
                            sel.firstName() + " " + sel.lastName() +
                            "\n(@" + username + ")" +
                            (!"Staff".equalsIgnoreCase(sel.systemRole()) && "Admin".equalsIgnoreCase(role)
                                    ? "\n\nNote: This will promote them from Staff to Admin." : ""),
                    confirmed -> {
                        if (!confirmed) return;
                        boolean done = AdminDAO.registerAccountForStaff(
                                sel.staffID(), username, pass, role);
                        if (done) {
                            showInfo("Success", role + " account registered successfully.");
                            dialog.close();
                        } else {
                            errUser.setText("Registration failed. Please try again.");
                        }
                    });
        });

        // ── Assemble — single addAll, no ScrollPane ───────────────────────
        root.getChildren().addAll(header, body, footer);

        Scene scene = new Scene(root, 520, 820);
        scene.setFill(Color.TRANSPARENT);
        injectDialogCss(scene);

        dialog.setScene(scene);
        dialog.showAndWait();
    }
    // ── Staff details card builder ────────────────────────────────────────────
    private VBox buildStaffDetailsCard() {
        // Header row
        StackPane dot = new StackPane();
        dot.setMinSize(8, 8); dot.setMaxSize(8, 8);
        dot.setStyle("-fx-background-color:" + COLOR_RED + ";-fx-background-radius:4;");
        Label hdrLbl = new Label("STAFF DETAILS");
        hdrLbl.setStyle(
                "-fx-font-family:'Inter';-fx-font-size:10px;-fx-font-weight:bold;" +
                        "-fx-text-fill:" + COLOR_RED + ";-fx-letter-spacing:0.5;");
        HBox hdrRow = new HBox(7, dot, hdrLbl);
        hdrRow.setAlignment(Pos.CENTER_LEFT);

        Region divider = new Region();
        divider.setMinHeight(1); divider.setMaxHeight(1);
        divider.setStyle("-fx-background-color:" + COLOR_BORDER + ";");
        VBox.setMargin(divider, new Insets(8, 0, 8, 0));

        HBox nameRow = buildCardRow("Full Name", "—");
        HBox roleRow = buildCardRow("Role",      "—");
        HBox idRow   = buildCardRow("Staff ID",  "—");

        VBox card = new VBox(hdrRow, divider, nameRow, roleRow, idRow);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle(
                "-fx-background-color:" + BG_INPUT + ";" +
                        "-fx-background-radius:10;" +
                        "-fx-border-color:" + COLOR_BORDER + ";" +
                        "-fx-border-radius:10;-fx-border-width:1;");
        return card;
    }

    private HBox buildCardRow(String label, String value) {
        Label lbl = new Label(label);
        lbl.setMinWidth(76);
        lbl.setStyle("-fx-font-family:'Inter';-fx-font-size:12px;-fx-text-fill:" + COLOR_MUTED + ";");

        Label val = new Label(value);
        val.setStyle("-fx-font-family:'Inter';-fx-font-size:13px;-fx-font-weight:600;-fx-text-fill:" + COLOR_TEXT + ";");

        HBox row = new HBox(lbl, val);
        row.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(row, new Insets(2, 0, 2, 0));
        return row;
    }

    // ── Step header builder ───────────────────────────────────────────────────
    private VBox buildStepHeader(String num, String text) {
        StackPane badge = new StackPane();
        badge.setMinSize(22, 22); badge.setMaxSize(22, 22);
        badge.setStyle("-fx-background-color:" + COLOR_RED + ";-fx-background-radius:11;");
        Label numLbl = new Label(num);
        numLbl.setStyle(
                "-fx-font-family:'Inter';-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:white;");
        badge.getChildren().add(numLbl);

        Label textLbl = new Label(text);
        textLbl.setStyle(
                "-fx-font-family:'Inter';-fx-font-size:14px;-fx-font-weight:bold;" +
                        "-fx-text-fill:" + COLOR_TEXT + ";");

        HBox headerRow = new HBox(10, badge, textLbl);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Region divider = new Region();
        divider.setMinHeight(1); divider.setMaxHeight(1);
        divider.setStyle("-fx-background-color:" + COLOR_BORDER + ";");
        VBox.setMargin(divider, new Insets(8, 0, 0, 0));

        return new VBox(headerRow, divider);
    }

    // ── Dialog field factories ────────────────────────────────────────────────
    private Label buildFieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: 600;" +
                        "-fx-text-fill: #ffffff;");
        VBox.setMargin(l, new Insets(0, 0, 5, 0));
        return l;
    }

    private TextField buildDialogTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefHeight(46);   // taller like payment details fields
        return tf;
    }
    private PasswordField buildDialogPassField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setPrefHeight(46);
        return pf;
    }

    private Label buildErrLabel() {
        Label l = new Label("");
        l.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 11px; -fx-text-fill: #ff5252;");
        VBox.setMargin(l, new Insets(3, 0, 0, 2));
        return l;
    }

    private Region spacer(double height) {
        Region r = new Region();
        r.setPrefHeight(height);
        return r;
    }

    // ── Button style helpers ──────────────────────────────────────────────────
    private void applyRedBtnStyle(Button b, boolean hover) {
        String bg = hover ? COLOR_RED_DK : COLOR_RED;
        b.setStyle(
                "-fx-background-color:" + bg + ";-fx-text-fill:white;" +
                        "-fx-font-family:'Inter';-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-background-radius:9;-fx-cursor:hand;");
    }

    private void applyGhostBtnStyle(Button b, boolean hover) {
        String bg = hover ? "#252a42" : "#1e2130";
        b.setStyle(
                "-fx-background-color:" + bg + ";-fx-text-fill:white;" +
                        "-fx-font-family:'Inter';-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-background-radius:9;-fx-border-color:" + COLOR_BORDER + ";" +
                        "-fx-border-radius:9;-fx-border-width:1;-fx-cursor:hand;");
    }

    private void applyCloseBtnStyle(Label l, boolean hover) {
        String color = hover ? COLOR_RED : COLOR_MUTED;
        l.setStyle("-fx-font-size:14px;-fx-text-fill:" + color + ";-fx-cursor:hand;");
    }

    // ════════════════════════════════════════════════════════════════════════
    // CSS INJECTION — fixes white ComboBox in dialogs
    // ════════════════════════════════════════════════════════════════════════
    private void injectDialogCss(Scene scene) {
        String css =
                // ── ComboBox chrome ──────────────────────────────────────────
                ".combo-box {" +
                        "  -fx-background-color: #141928;" +
                        "  -fx-border-color: #2e3349;" +
                        "  -fx-border-radius: 9;" +
                        "  -fx-background-radius: 9;" +
                        "  -fx-border-width: 1;" +
                        "}" +
                        // Combine focused and showing to stay dark blue/grey
                        ".combo-box:focused, .combo-box:showing {" +
                        "  -fx-border-color: #2e3349;" +
                        "  -fx-effect: none;" +
                        "}" +
                        ".combo-box > .text-field:focused {" +
                        "  -fx-border-color: transparent;" + // Hide the internal text field's red border
                        "  -fx-background-color: transparent;" +
                        "  -fx-effect: none;" +
                        "}" +
                        ".combo-box .list-cell {" +
                        "  -fx-background-color: #141928;" +
                        "  -fx-text-fill: white;" +
                        "  -fx-font-family: 'Inter';" +
                        "  -fx-font-size: 14px;" +
                        "  -fx-padding: 0 12;" +
                        "}" +
                        ".combo-box .arrow-button { -fx-background-color: transparent; -fx-padding: 0 10 0 0; }" +
                        ".combo-box .arrow        { -fx-background-color: #7a7f94; }" +
// ── Dropdown popup ───────────────────────────────────────────
                        ".combo-box-popup .list-view {" +
                        "  -fx-background-color: #1c2237;" +
                        "  -fx-border-color: #2e3349;" +
                        "  -fx-border-width: 1;" +
                        "  -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.65), 16, 0, 0, 4);" +
                        "}" +
                        ".combo-box-popup .list-cell {" +
                        "  -fx-background-color: #1c2237;" +
                        "  -fx-text-fill: white;" +
                        "  -fx-font-family: 'Inter';" +
                        "  -fx-font-size: 14px;" +
                        "  -fx-padding: 10 16;" +
                        "}" +
                        ".combo-box-popup .list-cell:filled:hover    { -fx-background-color: #252d45; }" +
                        ".combo-box-popup .list-cell:filled:selected { -fx-background-color: rgba(229,57,53,0.18); }" +
                        // ── Text / Password fields ───────────────────────────────────
                        ".text-field, .password-field {" +
                        "  -fx-background-color: #141928;" +
                        "  -fx-border-color: #2e3349;" +
                        "  -fx-border-radius: 9;" +
                        "  -fx-background-radius: 9;" +
                        "  -fx-border-width: 1;" +
                        "  -fx-text-fill: white;" +
                        "  -fx-prompt-text-fill: #4a5068;" +
                        "  -fx-font-family: 'Inter';" +
                        "  -fx-font-size: 14px;" +
                        "  -fx-padding: 0 14;" +
                        "}" +
                        ".text-field:focused, .password-field:focused {" +
                        "  -fx-border-color: #e53935;" +
                        "  -fx-effect: dropshadow(three-pass-box, rgba(229,57,53,0.18), 8, 0, 0, 0);" +
                        "}";


        try {
            File tmp = File.createTempFile("acegym-dialog-", ".css");
            tmp.deleteOnExit();
            Files.writeString(tmp.toPath(), css);
            scene.getStylesheets().add(tmp.toURI().toString());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }    // ════════════════════════════════════════════════════════════════════════
    // EYE TOGGLE — actually reveals the password text
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Injects a mirror TextField into the same StackPane as the PasswordField.
     * Clicking the eye swaps their visibility and syncs the text.
     * The TextField inherits the same styleClass list so CSS applies uniformly.
     */
    private void setupEyeToggle(ImageView eye, PasswordField pf) {
        if (eye == null || pf == null) return;

        // Mirror TextField — same style classes + height as the PasswordField
        TextField tf = new TextField();
        tf.getStyleClass().addAll(pf.getStyleClass());
        tf.setPrefHeight(pf.getPrefHeight());
        tf.setVisible(false);
        tf.setManaged(false);

        // Insert immediately after the PasswordField in its StackPane
        StackPane parent = (StackPane) pf.getParent();
        int idx = parent.getChildren().indexOf(pf);
        parent.getChildren().add(idx + 1, tf);

        // Keep text in sync (one-way is enough while only one is visible)
        pf.textProperty().addListener((obs, o, n) -> { if (!tf.isVisible()) tf.setText(n); });
        tf.textProperty().addListener((obs, o, n) -> { if (!pf.isVisible()) pf.setText(n);  });

        final boolean[] shown = {false};
        eye.setOnMouseClicked(ev -> {
            shown[0] = !shown[0];

            if (shown[0]) {
                tf.setText(pf.getText());
                pf.setVisible(false); pf.setManaged(false);
                tf.setVisible(true);  tf.setManaged(true);
                tf.requestFocus();
                tf.positionCaret(tf.getText().length());
            } else {
                pf.setText(tf.getText());
                tf.setVisible(false); tf.setManaged(false);
                pf.setVisible(true);  pf.setManaged(true);
            }

            // WITH — matches your actual resource folder:
            String icon = shown[0] ? "/image/eye-solid.png" : "/image/eye-slash-solid.png";
            try {
                var url = getClass().getResource(icon);
                if (url != null) eye.setImage(new Image(url.toExternalForm()));
            } catch (Exception ignored) {}
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // STYLED DIALOGS
    // ════════════════════════════════════════════════════════════════════════

    private void showConfirm(String title, String message, Consumer<Boolean> callback) {
        Stage d = makeDialogStage(adminNameLabel);
        VBox content = makeDialogContent(message, "❓", COLOR_MUTED);
        Button yes = makeDialogBtn("Yes, Confirm", COLOR_RED, COLOR_RED_DK, true);
        Button no  = makeDialogBtn("Cancel",       "#1e2130", "#252a42",    false);
        yes.setOnAction(e -> { d.close(); callback.accept(true);  });
        no.setOnAction(e  -> { d.close(); callback.accept(false); });
        showDialog(d, title, content, yes, no, 420, 230);
    }

    private void showConfirmInDialog(Stage owner, String title, String message, Consumer<Boolean> callback) {
        Stage d = new Stage();
        d.initModality(Modality.APPLICATION_MODAL);
        d.initOwner(owner);
        d.initStyle(StageStyle.UNDECORATED);
        VBox content = makeDialogContent(message, "❓", COLOR_MUTED);
        Button yes = makeDialogBtn("Yes, Register", COLOR_RED, COLOR_RED_DK, true);
        Button no  = makeDialogBtn("Cancel",        "#1e2130", "#252a42",   false);
        yes.setOnAction(e -> { d.close(); callback.accept(true);  });
        no.setOnAction(e  -> { d.close(); callback.accept(false); });
        showDialog(d, title, content, yes, no, 390, 240);
    }

    private void showInfo(String title, String message) {
        Stage d = makeDialogStage(adminNameLabel);
        VBox content = makeDialogContent(message, "✓", "#4caf50");
        Button ok = makeDialogBtn("OK", COLOR_RED, COLOR_RED_DK, true);
        ok.setOnAction(e -> d.close());
        showDialog(d, title, content, ok, null, 390, 210);
    }

    private void showError(String title, String message) {
        Stage d = makeDialogStage(adminNameLabel);
        VBox content = makeDialogContent(message, "✕", COLOR_RED);
        Button ok = makeDialogBtn("OK", COLOR_RED, COLOR_RED_DK, true);
        ok.setOnAction(e -> d.close());
        showDialog(d, title, content, ok, null, 390, 210);
    }

    // ── Dialog plumbing ───────────────────────────────────────────────────────
    private void showDialog(Stage d, String title, VBox content,
                            Button primary, Button secondary,
                            double w, double h) {
        HBox btns = new HBox(10);
        btns.setAlignment(Pos.CENTER_RIGHT);
        btns.setPadding(new Insets(0, 24, 22, 24));
        if (secondary != null) btns.getChildren().addAll(primary, secondary);
        else                    btns.getChildren().add(primary);

        VBox root = new VBox(buildDialogTitleBar(d, title), content, btns);
        root.setStyle(
                "-fx-background-color:" + BG_MAIN + ";" +
                        "-fx-border-color:" + COLOR_BORDER + ";" +
                        "-fx-border-width:1.5;-fx-border-radius:14;-fx-background-radius:14;");
        root.setEffect(new DropShadow(28, Color.web("#000000", 0.7)));

        Scene sc = new Scene(root, w, h);
        sc.setFill(Color.TRANSPARENT);
        d.setScene(sc);
        d.showAndWait();
    }

    private Stage makeDialogStage(javafx.scene.Node owner) {
        Stage d = new Stage();
        d.initModality(Modality.APPLICATION_MODAL);
        d.initStyle(StageStyle.UNDECORATED);
        try { d.initOwner(owner.getScene().getWindow()); } catch (Exception ignored) {}
        return d;
    }

    private HBox buildDialogTitleBar(Stage d, String title) {
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-family:'Inter';-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + COLOR_TEXT + ";");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label x = new Label("✕");
        applyCloseBtnStyle(x, false);
        x.setOnMouseEntered(e -> applyCloseBtnStyle(x, true));
        x.setOnMouseExited(e  -> applyCloseBtnStyle(x, false));
        x.setOnMouseClicked(e -> d.close());
        HBox bar = new HBox(lbl, sp, x);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 20, 14, 20));
        bar.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:" + COLOR_BORDER + ";-fx-border-width:0 0 1 0;");
        return bar;
    }

    private VBox makeDialogContent(String message, String icon, String iconColor) {
        StackPane circle = new StackPane();
        circle.setMinSize(46, 46); circle.setMaxSize(46, 46);
        circle.setStyle(
                "-fx-background-color:" + iconColor + "22;" +
                        "-fx-background-radius:23;" +
                        "-fx-border-color:" + iconColor + "44;" +
                        "-fx-border-radius:23;-fx-border-width:1.5;");
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size:17px;-fx-text-fill:" + iconColor + ";");
        circle.getChildren().add(iconLbl);

        Label msgLbl = new Label(message);
        msgLbl.setWrapText(true);
        msgLbl.setStyle("-fx-font-family:'Inter';-fx-font-size:13px;-fx-text-fill:" + COLOR_MUTED + ";-fx-line-spacing:3;");

        VBox box = new VBox(12, circle, msgLbl);
        box.setPadding(new Insets(20, 24, 14, 24));
        return box;
    }

    private Button makeDialogBtn(String text, String bg, String hover, boolean primary) {
        Button b = new Button(text);
        b.setPrefHeight(38);
        String base  = "-fx-background-color:" + bg +
                ";-fx-text-fill:white;-fx-font-family:'Inter';-fx-font-size:12px;" +
                "-fx-font-weight:bold;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:0 18;" +
                (primary ? "" : "-fx-border-color:" + COLOR_BORDER + ";-fx-border-radius:8;-fx-border-width:1;");
        String hoverStyle = base.replace(bg, hover);
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hoverStyle));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }

    // ════════════════════════════════════════════════════════════════════════
    // VALIDATION
    // ════════════════════════════════════════════════════════════════════════
    private boolean validateAccountInfo() {
        String val = usernameField.getText().trim();
        if (val.isBlank()) {
            usernameError.setText("Username is required."); return false;
        }
        if (!val.equals(currentProfile.username()) && AdminDAO.isUsernameTaken(val)) {
            usernameError.setText("Username is already taken."); return false;
        }
        return true;
    }

    private void clearAllErrors() {
        usernameError.setText("");
        clearPasswordErrors();
    }

    private void clearPasswordErrors() {
        currentPassError.setText("");
        newPassError.setText("");
        retypePassError.setText("");
    }

    // ════════════════════════════════════════════════════════════════════════
    // PICK PROFILE IMAGE
    // ════════════════════════════════════════════════════════════════════════
    private void pickProfileImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Profile Image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = chooser.showOpenDialog(avatarStackPane.getScene().getWindow());
        if (file == null) return;
        pendingImagePath = file.getAbsolutePath();
        try {
            profileImage.setImage(new Image(file.toURI().toString()));
        } catch (Exception e) {
            showError("Image Error", "Could not load the selected image.");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // IMAGE HELPERS
    // ════════════════════════════════════════════════════════════════════════
    private void loadProfileImage(String path) {
        if (path == null || path.isBlank() || path.equalsIgnoreCase("none.png")) {
            setDefaultImage(); return;
        }
        File f = new File(path);
        if (f.exists()) {
            try { profileImage.setImage(new Image(f.toURI().toString())); return; }
            catch (Exception ignored) {}
        }
        try {
            profileImage.setImage(new Image(
                    getClass().getResource("/codes/acegym/image/" + path).toExternalForm()));
        } catch (Exception ignored) { setDefaultImage(); }
    }

    private void setDefaultImage() {
        try {
            profileImage.setImage(new Image(
                    getClass().getResource("/codes/acegym/image/admin-user.png").toExternalForm()));
        } catch (Exception ignored) {}
    }
}