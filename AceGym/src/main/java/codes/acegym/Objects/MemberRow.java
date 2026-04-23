package codes.acegym.Objects;

import javafx.beans.property.*;

/**
 * Flat view-model used by the ViewMembers table.
 * Combines data from ClientTable, MembershipTable, ReceiptTable,
 * PaymentPeriodTable, RateTable, TrainingTypeTable, and StaffTable.
 */
public class MemberRow {

    // ── Identity ─────────────────────────────────────────────────────────────
    private final IntegerProperty clientID         = new SimpleIntegerProperty();
    private final StringProperty  fullName         = new SimpleStringProperty();
    private final StringProperty  contact          = new SimpleStringProperty();
    private final StringProperty  email            = new SimpleStringProperty();
    private final StringProperty  clientType       = new SimpleStringProperty(); // "Member" | "Non Member"

    // ── Membership ────────────────────────────────────────────────────────────
    private final StringProperty  membershipApplied  = new SimpleStringProperty();  // formatted or "—"
    private final StringProperty  membershipExpired  = new SimpleStringProperty();  // formatted or "—"
    private final StringProperty  membershipStatus   = new SimpleStringProperty();  // "Active" | "Expired" | "None"

    // ── Last payment-period plan ──────────────────────────────────────────────
    private final StringProperty  lastPaymentPeriod  = new SimpleStringProperty();  // Daily / Monthly / Yearly / "—"
    private final StringProperty  lastPaymentDate    = new SimpleStringProperty();  // formatted or "—"
    private final StringProperty  planExpiry         = new SimpleStringProperty();  // formatted or "—"
    private final StringProperty  planStatus         = new SimpleStringProperty();  // "Active" | "Expired" | "No Record"

    // ── Coach / training ──────────────────────────────────────────────────────
    private final StringProperty  coachName          = new SimpleStringProperty();
    private final StringProperty  trainingCategory   = new SimpleStringProperty();

    // ── Last receipt amount ───────────────────────────────────────────────────
    private final DoubleProperty  lastPaymentAmount  = new SimpleDoubleProperty();

    // ── Constructor ────────────────────────────────────────────────────────────
    public MemberRow(int clientID,
                     String fullName,
                     String contact,
                     String email,
                     String clientType,
                     String membershipApplied,
                     String membershipExpired,
                     String membershipStatus,
                     String lastPaymentPeriod,
                     String lastPaymentDate,
                     String planExpiry,
                     String planStatus,
                     String coachName,
                     String trainingCategory,
                     double lastPaymentAmount) {

        this.clientID.set(clientID);
        this.fullName.set(nvl(fullName, "Unknown"));
        this.contact.set(nvl(contact, "—"));
        this.email.set(nvl(email, "—"));
        this.clientType.set(nvl(clientType, "Non Member"));

        this.membershipApplied.set(nvl(membershipApplied, "—"));
        this.membershipExpired.set(nvl(membershipExpired, "—"));
        this.membershipStatus.set(nvl(membershipStatus, "None"));

        this.lastPaymentPeriod.set(nvl(lastPaymentPeriod, "—"));
        this.lastPaymentDate.set(nvl(lastPaymentDate, "—"));
        this.planExpiry.set(nvl(planExpiry, "—"));
        this.planStatus.set(nvl(planStatus, "No Record"));

        this.coachName.set(nvl(coachName, "Unassigned"));
        this.trainingCategory.set(nvl(trainingCategory, "None"));
        this.lastPaymentAmount.set(lastPaymentAmount);
    }

    // ── Property accessors ────────────────────────────────────────────────────
    public IntegerProperty clientIDProperty()         { return clientID; }
    public StringProperty  fullNameProperty()         { return fullName; }
    public StringProperty  contactProperty()          { return contact; }
    public StringProperty  emailProperty()            { return email; }
    public StringProperty  clientTypeProperty()       { return clientType; }
    public StringProperty  membershipAppliedProperty(){ return membershipApplied; }
    public StringProperty  membershipExpiredProperty(){ return membershipExpired; }
    public StringProperty  membershipStatusProperty() { return membershipStatus; }
    public StringProperty  lastPaymentPeriodProperty(){ return lastPaymentPeriod; }
    public StringProperty  lastPaymentDateProperty()  { return lastPaymentDate; }
    public StringProperty  planExpiryProperty()       { return planExpiry; }
    public StringProperty  planStatusProperty()       { return planStatus; }
    public StringProperty  coachNameProperty()        { return coachName; }
    public StringProperty  trainingCategoryProperty() { return trainingCategory; }
    public DoubleProperty  lastPaymentAmountProperty(){ return lastPaymentAmount; }

    // ── Plain getters ────────────────────────────────────────────────────────
    public int    getClientID()           { return clientID.get(); }
    public String getFullName()           { return fullName.get(); }
    public String getContact()            { return contact.get(); }
    public String getEmail()              { return email.get(); }
    public String getClientType()         { return clientType.get(); }
    public String getMembershipApplied()  { return membershipApplied.get(); }
    public String getMembershipExpired()  { return membershipExpired.get(); }
    public String getMembershipStatus()   { return membershipStatus.get(); }
    public String getLastPaymentPeriod()  { return lastPaymentPeriod.get(); }
    public String getLastPaymentDate()    { return lastPaymentDate.get(); }
    public String getPlanExpiry()         { return planExpiry.get(); }
    public String getPlanStatus()         { return planStatus.get(); }
    public String getCoachName()          { return coachName.get(); }
    public String getTrainingCategory()   { return trainingCategory.get(); }
    public double getLastPaymentAmount()  { return lastPaymentAmount.get(); }

    // ── Helper ────────────────────────────────────────────────────────────────
    private static String nvl(String val, String fallback) {
        return (val == null || val.isBlank()) ? fallback : val;
    }
}