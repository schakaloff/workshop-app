package Skeletons;

import javafx.beans.property.*;

public class PartTable {
    private final StringProperty name;
    private final IntegerProperty quantity;
    private final DoubleProperty price;
    private final DoubleProperty totalPrice;

    public PartTable(String name, int quantity, double price, double totalPrice){
        this.name = new SimpleStringProperty(name);
        this.quantity = new SimpleIntegerProperty(quantity);
        this.price = new SimpleDoubleProperty(price);
        this.totalPrice = new SimpleDoubleProperty(totalPrice);
    }

    public StringProperty nameProperty(){return name;}
    public String getName(){return name.get();}
    public void setName(String n){name.set(n);}

    public IntegerProperty quantityProperty(){return quantity;}
    public int getQuantity(){return quantity.get();}
    public void setQuantity(int q){quantity.set(q);}

    public DoubleProperty priceProperty(){return price;}
    public double getPrice(){return price.get();}
    public void setPrice(double p){price.set(p);}

    public DoubleProperty totalPriceProperty(){return totalPrice;}
    public double getTotalPrice(){return totalPrice.get();}
    public void setTotalPrice(double p){totalPrice.set(p);}
}
