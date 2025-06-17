package Skeletons;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class WorkOrder {
    /*
    We use StringProperty because they will let watch and update UI automatically (but its an abstract class)
    there for we use SimpleStringProperty (like a build in array);
     */
    private final StringProperty workorderNumber;
    private final StringProperty status;
    private final StringProperty createdAt; //

    private final StringProperty vendorId;
    private final StringProperty warrantyNumber;

    private final StringProperty type;
    private final StringProperty model;
    private final StringProperty serialNumber;
    private final StringProperty problemDesc;

    public WorkOrder(String workorderNumber, String status, String type, String createdAt, String vendorId, String warrantyNumber, String model, String serialNumber, String problemDesc){
        this.workorderNumber = new SimpleStringProperty(workorderNumber);
        this.status          = new SimpleStringProperty(status);
        this.type            = new SimpleStringProperty(type);
        this.createdAt       = new SimpleStringProperty(createdAt);

        this.vendorId        = new SimpleStringProperty(vendorId);
        this.warrantyNumber  = new SimpleStringProperty(warrantyNumber);

        this.model           = new SimpleStringProperty(model);
        this.serialNumber    = new SimpleStringProperty(serialNumber);
        this.problemDesc     = new SimpleStringProperty(problemDesc);

    }

    public StringProperty vendorIdProperty(){return vendorId;}
    public String getVendorId(){return vendorId.get();}
    public void setVendorId(String value){vendorId.set(value);}

    public StringProperty  warrantyNumberProperty(){return warrantyNumber;}
    public String getWarrantyNumber(){return warrantyNumber.get();}
    public void setWarrantyNumber(String value){warrantyNumber.set(value);}

    /*
    for javafx there are 3 steps -> 1)get accessor, 2)getters, and 3)setters
    */
    //1)
    public StringProperty workorderNumberProperty(){ //accessor
        return workorderNumber;
    }
    //2)
    public String getWorkorderNumber(){ //get
        return workorderNumber.get();
    }
    //3
    public void setWorkorderNumber(String value){ //set
        workorderNumber.set(value);
    }


    public StringProperty typeProperty(){return type;}
    public String getType(){return type.get();}
    public void setType(String value){type.set(value);}

    public StringProperty modelProperty(){return model; }
    public String getModel(){return model.get();}
    public void setModel(String value){model.set(value);}

    public StringProperty serialNumberProperty(){return serialNumber;}
    public String getSerialNumber(){return serialNumber.get();}
    public void  setSerialNumber(String value){serialNumber.set(value);}

    public StringProperty problemDescProperty(){return problemDesc;}
    public String getProblemDesc(){return problemDesc.get();}
    public void setProblemDesc(String value){problemDesc.set(value);}

    public StringProperty statusProperty(){
        return status;
    }
    public String getStatus(){
        return status.get();
    }
    public void setStatus(String value){
        status.set(value);
    }

    public StringProperty createdAtProperty() {return createdAt;}
    public String getCreatedAt() {
        return createdAt.get();
    }
    public void setCreatedAt(String value) {
        createdAt.set(value);
    }




}
