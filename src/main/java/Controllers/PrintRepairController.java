package Controllers;

import Skeletons.Customer;
import Skeletons.PartTable;
import Skeletons.WorkTable;
import Skeletons.WorkOrder;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PrintRepairController {

    // ─── Customer ────────────────────────────────────────────────────────────────
    @FXML private Text cxNameTXT;
    @FXML private Text adressTXT;
    @FXML private Text custIDTxt;
    @FXML private Text phoneTXT;
    @FXML private Text cityProvinceCodeTXT;

    // ─── WO / dates ──────────────────────────────────────────────────────────────
    @FXML private Text woTXT;
    @FXML private Text dateReceivedTXF;
    @FXML private Text dateCompletedTXT;

    // ─── Warranty ────────────────────────────────────────────────────────────────
    @FXML private Text yesOrNoTXT;
    @FXML private Text vendorTXT;
    @FXML private Text warrantyNumTXT;
    @FXML private Text POTXT;

    // ─── Device ──────────────────────────────────────────────────────────────────
    @FXML private Text unitDescTXT;
    @FXML private Text serialNumberTXT;
    @FXML private Text problemTXTArea;

    // ─── Labour rows container ────────────────────────────────────────────────────
    @FXML private VBox labourVBox;

    // ─── Parts header nodes (moved down dynamically) ──────────────────────────────
    @FXML private Rectangle partsHeaderRect;
    @FXML private Text      partsLabelPart;
    @FXML private Text      partsLabelQty;
    @FXML private Text      partsLabelUnit;
    @FXML private Text      partsLabelTotal;

    // ─── Parts rows container ─────────────────────────────────────────────────────
    @FXML private VBox partsVBox;

    // ─── Totals ──────────────────────────────────────────────────────────────────
    @FXML private Text totalLabourTXT;
    @FXML private Text totalPartsTXT;
    @FXML private Text totalTaxesTXT;
    @FXML private Text totalTXT;

    // ─── Constants ───────────────────────────────────────────────────────────────
    private static final double TAX_RATE      = 0.12;
    private static final String CURRENCY_FMT  = "$%.2f";
    private static final double ROW_H         = 16.0;
    private static final double LABOUR_Y      = 446.0; // must match FXML labourVBox layoutY
    private static final double PARTS_GAP     = 8.0;
    private static final double HEADER_H      = 16.0;

    // Column X positions — must match FXML header Text layoutX
    private static final double L_TECH  = 2.0;
    private static final double L_DATE  = 90.0;
    private static final double L_DESC  = 175.0;
    private static final double L_PRICE = 543.0;

    private static final double P_NAME  = 2.0;
    private static final double P_QTY   = 354.0;
    private static final double P_UNIT  = 424.0;
    private static final double P_TOTAL = 524.0;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy");

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

        buildLabourRows(repairData);

        // Defer parts repositioning to after JavaFX has laid out labourVBox
        javafx.application.Platform.runLater(() -> {
            repositionParts(repairData.size());
            buildPartsRows(partsData);
        });
        populateTotals(repairData, partsData);
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

    // ─── Labour rows ─────────────────────────────────────────────────────────────

    private void buildLabourRows(ObservableList<WorkTable> repairData) {
        labourVBox.getChildren().clear();

        for (WorkTable row : repairData) {
            String tech  = nullSafe(row.getTech());
            String date  = row.getDate() != null ? row.getDate().toString() : "";
            String desc  = nullSafe(row.getDescription());
            String price = String.format(CURRENCY_FMT, row.getPrice());

            // Each row is an AnchorPane so Text nodes pin to exact X positions
            AnchorPane ap = new AnchorPane();
            ap.setPrefWidth(590);
            ap.setPrefHeight(AnchorPane.USE_COMPUTED_SIZE);
            ap.setMinHeight(ROW_H);
            ap.setStyle("-fx-border-color: #eeeeee; -fx-border-width: 0 0 0.5 0;");

            ap.getChildren().addAll(
                    pin(tech,  L_TECH),
                    pin(date,  L_DATE),
                    pinWrapped(desc, L_DESC, 355.0, 8.0),  // smaller font to fit more text
                    pin(price, L_PRICE)
            );
            labourVBox.getChildren().add(ap);
        }
    }

    // ─── Parts repositioning ─────────────────────────────────────────────────────

    /**
     * Moves all parts header nodes and partsVBox down based on the actual
     * rendered height of labourVBox — accounts for rows that wrap to multiple lines.
     */
    private void repositionParts(int labourRowCount) {
        // Force labourVBox to compute its layout so getHeight() is accurate
        labourVBox.applyCss();
        labourVBox.layout();

        double labourActualH = labourVBox.getBoundsInLocal().getHeight();
        // Fall back to row count estimate if layout hasn't run yet (height == 0)
        if (labourActualH <= 0) {
            labourActualH = labourRowCount * ROW_H;
        }

        double newHeaderY = LABOUR_Y + labourActualH + PARTS_GAP;
        double newVBoxY   = newHeaderY + HEADER_H;

        partsHeaderRect.setLayoutY(newHeaderY);
        partsLabelPart.setLayoutY(newHeaderY + 12);
        partsLabelQty.setLayoutY(newHeaderY + 12);
        partsLabelUnit.setLayoutY(newHeaderY + 12);
        partsLabelTotal.setLayoutY(newHeaderY + 12);
        partsVBox.setLayoutY(newVBoxY);
    }

    // ─── Parts rows ──────────────────────────────────────────────────────────────

    private void buildPartsRows(ObservableList<PartTable> partsData) {
        partsVBox.getChildren().clear();

        for (PartTable row : partsData) {
            String name  = nullSafe(row.getName());
            String qty   = String.valueOf(row.getQuantity());
            String unit  = String.format(CURRENCY_FMT, row.getPrice());
            String total = String.format(CURRENCY_FMT, row.getTotalPrice());

            AnchorPane ap = new AnchorPane();
            ap.setPrefWidth(590);
            ap.setPrefHeight(AnchorPane.USE_COMPUTED_SIZE);
            ap.setMinHeight(ROW_H);
            ap.setStyle("-fx-border-color: #eeeeee; -fx-border-width: 0 0 0.5 0;");

            ap.getChildren().addAll(
                    pinWrapped(name, P_NAME, 345.0),   // capped before qty column
                    pin(qty,   P_QTY),
                    pin(unit,  P_UNIT),
                    pin(total, P_TOTAL)
            );
            partsVBox.getChildren().add(ap);
        }
    }

    // ─── Totals ──────────────────────────────────────────────────────────────────

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

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    /** Creates a Text node pinned to a specific X position inside an AnchorPane. */
    private Text pin(String value, double x) {
        Text t = new Text(value);
        t.setFont(Font.font(11));
        AnchorPane.setLeftAnchor(t, x);
        AnchorPane.setTopAnchor(t, 3.0);
        return t;
    }

    /** Same as pin() but caps the text at maxWidth and allows a custom font size. */
    private Text pinWrapped(String value, double x, double maxWidth) {
        return pinWrapped(value, x, maxWidth, 11.0);
    }

    private Text pinWrapped(String value, double x, double maxWidth, double fontSize) {
        Text t = new Text(value);
        t.setFont(Font.font(fontSize));
        t.setWrappingWidth(maxWidth);
        AnchorPane.setLeftAnchor(t, x);
        AnchorPane.setTopAnchor(t, 3.0);
        return t;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}