package Controllers;

import Skeletons.Customer;
import Skeletons.WorkOrder;
import javafx.fxml.FXML;
import javafx.scene.text.Text;


public class FirstInvoiceController {
    @FXML private Text cxFullNameTXT;
    @FXML private Text salesPersonTXT;
    @FXML private Text PONumberTXT;
    @FXML private Text paymentMethodTXT;
    @FXML private Text depositTXT;
    @FXML private Text woNumberTXT;
    @FXML private Text dateTXT;

    public void initData(WorkOrder wo, Customer co, String method, double amount, String tech, String date) {
        cxFullNameTXT.setText(co.getFirstName() + " " + co.getLastName());
        salesPersonTXT.setText(tech);
        PONumberTXT.setText(String.valueOf(wo.getWorkorderNumber()));
        paymentMethodTXT.setText(method);
        depositTXT.setText(String.format("CDN$%.2f", amount));
        woNumberTXT.setText(String.valueOf(wo.getWorkorderNumber()));
        dateTXT.setText(date);
    }

}
