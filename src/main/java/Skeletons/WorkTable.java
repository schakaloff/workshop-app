package Skeletons;

import javafx.beans.property.*;

import java.time.LocalDate;

public class WorkTable {
    private final ObjectProperty<LocalDate> date;
    private final StringProperty tech;
    private final StringProperty description;
    private final DoubleProperty price;

    public WorkTable(LocalDate date, String tech, String description, double price){
        this.date = new SimpleObjectProperty(date);
        this.tech = new SimpleStringProperty(tech);
        this.description = new SimpleStringProperty(description);
        this.price = new SimpleDoubleProperty(price);
    }

    public ObjectProperty<LocalDate> dateProperty(){return date;}
    public LocalDate getDate(){return date.get();}
    public void setDate(LocalDate d){date.set(d);}

    public StringProperty techProperty(){return tech;}
    public String getTech(){return tech.get();}
    public void setTech(String t){tech.set(t);}

    public StringProperty descriptionProperty(){ return description; }
    public String getDescription(){ return description.get(); }
    public void setDescription(String d){ description.set(d); }

    public DoubleProperty priceProperty(){ return price; }
    public double getPrice(){ return price.get(); }
    public void setPrice(double p){ price.set(p); }

}
