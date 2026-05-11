package Skeletons;

import javafx.beans.property.*;

public class Vendor {
    private final IntegerProperty id;
    private final StringProperty name;
    private final BooleanProperty paysLabour;
    private final BooleanProperty paysParts;
    private final BooleanProperty paysPst;
    private final BooleanProperty paysGst;
    private final StringProperty address;
    private final StringProperty city;
    private final StringProperty province;
    private final StringProperty postal;
    private final StringProperty contact;
    private final StringProperty phone;

    public Vendor(int id, String name, boolean paysLabour, boolean paysParts,
                  boolean paysPst, boolean paysGst,
                  String address, String city, String province,
                  String postal, String contact, String phone) {
        this.id         = new SimpleIntegerProperty(id);
        this.name       = new SimpleStringProperty(name);
        this.paysLabour = new SimpleBooleanProperty(paysLabour);
        this.paysParts  = new SimpleBooleanProperty(paysParts);
        this.paysPst    = new SimpleBooleanProperty(paysPst);
        this.paysGst    = new SimpleBooleanProperty(paysGst);
        this.address  = new SimpleStringProperty(address);
        this.city     = new SimpleStringProperty(city);
        this.province = new SimpleStringProperty(province);
        this.postal   = new SimpleStringProperty(postal);
        this.contact  = new SimpleStringProperty(contact);
        this.phone    = new SimpleStringProperty(phone);
    }

    public int     getId()          { return id.get(); }
    public String  getName()        { return name.get(); }
    public boolean isPaysLabour()   { return paysLabour.get(); }
    public boolean isPaysParts()    { return paysParts.get(); }
    public boolean isPaysPst()      { return paysPst.get(); }
    public boolean isPaysGst()      { return paysGst.get(); }

    public StringProperty  nameProperty()       { return name; }
    public BooleanProperty paysLabourProperty() { return paysLabour; }
    public BooleanProperty paysPartsProperty()  { return paysParts; }
    public BooleanProperty paysPstProperty()    { return paysPst; }
    public BooleanProperty paysGstProperty()    { return paysGst; }

    public void setName(String v)       { name.set(v); }
    public void setPaysLabour(boolean v){ paysLabour.set(v); }
    public void setPaysParts(boolean v) { paysParts.set(v); }
    public void setPaysPst(boolean v)   { paysPst.set(v); }
    public void setPaysGst(boolean v)   { paysGst.set(v); }

    public String getAddress()  { return address.get(); }
    public String getCity()     { return city.get(); }
    public String getProvince() { return province.get(); }
    public String getPostal()   { return postal.get(); }
    public String getContact()  { return contact.get(); }
    public String getPhone()    { return phone.get(); }

    public void setAddress(String v)  { address.set(v); }
    public void setCity(String v)     { city.set(v); }
    public void setProvince(String v) { province.set(v); }
    public void setPostal(String v)   { postal.set(v); }
    public void setContact(String v)  { contact.set(v); }
    public void setPhone(String v)    { phone.set(v); }
}
