package Controllers;

import DB.ShopSettings;
import Skeletons.Customer;
import Skeletons.PartTable;
import Skeletons.WorkTable;
import Skeletons.WorkOrder;
import javafx.application.Platform;
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
import java.util.Arrays;
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
    @FXML private Text      totalPSTTXT;     // new
    @FXML private Text      totalGSTTXT;     // new
    @FXML private Text      totalTXT;
    @FXML private Rectangle totalsRect;
    @FXML private VBox      totalsLabelVBox;
    @FXML private VBox      totalsValueVBox;
    @FXML private Rectangle footerRect;

    // ─── Pagination state ────────────────────────────────────────────────────────
    // Holds overflow parts rows that didn't fit on page 1
    private final List<AnchorPane> overflowRows = new ArrayList<>();
    // Tail of the problem/complaint text that didn't fit in its box on page 1
    private String problemOverflowText = "";

    private static final double PROBLEM_BOX_W = 265.0;
    private static final double PROBLEM_BOX_H = 118.0;
    private String  totalLabour;
    private String  totalParts;
    private String totalPST;
    private String totalGST;
    private String  totalTaxes;
    private String  totalTotal;

    // ─── Constants ───────────────────────────────────────────────────────────────
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

        boolean hasWarranty = wo.getVendorId() != null && !wo.getVendorId().isBlank();
        boolean hasPstNum   = co.getPstNumber() != null && !co.getPstNumber().isBlank();
        boolean hasGstNum   = co.getGstNumber() != null && !co.getGstNumber().isBlank();

        double labour       = repairData.stream().mapToDouble(WorkTable::getPrice).sum();
        double partsDisplay = partsData.stream().mapToDouble(PartTable::getTotalPrice).sum();

