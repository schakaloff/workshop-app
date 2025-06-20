package Skeletons;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Customer {
    private final StringProperty id;

    private final StringProperty firstName;
    private final StringProperty lastName;
    private final StringProperty additionalName;

    private final StringProperty phone;
    private final StringProperty additionalPhone;

    private final StringProperty address;
    private final StringProperty postalCode;
    private final StringProperty town;

    public Customer(String id, String firstName, String lastName, String additionalName, String phone, String additionalPhone, String address, String postalCode, String town){
        this.id = new SimpleStringProperty(id);
        this.firstName = new SimpleStringProperty(firstName);
        this.lastName = new SimpleStringProperty(lastName);
        this.additionalName = new SimpleStringProperty(additionalName);

        this.phone = new SimpleStringProperty(phone);
        this.additionalPhone = new SimpleStringProperty(additionalPhone);
        this.address = new SimpleStringProperty(address);
        this.postalCode = new SimpleStringProperty(postalCode);
        this.town = new SimpleStringProperty(town);
    }
    public StringProperty idProperty(){ return id;}
    public String getId(){return id.get();}
    public void setId(String value){id.set(value);}


    public StringProperty firstNameProperty() {
        return firstName;
    }
    public String getFirstName() {
        return firstName.get();
    }
    public void setFirstName(String value) {
        firstName.set(value);
    }

    public StringProperty lastNameProperty() {
        return lastName;
    }
    public String getLastName() {
        return lastName.get();
    }
    public void setLastName(String value) {
        lastName.set(value);
    }

    public StringProperty additionalNameProperty() {
        return additionalName;
    }
    public String getAdditionalName() {
        return additionalName.get();
    }
    public void setAdditionalName(String value) {
        additionalName.set(value);
    }

    public StringProperty phoneProperty() {
        return phone;
    }
    public String getPhone() {
        return phone.get();
    }
    public void setPhone(String value) {
        phone.set(value);
    }

    public StringProperty additionalPhoneProperty() {
        return additionalPhone;
    }
    public String getAdditionalPhone() {
        return additionalPhone.get();
    }
    public void setAdditionalPhone(String value) {
        additionalPhone.set(value);
    }

    public StringProperty addressProperty() {
        return address;
    }
    public String getAddress() {
        return address.get();
    }
    public void setAddress(String value) {
        address.set(value);
    }

    public StringProperty postalCodeProperty() {
        return postalCode;
    }
    public String getPostalCode() {
        return postalCode.get();
    }
    public void setPostalCode(String value) {
        postalCode.set(value);
    }

    public StringProperty townProperty() {
        return town;
    }
    public String getTown() {
        return town.get();
    }
    public void setTown(String value) {
        town.set(value);
    }



}
