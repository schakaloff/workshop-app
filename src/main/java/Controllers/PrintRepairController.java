package Controllers;

import Skeletons.Customer;
import Skeletons.PartTable;
import Skeletons.WorkTable;
import Skeletons.WorkOrder;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
    @FXML private Text      totalLabourTXT;
    @FXML private Text      totalPartsTXT;
    @FXML private Text      totalTaxesTXT;
    @FXML private Text      totalTXT;
    @FXML private Rectangle totalsRect;
    @FXML private VBox      totalsLabelVBox;
    @FXML private VBox      totalsValueVBox;
    @FXML private Rectangle footerRect;

    // ─── Pagination state ────────────────────────────────────────────────────────
    // Holds overflow parts rows that didn't fit on page 1
    private final List<AnchorPane> overflowRows = new ArrayList<>();
    private String  totalLabour;
    private String  totalParts;
    private String  totalTaxes;
    private String  totalTotal;

    // ─── Constants ───────────────────────────────────────────────────────────────
    private static final double TAX_RATE      = 0.12;
    private static final String CURRENCY_FMT  = "$%.2f";
    private static final double ROW_H         = 16.0;
    private static final double LABOUR_Y      = 446.0;
    private static final double PARTS_GAP     = 8.0;
    private static final double HEADER_H      = 16.0;
    private static final double PAGE_CUTOFF   = 735.0; // parts rows below this go to page 2
    private static final double PAGE_H        = 842.0;
    private static final double PAGE_W        = 595.0;

    // Labour column X positions
    private static final double L_TECH  = 2.0;
    private static final double L_DATE  = 90.0;
    private static final double L_DESC  = 175.0;
    private static final double L_PRICE = 543.0;

    // Parts column X positions
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

        // Pre-compute totals strings so page 2 can reuse them
        double labour = repairData.stream().mapToDouble(WorkTable::getPrice).sum();
        double parts  = partsData.stream().mapToDouble(PartTable::getTotalPrice).sum();
        double taxes  = (labour + parts) * TAX_RATE;
        double total  = labour + parts + taxes;
        totalLabour = String.format(CURRENCY_FMT, labour);
        totalParts  = String.format(CURRENCY_FMT, parts);
        totalTaxes  = String.format(CURRENCY_FMT, taxes);
        totalTotal  = String.format(CURRENCY_FMT, total);

        buildLabourRows(repairData);

        // Defer parts positioning + overflow detection until after layout
        javafx.application.Platform.runLater(() -> {
            double partsHeaderY = repositionParts(repairData.size());
            buildPartsRowsWithOverflow(partsData, partsHeaderY);
            populateTotals();
        });
    }

    /**
     * Returns a fully built page 2 AnchorPane if there are overflow parts rows,
     * or null if everything fit on page 1.
     * Called by DocumentOutput AFTER the hidden stage has rendered page 1.
     */
    public AnchorPane buildPage2() {
        if (overflowRows.isEmpty()) return null;

        AnchorPane page = new AnchorPane();
        page.setPrefWidth(PAGE_W);
        page.setPrefHeight(PAGE_H);
        page.setStyle("-fx-background-color: white;");

        // Parts header at top of page 2
        double y = 20.0;

        Rectangle hdrRect = new Rectangle(4, y, 590, HEADER_H);
        hdrRect.setFill(Color.LIGHTGRAY);
        hdrRect.setStroke(Color.BLACK);
        hdrRect.setStrokeWidth(0.5);
        page.getChildren().add(hdrRect);

        page.getChildren().addAll(
                staticText("Part (continued)", 6,  y + 12, true),
                staticText("Qty",              354, y + 12, true),
                staticText("Unit Price",       424, y + 12, true),
                staticText("Total",            524, y + 12, true)
        );

        y += HEADER_H;

        // Add overflow rows
        for (AnchorPane row : overflowRows) {
            row.setLayoutX(4);
            row.setLayoutY(y);
            page.getChildren().add(row);
            y += row.getPrefHeight() <= 0 ? ROW_H : row.getPrefHeight();
        }

        // Totals box at bottom of page 2
        double totalsY = PAGE_H - 110;

        Rectangle totalsRect = new Rectangle(465, totalsY, 129, 93);
        totalsRect.setFill(Color.WHITE);
        totalsRect.setStroke(Color.BLACK);
        totalsRect.setArcWidth(5);
        totalsRect.setArcHeight(5);
        page.getChildren().add(totalsRect);

        double lx = 471, rx = 532, ty = totalsY + 16;
        page.getChildren().addAll(
                staticText("Labour:", lx, ty,      false),
                staticText("Parts:",  lx, ty + 16, false),
                staticText("Taxes:",  lx, ty + 32, false),
                staticText("TOTAL:",  lx, ty + 50, false)
        );
        page.getChildren().addAll(
                staticText(totalLabour, rx, ty,      false),
                staticText(totalParts,  rx, ty + 16, false),
                staticText(totalTaxes,  rx, ty + 32, false),
                staticText(totalTotal,  rx, ty + 50, false)
        );

        // Footer line
        Rectangle footer = new Rectangle(2, PAGE_H - 37, 200, 34);
        footer.setFill(Color.WHITE);
        footer.setStroke(Color.BLACK);
        page.getChildren().add(footer);

        return page;
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

    private void populateTotals() {
        totalLabourTXT.setText(totalLabour);
        totalPartsTXT.setText(totalParts);
        totalTaxesTXT.setText(totalTaxes);
        totalTXT.setText(totalTotal);
    }

    // ─── Labour rows ─────────────────────────────────────────────────────────────

    private void buildLabourRows(ObservableList<WorkTable> repairData) {
        labourVBox.getChildren().clear();

        for (WorkTable row : repairData) {
            String tech  = nullSafe(row.getTech());
            String date  = row.getDate() != null ? row.getDate().toString() : "";
            String desc  = nullSafe(row.getDescription());
            String price = String.format(CURRENCY_FMT, row.getPrice());

            AnchorPane ap = new AnchorPane();
            ap.setPrefWidth(590);
            ap.setPrefHeight(AnchorPane.USE_COMPUTED_SIZE);
            ap.setMinHeight(ROW_H);
            ap.setStyle("-fx-border-color: #eeeeee; -fx-border-width: 0 0 0.5 0;");

            ap.getChildren().addAll(
                    pin(tech,  L_TECH),
                    pin(date,  L_DATE),
                    pinWrapped(desc, L_DESC, 355.0, 8.0),
                    pin(price, L_PRICE)
            );
            labourVBox.getChildren().add(ap);
        }
    }

    // ─── Parts repositioning ─────────────────────────────────────────────────────

    private double repositionParts(int labourRowCount) {
        labourVBox.applyCss();
        labourVBox.layout();

        double labourActualH = labourVBox.getBoundsInLocal().getHeight();
        if (labourActualH <= 0) labourActualH = labourRowCount * ROW_H;

        double newHeaderY = LABOUR_Y + labourActualH + PARTS_GAP;
        double newVBoxY   = newHeaderY + HEADER_H;

        partsHeaderRect.setLayoutY(newHeaderY);
        partsLabelPart.setLayoutY(newHeaderY + 12);
        partsLabelQty.setLayoutY(newHeaderY + 12);
        partsLabelUnit.setLayoutY(newHeaderY + 12);
        partsLabelTotal.setLayoutY(newHeaderY + 12);
        partsVBox.setLayoutY(newVBoxY);

        return newVBoxY; // return Y where parts rows start
    }

    // ─── Parts rows with overflow detection ──────────────────────────────────────

    private void buildPartsRowsWithOverflow(ObservableList<PartTable> partsData,
                                            double partsStartY) {
        partsVBox.getChildren().clear();
        overflowRows.clear();

        double currentY = partsStartY;

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
                    pinWrapped(name, P_NAME, 345.0),
                    pin(qty,   P_QTY),
                    pin(unit,  P_UNIT),
                    pin(total, P_TOTAL)
            );

            if (currentY < PAGE_CUTOFF) {
                // Fits on page 1
                partsVBox.getChildren().add(ap);
                currentY += ROW_H;
            } else {
                // Overflows to page 2
                overflowRows.add(ap);
            }
        }

        // If there are overflow rows, hide the entire totals box and footer on page 1
        if (!overflowRows.isEmpty()) {
            totalLabourTXT.setVisible(false);
            totalPartsTXT.setVisible(false);
            totalTaxesTXT.setVisible(false);
            totalTXT.setVisible(false);
            totalsRect.setVisible(false);
            totalsLabelVBox.setVisible(false);
            totalsValueVBox.setVisible(false);
            footerRect.setVisible(false);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private Text pin(String value, double x) {
        Text t = new Text(value);
        t.setFont(Font.font(11));
        AnchorPane.setLeftAnchor(t, x);
        AnchorPane.setTopAnchor(t, 3.0);
        return t;
    }

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

    private Text staticText(String value, double x, double y, boolean bold) {
        Text t = new Text(value);
        t.setFont(bold ? Font.font("System", javafx.scene.text.FontWeight.BOLD, 11)
                : Font.font(11));
        t.setLayoutX(x);
        t.setLayoutY(y);
        return t;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}