package Controllers;

import Controllers.DbRepo.ViewControllerQueries;
import Skeletons.Customer;
import Skeletons.WorkOrder;
import javafx.fxml.FXML;
import javafx.scene.text.Text;

public class InvoiceController {

    @FXML private Text cxFullNameTXT;
    @FXML private Text cxAddressTXT;
    @FXML private Text cxCityTXT;
    @FXML private Text cxPhoneTXT;
    @FXML private Text salesPersonTXT;
    @FXML private Text PONumberTXT;
    @FXML private Text paymentMethodTXT;
    @FXML private Text woNumberTXT;
    @FXML private Text dateTXT;
    @FXML private Text woRefTXT;
    @FXML private Text dateRefTXT;

    // line items
    @FXML private Text labourTXT;
    @FXML private Text partsTXT;
    @FXML private Text pstTXT;
    @FXML private Text gstTXT;
    @FXML private Text subtotalTXT;
    @FXML private Text depositDeductTXT;
    @FXML private Text totalDueTXT;

    public void initData(WorkOrder wo, Customer co, String method, double amount, String tech, String date) {
        // load line items from DB
        double labour  = ViewControllerQueries.labourTotalDb(wo.getWorkorderNumber());
        double parts   = ViewControllerQueries.partsTotalDb(wo.getWorkorderNumber());
        double deposit = wo.getDepositAmount();

        // get saved taxes from DB via ShopSettings calc
        double[] taxes = DB.ShopSettings.calcTaxes(
                labour, parts,
                wo.getVendorId() != null && !wo.getVendorId().isBlank(),
                co.getPstNumber() != null && !co.getPstNumber().isBlank(),
                co.getGstNumber() != null && !co.getGstNumber().isBlank()
        );
        double pst      = taxes[0];
        double gst      = taxes[1];
        double subtotal = labour + parts + pst + gst;
        double totalDue = Math.max(0, subtotal - deposit);

        // customer
        cxFullNameTXT.setText(co.getFirstName() + " " + co.getLastName());
        cxAddressTXT.setText(co.getAddress() != null ? co.getAddress() : "");
        cxCityTXT.setText((co.getTown() != null ? co.getTown() : "") +
                (co.getPostalCode() != null ? "  " + co.getPostalCode() : ""));
        cxPhoneTXT.setText(co.getPhone() != null ? co.getPhone() : "");

        // header meta
        salesPersonTXT.setText(tech);
        PONumberTXT.setText(String.valueOf(wo.getWorkorderNumber()));
        woNumberTXT.setText(String.valueOf(wo.getWorkorderNumber()));
        dateTXT.setText(date);
        woRefTXT.setText(String.valueOf(wo.getWorkorderNumber()));
        dateRefTXT.setText(date);

        // payment
        paymentMethodTXT.setText(method);

        // line items
        labourTXT.setText(String.format("CDN $%.2f", labour));
        partsTXT.setText(String.format("CDN $%.2f", parts));
        pstTXT.setText(String.format("CDN $%.2f", pst));
        gstTXT.setText(String.format("CDN $%.2f", gst));
        subtotalTXT.setText(String.format("CDN $%.2f", subtotal));
        depositDeductTXT.setText(deposit > 0 ? String.format("- CDN $%.2f", deposit) : "—");
        totalDueTXT.setText(String.format("CDN $%.2f", totalDue));
    }
}