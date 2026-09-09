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
    @FXML private Text addressText;
    @FXML private Text townText;
    @FXML private Text postalText;

    @FXML private Text warrantyText;
    @FXML private Text warrantyNumLabel;
    @FXML private Text warrantyNumText;
    @FXML private Text poLabel;
    @FXML private Text poText;

    @FXML private Text typeText;
    @FXML private Text descriptionText;
    @FXML private Text serialNumberText;
    @FXML private Text problemTextArea;


    public void initData(WorkOrder wo, Customer co){
        String firstName = co.getFirstName();
        String lastName = co.getLastName();
        String fullName = lastName + ", " + firstName;
        String firstNLast = firstName +" "+ lastName;

        String orderID = String.valueOf(wo.getWorkorderNumber());

        workIDText.setText(orderID);
        dateText.setText(wo.getCreatedAt());
        customerNameText.setText(fullName);
        custIDText.setText(co.getId());
        // Order can name its own contact/phone (e.g. an employee who dropped off
        // a company's item) distinct from the account owner — use it when set.
        String contactName = wo.getContactName();
        String contactPhone = wo.getContactPhone();
        contactText.setText(contactName != null && !contactName.isBlank() ? contactName : firstNLast);
        phoneText.setText(formatPhone(contactPhone != null && !contactPhone.isBlank() ? contactPhone : co.getPhone()));
        addressText.setText(co.getAddress());
        townText.setText(co.getTown());
        postalText.setText(co.getPostalCode());


        warrantyText.setText(wo.getVendorId());
        warrantyNumText.setText(wo.getWarrantyNumber());
        String po = wo.getPoNumber();
        poText.setText(po == null || po.isBlank() ? "None" : po);
        layoutWarrantyRow();

        typeText.setText(wo.getType());
        descriptionText.setText(wo.getModel());
        serialNumberText.setText(wo.getSerialNumber());
        problemTextArea.setText(wo.getProblemDesc());

    }

    // Formats to +1(XXX)XXX-XXXX regardless of how the phone digits were stored.
    private static String formatPhone(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 11 && digits.startsWith("1")) digits = digits.substring(1);
        if (digits.length() != 10) return raw;
        return "+1(" + digits.substring(0, 3) + ")" + digits.substring(3, 6) + "-" + digits.substring(6);
    }

    // Vendor names vary a lot in length, so the "Warranty#:" / "PO#:" columns
    // that follow the vendor name can't sit at fixed x positions without risking
    // overlap — push each one out based on the measured width of what's before it.
    private static final double GAP = 16.0;

    private void layoutWarrantyRow() {
        double x = warrantyText.getLayoutX() + warrantyText.getLayoutBounds().getWidth() + GAP;
        warrantyNumLabel.setLayoutX(x);

        x = warrantyNumLabel.getLayoutX() + warrantyNumLabel.getLayoutBounds().getWidth() + 6;
        warrantyNumText.setLayoutX(x);

        x = warrantyNumText.getLayoutX() + warrantyNumText.getLayoutBounds().getWidth() + GAP;
        poLabel.setLayoutX(x);

        x = poLabel.getLayoutX() + poLabel.getLayoutBounds().getWidth() + 6;
        poText.setLayoutX(x);
    }
}
