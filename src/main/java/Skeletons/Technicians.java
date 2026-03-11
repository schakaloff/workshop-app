package Skeletons;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Technicians {

    private final StringProperty userName;
    private final StringProperty fName;
    private final StringProperty lName;
    private final StringProperty role;

    public Technicians(String userName, String fName, String lName, String role) {
        this.userName = new SimpleStringProperty(userName);
        this.fName = new SimpleStringProperty(fName);
        this.lName = new SimpleStringProperty(lName);
        this.role = new SimpleStringProperty(role);
    }

    // username
    public StringProperty userNameProperty() { return userName; }
    public String getUserName() { return userName.get(); }
    public void setUserName(String s) { userName.set(s); }

    // first name
    public StringProperty fNameProperty() { return fName; }
    public String getFName() { return fName.get(); }
    public void setFName(String s) { fName.set(s); }

    // last name
    public StringProperty lNameProperty() { return lName; }
    public String getLName() { return lName.get(); }
    public void setLName(String s) { lName.set(s); }

    // role
    public StringProperty roleProperty() { return role; }
    public String getRole() { return role.get(); }
    public void setRole(String s) { role.set(s); }
}