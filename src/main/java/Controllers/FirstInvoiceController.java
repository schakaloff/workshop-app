package Controllers;

import Skeletons.Customer;
import Skeletons.WorkOrder;
import javafx.fxml.FXML;
import javafx.scene.text.Text;

public class FirstInvoiceController {

    @FXML private Text cxFullNameTXT;
    @FXML private Text cxAddressTXT;
    @FXML private Text cxCityTXT;
    @FXML private Text cxPhoneTXT;
    @FXML private Text salesPersonTXT;
    @FXML private Text PONumberTXT;
    @FXML private Text paymentMethodTXT;
    @FXML private Text depositTXT;
    @FXML private Text totalTXT;
    @FXML private Text woNumberTXT;
    @FXML private Text dateTXT;
    @FXML private Text woRefTXT;
    @FXML private Text dateRefTXT;

    public void initData(WorkOrder wo, Customer co, String method, double amount, String tech, String date) {
        cxFullNameTXT.setText(co.getFirstName() + " " + co.getLastName());
        cxAddressTXT.setText(co.getAddress() != null ? co.getAddress() : "");
        cxCityTXT.setText((co.getTown() != null ? co.getTown() : "") +
                (co.getPostalCode() != null ? "  " + co.getPostalCode() : ""));
        cxPhoneTXT.setText(co.getPhone() != null ? co.getPhone() : "");

        salesPersonTXT.setText(tech);
        String po = wo.getPoNumber();
        PONumberTXT.setText(po == null || po.isBlank() ? "None" : po);
        paymentMethodTXT.setText(method);
        depositTXT.setText(String.format("CDN $%.2f", amount));
        totalTXT.setText(String.format("CDN $%.2f", amount));
        woNumberTXT.setText(String.valueOf(wo.getWorkorderNumber()));
        dateTXT.setText(date);
        woRefTXT.setText(String.valueOf(wo.getWorkorderNumber()));
        dateRefTXT.setText(date);

        // Amounts are left-anchored in the FXML, so a wide value ("CDN $1234.56")
        // runs into the bold label beside it — pin each one to the box's right edge.
        depositTXT.setLayoutX(AMOUNT_RIGHT_X - depositTXT.getLayoutBounds().getWidth());
        totalTXT.setLayoutX(AMOUNT_RIGHT_X - totalTXT.getLayoutBounds().getWidth());
    }

    private static final double AMOUNT_RIGHT_X = 575.0;
}