// warranty = parts not included in bill at all
        double taxBase = hasWarranty ? labour : labour + partsDisplay;
        double billParts = hasWarranty ? 0.0 : partsDisplay;

        double pst = ShopSettings.get().calcPst(taxBase, hasPstNum);
        double gst = ShopSettings.get().calcGst(taxBase, hasGstNum);

        double total = labour + billParts + pst + gst;

        totalLabour = String.format(CURRENCY_FMT, labour);
        totalParts  = String.format(CURRENCY_FMT, billParts); // shows $0.00 if warranty
        totalPST    = String.format(CURRENCY_FMT, pst);
        totalGST    = String.format(CURRENCY_FMT, gst);
        totalTotal  = String.format(CURRENCY_FMT, total);

        buildLabourRows(repairData);

        buildBillingSummary(wo, co, labour, billParts, pst, gst);


        Platform.runLater(() -> {
            double partsHeaderY = repositionParts(repairData.size());
            buildPartsRowsWithOverflow(partsData, partsHeaderY);
            populateTotals();
        });
    }

    public AnchorPane buildPage2() {
        if (overflowRows.isEmpty() && problemOverflowText.isEmpty()) return null;

        AnchorPane page = new AnchorPane();
        page.setPrefWidth(PAGE_W);
        page.setPrefHeight(PAGE_H);
        page.setStyle("-fx-background-color: white;");

        double y = 20.0;

        // Problem/Complaint continuation, if it overflowed off page 1
        if (!problemOverflowText.isEmpty()) {
            Rectangle problemHdrRect = new Rectangle(4, y, 590, HEADER_H);
            problemHdrRect.setFill(Color.LIGHTGRAY);
            problemHdrRect.setStroke(Color.BLACK);
            problemHdrRect.setStrokeWidth(0.5);
            page.getChildren().add(problemHdrRect);
            page.getChildren().add(staticText("Problem/Complaint (continued)", 6, y + 12, true));
            y += HEADER_H + 10;

            Text continued = new Text(problemOverflowText);
            continued.setFont(Font.font(11));
            continued.setWrappingWidth(590);
            continued.setLayoutX(4);
            continued.setLayoutY(y);
            page.getChildren().add(continued);
            y += continued.getLayoutBounds().getHeight() + 20;
        }

        // Parts header, if any parts rows overflowed off page 1
        if (!overflowRows.isEmpty()) {
            Rectangle hdrRect = new Rectangle(4, y, 590, HEADER_H);
            hdrRect.setFill(Color.LIGHTGRAY);
            hdrRect.setStroke(Color.BLACK);
            hdrRect.setStrokeWidth(0.5);
            page.getChildren().add(hdrRect);

            page.getChildren().addAll(
                    staticText("Part (continued)", 6,   y + 12, true),
                    staticText("Qty",              354,  y + 12, true),
                    staticText("Unit Price",       424,  y + 12, true),
                    staticText("Total",            524,  y + 12, true)
            );

            y += HEADER_H;

            for (AnchorPane row : overflowRows) {
                row.setLayoutX(4);
                row.setLayoutY(y);
                page.getChildren().add(row);
                y += row.getPrefHeight() <= 0 ? ROW_H : row.getPrefHeight();
            }
        }

        // Totals box at bottom of page 2 — taller to fit 5 lines
        // (pushed down further if the content above ran long)
        double totalsY = Math.max(PAGE_H - 120, y + 20);

        Rectangle totalsRect = new Rectangle(465, totalsY, 129, 109);
        totalsRect.setFill(Color.WHITE);
        totalsRect.setStroke(Color.BLACK);
        totalsRect.setArcWidth(5);
        totalsRect.setArcHeight(5);
        page.getChildren().add(totalsRect);

        double lx = 471, rx = 532, ty = totalsY + 14;
        page.getChildren().addAll(
                staticText("Labour:", lx, ty,       false),
                staticText("Parts:",  lx, ty + 16,  false),
                staticText("PST:",    lx, ty + 32,  false),
                staticText("GST:",    lx, ty + 48,  false),
                staticText("TOTAL:",  lx, ty + 66,  false)
        );
        page.getChildren().addAll(
                staticText(totalLabour, rx, ty,       false),
                staticText(totalParts,  rx, ty + 16,  false),
                staticText(totalPST,    rx, ty + 32,  false),
                staticText(totalGST,    rx, ty + 48,  false),
                staticText(totalTotal,  rx, ty + 66,  false)
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

        String[] split = splitTextToFit(nullSafe(wo.getProblemDesc()), PROBLEM_BOX_W, PROBLEM_BOX_H, 11.0);
        problemTXTArea.setText(split[0]);
        problemOverflowText = split[1];
    }

    // Splits text at the last word boundary that still fits within maxHeight
    // when wrapped at maxWidth; returns {fits, remainder}.
    private String[] splitTextToFit(String full, double maxWidth, double maxHeight, double fontSize) {
        if (full.isEmpty()) return new String[] { "", "" };

        Text probe = new Text();
        probe.setFont(Font.font(fontSize));
        probe.setWrappingWidth(maxWidth);

        probe.setText(full);
        if (probe.getLayoutBounds().getHeight() <= maxHeight) {
            return new String[] { full, "" };
        }

        String[] words = full.split(" ");
        StringBuilder fitted = new StringBuilder();
        int fitWordCount = 0;

        for (int i = 0; i < words.length; i++) {
            String candidate = fitted.length() == 0 ? words[i] : fitted + " " + words[i];
            probe.setText(candidate);
            if (probe.getLayoutBounds().getHeight() > maxHeight) break;
            fitted = new StringBuilder(candidate);
            fitWordCount = i + 1;
        }

        String remainder = String.join(" ", Arrays.copyOfRange(words, fitWordCount, words.length));
        return new String[] { fitted.toString(), remainder };
    }

    private void populateTotals() {
        totalLabourTXT.setText(totalLabour);
        totalPartsTXT.setText(totalParts);
        totalPSTTXT.setText(totalPST);
        totalGSTTXT.setText(totalGST);
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
            totalPSTTXT.setVisible(false);   // new
            totalGSTTXT.setVisible(false);   // new
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

    private void buildBillingSummary(WorkOrder wo, Customer co,
                                     double labour, double parts,
                                     double pst, double gst) {
        String vendorName = wo.getVendorId();
        boolean hasVendor = vendorName != null && !vendorName.isBlank();
        Skeletons.Vendor vendor = hasVendor
                ? Controllers.DbRepo.VendorsQueries.getVendorByName(vendorName)
                : null;

        String customerName = co.getLastName() + ", " + co.getFirstName();

        // build summary lines
        List<String> lines = new ArrayList<>();
        if (vendor != null) {
            double vLabour = vendor.isPaysLabour() ? labour : 0;
            double vParts  = vendor.isPaysParts()  ? parts  : 0;
            double vPst    = vendor.isPaysPst()    ? pst    : 0;
            double vGst    = vendor.isPaysGst()    ? gst    : 0;
            double vTotal  = vLabour + vParts + vPst + vGst;

            if (vTotal > 0) {
                List<String> covers = new ArrayList<>();
                if (vendor.isPaysLabour()) covers.add("Labour");
                if (vendor.isPaysParts())  covers.add("Parts");
                if (vendor.isPaysPst())    covers.add("PST");
                if (vendor.isPaysGst())    covers.add("GST");
                lines.add(vendorName + " (Warranty)  |  " +
                        String.join("+", covers) + "  |  " +
                        String.format("$%.2f", vTotal));
            }

            double cLabour = vendor.isPaysLabour() ? 0 : labour;
            double cParts  = vendor.isPaysParts()  ? 0 : parts;
            double cPst    = vendor.isPaysPst()    ? 0 : pst;
            double cGst    = vendor.isPaysGst()    ? 0 : gst;
            double cTotal  = cLabour + cParts + cPst + cGst;

            if (cTotal > 0) {
                List<String> covers = new ArrayList<>();
                if (cLabour > 0) covers.add("Labour");
                if (cParts  > 0) covers.add("Parts");
                if (cPst    > 0) covers.add("PST");
                if (cGst    > 0) covers.add("GST");
                lines.add(customerName + "  |  " +
                        String.join("+", covers) + "  |  " +
                        String.format("$%.2f", cTotal));
            }
        } else {
            double total = labour + parts + pst + gst;
            lines.add(customerName + "  |  Labour+Parts+Tax  |  " +
                    String.format("$%.2f", total));
        }

        // render box in FXML AnchorPane — add nodes dynamically
        // billing header rect at Y=755 (below totals)
        double bY = 755.0;
        AnchorPane root = (AnchorPane) totalsRect.getParent();

        Rectangle hdr = new Rectangle(4, bY, 460, 14);
        hdr.setFill(Color.LIGHTGRAY);
        hdr.setStroke(Color.BLACK);
        hdr.setStrokeWidth(0.5);
        root.getChildren().add(hdr);

        Text hdrTxt = staticText("BILLING SUMMARY", 6, bY + 11, true);
        root.getChildren().add(hdrTxt);

        bY += 14;
        for (String line : lines) {
            Rectangle row = new Rectangle(4, bY, 460, 14);
            row.setFill(Color.WHITE);
            row.setStroke(Color.BLACK);
            row.setStrokeWidth(0.5);
            root.getChildren().add(row);
            root.getChildren().add(staticText(line, 6, bY + 11, false));
            bY += 14;
        }
    }
}