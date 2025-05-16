package main;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class WorkOrder {
    /*
    We use StringProperty because they will let watch and update UI automatically (but its an abbstract class)
    there for we use SimpleStringProperty (like a build in array);
     */
    private final StringProperty workorderNumber;
    private final StringProperty status;
    private final StringProperty description;
    private final StringProperty createdAt; //

    public WorkOrder(String workorderNumber, String status, String description, String createdAt){
        this.workorderNumber = new SimpleStringProperty(workorderNumber);
        this.status = new SimpleStringProperty(status);
        this.description = new SimpleStringProperty(description);
        this.createdAt = new SimpleStringProperty(createdAt);
    }
        /*
        for java fx there are 3 steps -> 1)get accessor, 2)getters, and 3)setters
        */
    //1)
    public StringProperty workorderNumberProperty(){ //accessor
        return workorderNumber;
    }
    //2)
    public String getWorkorderNumber(){ //get
        return workorderNumber.get();
    }
    //3)
    public void setWorkorderNumber(String value){ //set
        workorderNumber.set(value);
    }

    public StringProperty statusProperty(){
        return status;
    }

    public String getStatus(){
        return status.get();
    }

    public void setStatus(String value){
        status.set(value);
    }

    public StringProperty descriptionProperty(){
        return description;
    }

    public String getDescription(){
        return description.get();
    }

    public void setDescription(String value){
        description.set(value);
    }

    public StringProperty createdAtProperty() {
        return createdAt;
    }

    public String getCreatedAt() {
        return createdAt.get();
    }

    public void setCreatedAt(String value) {
        createdAt.set(value);
    }




}
