package Controllers;

import Skeletons.Customer;
import Skeletons.WorkOrder;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;

public class PrinterController {
    @FXML private Text workIDText;
    @FXML private Text dateText;

    @FXML private Text customerNameText;
    @FXML private Text custIDText;
    @FXML private Text contactText;
    @FXML private Text phoneText;

    @FXML private Text warrantyText;
    @FXML private Text warrantyNumText;

    @FXML private Text typeText;
    @FXML private Text descriptionText;
    @FXML private Text serialNumberText;
    @FXML private Text problemTextArea;


    public void initData(WorkOrder wo, Customer co){
        String firstName = co.getFirstName();
        String lastName = co.getLastName();
        String fullName = lastName + ", " + firstName;
        String firstNLast = firstName +" "+ lastName;

        workIDText.setText(wo.getWorkorderNumber());
        dateText.setText(wo.getCreatedAt());
        customerNameText.setText(fullName);
        custIDText.setText(co.getId());
        contactText.setText(firstNLast);
        phoneText.setText("+1" + co.getPhone());


        warrantyText.setText(wo.getVendorId());
        warrantyNumText.setText(wo.getWarrantyNumber());

        typeText.setText(wo.getType());
        descriptionText.setText(wo.getModel());
        serialNumberText.setText(wo.getSerialNumber());
        problemTextArea.setText(wo.getProblemDesc());

    }
}
