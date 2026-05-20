package Controllers;

import Controllers.DbRepo.ViewControllerQueries;
import Skeletons.Customer;
import Skeletons.WorkOrder;
import javafx.fxml.FXML;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

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

    @FXML private Text labourSubTXT;
    @FXML private Text partsSubTXT;

    @FXML private Text billingNotesTXT;

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
        labourSubTXT.setText(String.format("CDN $%.2f", labour));
        partsSubTXT.setText(String.format("CDN $%.2f", parts));

        // billing summary
        String vendorName = wo.getVendorId();
        boolean hasVendor = vendorName != null && !vendorName.isBlank();
        Skeletons.Vendor vendor = hasVendor
                ? Controllers.DbRepo.VendorsQueries.getVendorByName(vendorName)
                : null;
        String customerName = co.getFirstName() + " " + co.getLastName();
        StringBuilder sb = new StringBuilder();
        if (vendor != null) {
            double vL = vendor.isPaysLabour() ? labour : 0;
            double vP = vendor.isPaysParts()  ? parts  : 0;
            double vPst2 = vendor.isPaysPst() ? pst    : 0;
            double vG = vendor.isPaysGst()    ? gst    : 0;
            if (vL+vP+vPst2+vG > 0) {
                List<String> c = new ArrayList<>();
                if (vendor.isPaysLabour()) c.add("Labour");
                if (vendor.isPaysParts())  c.add("Parts");
                if (vendor.isPaysPst())    c.add("PST");
                if (vendor.isPaysGst())    c.add("GST");
                sb.append(vendorName).append(" (Warranty)  |  ")
                        .append(String.join("+", c)).append("  |  CDN $")
                        .append(String.format("%.2f", vL+vP+vPst2+vG)).append("\n");
            }
            double cL = vendor.isPaysLabour() ? 0 : labour;
            double cP = vendor.isPaysParts()  ? 0 : parts;
            double cPst2 = vendor.isPaysPst() ? 0 : pst;
            double cG = vendor.isPaysGst()    ? 0 : gst;
            if (cL+cP+cPst2+cG > 0) {
                List<String> c = new ArrayList<>();
                if (cL>0) c.add("Labour");
                if (cP>0) c.add("Parts");
                if (cPst2>0) c.add("PST");
                if (cG>0) c.add("GST");
                sb.append(customerName).append("  |  ")
                        .append(String.join("+", c)).append("  |  CDN $")
                        .append(String.format("%.2f", cL+cP+cPst2+cG)).append("\n");
            }
        } else {
            sb.append(customerName).append("  |  Labour+Parts+Tax  |  CDN $")
                    .append(String.format("%.2f", labour+parts+pst+gst)).append("\n");
        }
        billingNotesTXT.setText(sb.toString());
    }
}