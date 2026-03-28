package Controllers;

import Skeletons.Customer;
import Skeletons.PartTable;
import Skeletons.WorkTable;
import Skeletons.WorkOrder;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PrintRepairController {

    // ─── Customer box ────────────────────────────────────────────────────────────
    @FXML private Text cxNameTXT;
    @FXML private Text adressTXT;
    @FXML private Text custIDTxt;
    @FXML private Text phoneTXT;
    @FXML private Text cityProvinceCodeTXT;

    // ─── WO number ───────────────────────────────────────────────────────────────
    @FXML private Text woTXT;

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

    // ─── Dynamic table containers ────────────────────────────────────────────────
    @FXML private VBox labourContainer;   // labour rows injected here
    @FXML private VBox partsSection;      // parts header + rows

    // ─── Totals box ──────────────────────────────────────────────────────────────
    @FXML private Text totalLabourTXT;
    @FXML private Text totalPartsTXT;
    @FXML private Text totalTaxesTXT;
    @FXML private Text totalTXT;

    // ─── Constants ───────────────────────────────────────────────────────────────
    private static final double TAX_RATE     = 0.12;
    private static final String CURRENCY_FMT = "$%.2f";
    private static final double MAX_TABLE_H  = 280.0; // px available before totals box

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // ─── Column widths (must sum to ~590) ────────────────────────────────────────
    // Labour:  Date | Tech | Description | Price
    private static final double[] LABOUR_COLS = { 80, 90, 350, 70 };
    // Parts:   Name | Qty  | Unit Price  | Total
    private static final double[] PARTS_COLS  = { 200, 55, 100, 70, 70 };

    // ─── Entry point ─────────────────────────────────────────────────────────────

    public void initData(WorkOrder wo,
                         Customer co,
                         ObservableList<WorkTable> repairData,
                         ObservableList<PartTable>  partsData) {

        populateCustomer(co);
        populateDates(wo);
        populateWarranty(wo);
        populateDevice(wo);
        woTXT.setText(String.valueOf(wo.getWorkorderNumber()));

        buildLabourGrid(repairData);
        buildPartsGrid(partsData);
        populateTotals(repairData, partsData);
        applyScaleIfNeeded();
    }

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
        dateCompletedTXT.setText(LocalDate.now().format(DATE_FMT));
    }

    private void populateWarranty(WorkOrder wo) {
        boolean hasWarranty = wo.getWarrantyNumber() != null
                && !wo.getWarrantyNumber().isBlank();
        yesOrNoTXT.setText(hasWarranty ? "Yes" : "No");
        vendorTXT.setText(nullSafe(wo.getVendorId()));
        warrantyNumTXT.setText(nullSafe(wo.getWarrantyNumber()));
        POTXT.setText("");
    }

    private void populateDevice(WorkOrder wo) {
        unitDescTXT.setText(wo.getType() + "  " + wo.getModel());
        serialNumberTXT.setText(nullSafe(wo.getSerialNumber()));
        problemTXTArea.setText(nullSafe(wo.getProblemDesc()));
    }

    private void populateTotals(ObservableList<WorkTable> repairData,
                                ObservableList<PartTable>  partsData) {
        double labour = repairData.stream().mapToDouble(WorkTable::getPrice).sum();
        double parts  = partsData.stream().mapToDouble(PartTable::getTotalPrice).sum();
        double taxes  = (labour + parts) * TAX_RATE;
        double total  = labour + parts + taxes;

        totalLabourTXT.setText(String.format(CURRENCY_FMT, labour));
        totalPartsTXT.setText(String.format(CURRENCY_FMT, parts));
        totalTaxesTXT.setText(String.format(CURRENCY_FMT, taxes));
        totalTXT.setText(String.format(CURRENCY_FMT, total));
    }

    // ─── Labour GridPane ─────────────────────────────────────────────────────────

    private void buildLabourGrid(ObservableList<WorkTable> repairData) {
        GridPane grid = makeGrid(LABOUR_COLS);

        int rowIdx = 0;
        for (WorkTable row : repairData) {
            String date  = row.getDate() != null ? row.getDate().toString() : "";
            String tech  = nullSafe(row.getTech());
            String desc  = nullSafe(row.getDescription());
            String price = String.format(CURRENCY_FMT, row.getPrice());

            addCell(grid, date,  rowIdx, 0, LABOUR_COLS[0] - 4, false);
            addCell(grid, tech,  rowIdx, 1, LABOUR_COLS[1] - 4, false);
            addCell(grid, desc,  rowIdx, 2, LABOUR_COLS[2] - 4, true);  // wraps
            addCell(grid, price, rowIdx, 3, LABOUR_COLS[3] - 4, false);

            rowIdx++;
        }

        labourContainer.getChildren().add(grid);
    }

    // ─── Parts GridPane ──────────────────────────────────────────────────────────

    private void buildPartsGrid(ObservableList<PartTable> partsData) {
        GridPane grid = makeGrid(PARTS_COLS);

        int rowIdx = 0;
        for (PartTable row : partsData) {
            String name      = nullSafe(row.getName());
            String qty       = String.valueOf(row.getQuantity());
            String unitPrice = String.format(CURRENCY_FMT, row.getPrice());
            String total     = String.format(CURRENCY_FMT, row.getTotalPrice());

            addCell(grid, name,      rowIdx, 0, PARTS_COLS[0] - 4, true);
            addCell(grid, qty,       rowIdx, 1, PARTS_COLS[1] - 4, false);
            addCell(grid, unitPrice, rowIdx, 2, PARTS_COLS[2] - 4, false);
            addCell(grid, total,     rowIdx, 3, PARTS_COLS[3] - 4, false);

            rowIdx++;
        }

        // Append data rows below the header AnchorPane already in the FXML
        partsSection.getChildren().add(grid);
    }

    // ─── Grid helpers ────────────────────────────────────────────────────────────

    private GridPane makeGrid(double[] colWidths) {
        GridPane grid = new GridPane();
        grid.setStyle("-fx-border-color: #cccccc; -fx-border-width: 0.5;");

        for (double w : colWidths) {
            ColumnConstraints cc = new ColumnConstraints(w);
            cc.setHgrow(Priority.NEVER);
            grid.getColumnConstraints().add(cc);
        }

        return grid;
    }

    /**
     * Adds a Text node into the GridPane cell.
     * @param wrap  when true the text wraps at the column width (long descriptions)
     */
    private void addCell(GridPane grid, String value,
                         int row, int col, double maxWidth, boolean wrap) {
        Text text = new Text(value);
        text.setFont(Font.font(10));

        if (wrap) {
            text.setWrappingWidth(maxWidth);
        }

        GridPane.setMargin(text, new Insets(2, 3, 2, 3));
        grid.add(text, col, row);
    }

    // ─── Scale fallback ──────────────────────────────────────────────────────────

    /**
     * If combined labour + parts area exceeds MAX_TABLE_H, scale both containers
     * down proportionally so everything fits on one printed page.
     */
    private void applyScaleIfNeeded() {
        labourContainer.layoutBoundsProperty().addListener((obs, oldB, newB) -> {
            double labourH = newB.getHeight();
            double partsH  = partsSection.getBoundsInLocal().getHeight();
            double combined = labourH + partsH;

            if (combined > MAX_TABLE_H) {
                double scale = MAX_TABLE_H / combined;
                labourContainer.setScaleX(scale);
                labourContainer.setScaleY(scale);
                partsSection.setScaleX(scale);
                partsSection.setScaleY(scale);
            }
        });
    }

    // ─── Utility ─────────────────────────────────────────────────────────────────

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}