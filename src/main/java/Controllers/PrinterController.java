package Controllers;

import Skeletons.Customer;
import Skeletons.WorkOrder;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;

public class PrinterController {
    @FXML private Label lblWorkorderNumber;
    @FXML private Label lblCreatedAt;
    @FXML private Label lblModel;
    @FXML private Label lblSerialNumber;
    @FXML private TextArea ProblemDesc;

    @FXML private Text customerNameText;

    public void initData(WorkOrder wo, Customer co){
//        lblWorkorderNumber.setText("WO#: "     + wo.getWorkorderNumber());
//        lblCreatedAt.setText("Created: "+ wo.getCreatedAt());
//        lblModel.setText("Model: "  + wo.getModel());
//        lblSerialNumber.setText("SN: "     + wo.getSerialNumber());
//        ProblemDesc.setText("Problem: "+ wo.getProblemDesc());

//        String firstName = co.getFirstName();
//        String lastName = co.getLastName();
//        String fullName = lastName + ", " + firstName;
//
//
//        customerNameText.setText(fullName);



    }
}
