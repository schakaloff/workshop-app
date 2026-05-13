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
        PONumberTXT.setText(String.valueOf(wo.getWorkorderNumber()));
        paymentMethodTXT.setText(method);
        depositTXT.setText(String.format("CDN $%.2f", amount));
        totalTXT.setText(String.format("CDN $%.2f", amount));
        woNumberTXT.setText(String.valueOf(wo.getWorkorderNumber()));
        dateTXT.setText(date);
        woRefTXT.setText(String.valueOf(wo.getWorkorderNumber()));
        dateRefTXT.setText(date);
    }
}