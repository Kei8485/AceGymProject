package codes.acegym.Objects;

import javafx.beans.property.*;

public class Client {

    private final IntegerProperty clientID   = new SimpleIntegerProperty();
    private final StringProperty  firstName  = new SimpleStringProperty();
    private final StringProperty  lastName   = new SimpleStringProperty();
    private final StringProperty  email      = new SimpleStringProperty();
    private final StringProperty  contact    = new SimpleStringProperty();
    private final StringProperty  clientType = new SimpleStringProperty();

    public Client(int clientID, String firstName, String lastName,
                  String email, String contact, String clientType) {
        this.clientID.set(clientID);
        this.firstName.set(firstName);
        this.lastName.set(lastName);
        this.email.set(email);
        this.contact.set(contact);
        this.clientType.set(clientType);
    }

    // ── Property accessors ──────────────────────────────────────────────────
    public IntegerProperty clientIDProperty()   { return clientID; }
    public StringProperty  firstNameProperty()  { return firstName; }
    public StringProperty  lastNameProperty()   { return lastName; }
    public StringProperty  emailProperty()      { return email; }
    public StringProperty  contactProperty()    { return contact; }
    public StringProperty  clientTypeProperty() { return clientType; }

    // ── Plain getters ───────────────────────────────────────────────────────
    public int    getClientID()   { return clientID.get(); }
    public String getFirstName()  { return firstName.get(); }
    public String getLastName()   { return lastName.get(); }
    public String getEmail()      { return email.get(); }
    public String getContact()    { return contact.get(); }
    public String getClientType() { return clientType.get(); }

    // ── Convenience ─────────────────────────────────────────────────────────
    public String getFullName() {
        return firstName.get() + " " + lastName.get();
    }

    @Override
    public String toString() {
        // Used by ComboBox to display the client name
        return getFullName();
    }
}