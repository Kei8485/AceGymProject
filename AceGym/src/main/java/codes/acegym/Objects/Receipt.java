package codes.acegym.Objects;

import javafx.beans.property.*;

public class Receipt {
    private final IntegerProperty receiptID       = new SimpleIntegerProperty();
    private final IntegerProperty clientID        = new SimpleIntegerProperty();
    private final StringProperty  firstName       = new SimpleStringProperty();
    private final StringProperty  lastName        = new SimpleStringProperty();
    private final StringProperty  paymentType     = new SimpleStringProperty();
    private final DoubleProperty  total           = new SimpleDoubleProperty();
    private final StringProperty  date            = new SimpleStringProperty();
    private final StringProperty  status          = new SimpleStringProperty("Paid");

    // New fields
    private final StringProperty  trainingCategory = new SimpleStringProperty();
    private final StringProperty  paymentPeriod    = new SimpleStringProperty();
    private final StringProperty  coachName        = new SimpleStringProperty();
    private final StringProperty  membershipType   = new SimpleStringProperty();

    // Original constructor — still works everywhere you already use it
    public Receipt(int receiptID, int clientID, String firstName, String lastName,
                   String paymentType, double total, String date) {
        this(receiptID, clientID, firstName, lastName, paymentType, total, date,
                null, null, null, null);
    }

    // Full constructor with new fields
    public Receipt(int receiptID, int clientID, String firstName, String lastName,
                   String paymentType, double total, String date,
                   String trainingCategory, String paymentPeriod,
                   String coachName, String membershipType) {
        this.receiptID.set(receiptID);
        this.clientID.set(clientID);
        this.firstName.set(firstName);
        this.lastName.set(lastName);
        this.paymentType.set(paymentType);
        this.total.set(total);
        this.date.set(date);
        this.trainingCategory.set(trainingCategory);
        this.paymentPeriod.set(paymentPeriod);
        this.coachName.set(coachName);
        this.membershipType.set(membershipType);
    }

    // ── Property accessors ──
    public IntegerProperty receiptIDProperty()        { return receiptID; }
    public IntegerProperty clientIDProperty()         { return clientID; }
    public StringProperty  firstNameProperty()        { return firstName; }
    public StringProperty  lastNameProperty()         { return lastName; }
    public StringProperty  paymentTypeProperty()      { return paymentType; }
    public DoubleProperty  totalProperty()            { return total; }
    public StringProperty  dateProperty()             { return date; }
    public StringProperty  statusProperty()           { return status; }
    public StringProperty  trainingCategoryProperty() { return trainingCategory; }
    public StringProperty  paymentPeriodProperty()    { return paymentPeriod; }
    public StringProperty  coachNameProperty()        { return coachName; }
    public StringProperty  membershipTypeProperty()   { return membershipType; }

    // ── Plain getters ──
    public int    getReceiptID()         { return receiptID.get(); }
    public int    getClientID()          { return clientID.get(); }
    public String getFirstName()         { return firstName.get(); }
    public String getLastName()          { return lastName.get(); }
    public String getPaymentType()       { return paymentType.get(); }
    public double getTotal()             { return total.get(); }
    public String getDate()              { return date.get(); }
    public String getStatus()            { return status.get(); }
    public String getTrainingCategory()  { return trainingCategory.get(); }
    public String getPaymentPeriod()     { return paymentPeriod.get(); }
    public String getCoachName()         { return coachName.get(); }
    public String getMembershipType()    { return membershipType.get(); }
}