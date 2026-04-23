package codes.acegym.Objects;

import javafx.beans.property.*;

public class Coach {

    private final IntegerProperty staffID          = new SimpleIntegerProperty();
    private final StringProperty  firstName        = new SimpleStringProperty();
    private final StringProperty  lastName         = new SimpleStringProperty();
    private final IntegerProperty trainingTypeID   = new SimpleIntegerProperty();
    private final StringProperty  trainingCategory = new SimpleStringProperty();
    private final StringProperty  systemRole       = new SimpleStringProperty();
    private final StringProperty  staffImage       = new SimpleStringProperty();

    // ── Full constructor (from DB) ──────────────────────────────────────────
    public Coach(int staffID, String firstName, String lastName,
                 int trainingTypeID, String trainingCategory,
                 String systemRole, String staffImage) {
        this.staffID.set(staffID);
        this.firstName.set(firstName);
        this.lastName.set(lastName);
        this.trainingTypeID.set(trainingTypeID);
        this.trainingCategory.set(trainingCategory);
        this.systemRole.set(systemRole);
        this.staffImage.set(staffImage);
    }

    // ── Property accessors ──────────────────────────────────────────────────
    public IntegerProperty staffIDProperty()          { return staffID; }
    public StringProperty  firstNameProperty()        { return firstName; }
    public StringProperty  lastNameProperty()         { return lastName; }
    public IntegerProperty trainingTypeIDProperty()   { return trainingTypeID; }
    public StringProperty  trainingCategoryProperty() { return trainingCategory; }
    public StringProperty  systemRoleProperty()       { return systemRole; }
    public StringProperty  staffImageProperty()       { return staffImage; }

    // ── Plain getters ───────────────────────────────────────────────────────
    public int    getStaffID()          { return staffID.get(); }
    public String getFirstName()        { return firstName.get(); }
    public String getLastName()         { return lastName.get(); }
    public int    getTrainingTypeID()   { return trainingTypeID.get(); }
    public String getTrainingCategory() { return trainingCategory.get(); }
    public String getSystemRole()       { return systemRole.get(); }
    public String getStaffImage()       { return staffImage.get(); }

    // ── Plain setters (needed by edit form) ─────────────────────────────────
    public void setFirstName(String v)        { firstName.set(v); }
    public void setLastName(String v)         { lastName.set(v); }
    public void setTrainingTypeID(int v)      { trainingTypeID.set(v); }
    public void setTrainingCategory(String v) { trainingCategory.set(v); }
    public void setSystemRole(String v)       { systemRole.set(v); }
    public void setStaffImage(String v)       { staffImage.set(v); }

    // ── Convenience ─────────────────────────────────────────────────────────
    public String getFullName() {
        return firstName.get() + " " + lastName.get();
    }
}