package Controllers;

import Skeletons.Customer;
import Skeletons.PartTable;
import Skeletons.WorkTable;
import Skeletons.WorkOrder;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PrintRepairController {

    @FXML private Text woTXT;

    // ─── Customer box ────────────────────────────────────────────────────────────
    @FXML private Text cxNameTXT;
    @FXML private Text adressTXT;
    @FXML private Text custIDTxt;
    @FXML private Text phoneTXT;
    @FXML private Text cityProvinceCodeTXT;

    // ─── Dates ───────────────────────────────────────────────────────────────────
    @FXML private Text dateReceivedTXF;
    @FXML private Text dateCompletedTXT;

    // ─── Warranty row ────────────────────────────────────────────────────────────
    @FXML private Text yesOrNoTXT;
    @FXML private Text vendorTXT;
    @FXML private Text warrantyNumTXT;
    @FXML private Text POTXT;

    // ─── Device / problem box ────────────────────────────────────────────────────
    @FXML private Text     unitDescTXT;
    @FXML private Text     serialNumberTXT;
    @FXML private TextArea problemTXTArea;

    // ─── Totals box ──────────────────────────────────────────────────────────────
    @FXML private Text totalLabourTXT;
    @FXML private Text totalPartsTXT;
    @FXML private Text totalTaxesTXT;
    @FXML private Text totalTXT;

    // ─── Constants ───────────────────────────────────────────────────────────────
    private static final double TAX_RATE      = 0.12;   // BC GST+PST 12 %
    private static final String CURRENCY_FMT  = "$%.2f";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // ─── Entry point ─────────────────────────────────────────────────────────────

    /**
     * Called from ViewOrderController.printRepaired() after the FXML is loaded.
     *
     * @param wo         the current work order
     * @param co         the current customer
     * @param repairData labour rows from the repair table
     * @param partsData  parts rows from the parts table
     */
    public void initData(WorkOrder wo, Customer co, ObservableList<WorkTable> repairData, ObservableList<PartTable>  partsData) {
        populateCustomer(co);
        populateDates(wo);
        populateWarranty(wo);
        populateDevice(wo);
        populateTotals(repairData, partsData);
        woTXT.setText(String.valueOf(wo.getWorkorderNumber()));    }

    // ─── Populate helpers ────────────────────────────────────────────────────────

    private void populateCustomer(Customer co) {
        cxNameTXT.setText(co.getLastName() + ", " + co.getFirstName());
        adressTXT.setText(co.getAddress());
        custIDTxt.setText(co.getId());
        phoneTXT.setText(co.getPhone());
        cityProvinceCodeTXT.setText(co.getTown() + "  " + co.getPostalCode());
    }

    private void populateDates(WorkOrder wo) {
        dateReceivedTXF.setText(wo.getCreatedAt());
        // Use today as the completion date (work is repair-complete at print time)
        dateCompletedTXT.setText(LocalDate.now().format(DATE_FMT));
    }

    private void populateWarranty(WorkOrder wo) {
        boolean hasWarranty = wo.getWarrantyNumber() != null
                && !wo.getWarrantyNumber().isBlank();

        yesOrNoTXT.setText(hasWarranty ? "Yes" : "No");
        vendorTXT.setText(nullSafe(wo.getVendorId()));
        warrantyNumTXT.setText(nullSafe(wo.getWarrantyNumber()));
        POTXT.setText("");   // PO# not currently stored on WorkOrder; leave blank
    }

    private void populateDevice(WorkOrder wo) {
        unitDescTXT.setText(wo.getType() + "  " + wo.getModel());
        serialNumberTXT.setText(nullSafe(wo.getSerialNumber()));
        problemTXTArea.setText(nullSafe(wo.getProblemDesc()));
    }

    private void populateTotals(ObservableList<WorkTable> repairData,
                                ObservableList<PartTable>  partsData) {

        double labour = repairData.stream()
                .mapToDouble(WorkTable::getPrice)
                .sum();

        double parts = partsData.stream()
                .mapToDouble(PartTable::getTotalPrice)
                .sum();

        double taxes = (labour + parts) * TAX_RATE;
        double total = labour + parts + taxes;

        totalLabourTXT.setText(String.format(CURRENCY_FMT, labour));
        totalPartsTXT.setText(String.format(CURRENCY_FMT, parts));
        totalTaxesTXT.setText(String.format(CURRENCY_FMT, taxes));
        totalTXT.setText(String.format(CURRENCY_FMT, total));
    }

    // ─── Utility ─────────────────────────────────────────────────────────────────

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}