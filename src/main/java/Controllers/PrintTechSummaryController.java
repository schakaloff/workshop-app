package Controllers;

import Skeletons.TechWorkRow;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PrintTechSummaryController {

    private static final double PAGE_W      = 595.0;
    private static final double PAGE_H      = 842.0;
    private static final double PAGE_CUTOFF = 720.0;
    private static final double ROW_H       = 18.0;
    private static final double HEADER_H    = 16.0;
    private static final double MARGIN      = 20.0;

    // Column X positions
    private static final double COL_WO     = MARGIN;
    private static final double COL_DATE   = 100.0;
    private static final double COL_LABOUR = 450.0;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final String CURRENCY_FMT = "$%.2f";

    public List<AnchorPane> buildPages(String techName,
                                       LocalDate from,
                                       LocalDate to,
                                       List<TechWorkRow> rows) {
        List<AnchorPane> pages = new ArrayList<>();

        // Filter to completed only
        List<TechWorkRow> completed = new ArrayList<>();
        for (TechWorkRow r : rows) {
            String s = r.getStatus();
            if (s != null && (s.equalsIgnoreCase("Repair Complete")
                    || s.equalsIgnoreCase("Billing Complete"))) {
                completed.add(r);
            }
        }

        double totalLabour = completed.stream().mapToDouble(TechWorkRow::getLabourAmount).sum();

        // Partition rows into pages
        List<List<TechWorkRow>> partitions = partitionRows(completed);

        for (int i = 0; i < partitions.size(); i++) {
            boolean isLast = (i == partitions.size() - 1);
            List<TechWorkRow> pageRows = partitions.get(i);

            if (i == 0) {
                pages.add(buildPage1(techName, from, to, pageRows, isLast, totalLabour));
            } else {
                pages.add(buildPageN(pageRows, isLast, totalLabour, i + 1));
            }
        }

        return pages;
    }

    // ─── Partition ───────────────────────────────────────────────────────────────

    private List<List<TechWorkRow>> partitionRows(List<TechWorkRow> rows) {
        List<List<TechWorkRow>> partitions = new ArrayList<>();
        if (rows.isEmpty()) {
            partitions.add(new ArrayList<>());
            return partitions;
        }

        // Page 1: rows start at Y=120 (after header block + column headers)
        // Page N: rows start at Y=60 (after "continued" header + column headers)
        double page1Start  = 120.0;
        double pageNStart  = 60.0;

        List<TechWorkRow> current = new ArrayList<>();
        double y = page1Start;

        for (TechWorkRow r : rows) {
            if (y + ROW_H > PAGE_CUTOFF) {
                partitions.add(current);
                current = new ArrayList<>();
                y = pageNStart;
            }
            current.add(r);
            y += ROW_H;
        }
        partitions.add(current);
        return partitions;
    }

    // ─── Page 1 ──────────────────────────────────────────────────────────────────

    private AnchorPane buildPage1(String techName,
                                  LocalDate from, LocalDate to,
                                  List<TechWorkRow> pageRows,
                                  boolean showTotals,
                                  double totalLabour) {
        AnchorPane page = blankPage();

        double y = MARGIN;

        // Shop name
        page.getChildren().add(placed(boldText("TELTRONICS SERVICE CENTRE", 14), MARGIN, y + 12));
        y += 18;

        // Report title
        page.getChildren().add(placed(boldText("Technician Time Evaluation Summary Report", 11), MARGIN, y + 11));
        y += 16;

        // Tech + date range
        String fromStr = from != null ? from.format(DATE_FMT) : "";
        String toStr   = to   != null ? to.format(DATE_FMT)   : "";
        String info = "Tech: " + techName + "    From: " + fromStr + "    To: " + toStr;
        page.getChildren().add(placed(normalText(info, 10), MARGIN, y + 11));
        y += 16;

        // Separator
        Line sep = new Line(MARGIN, y, PAGE_W - MARGIN, y);
        sep.setStroke(Color.DARKGRAY);
        page.getChildren().add(sep);
        y += 6;

        // Column headers
        y = addColumnHeaders(page, y);

        // Data rows
        for (TechWorkRow r : pageRows) {
            y = addDataRow(page, y, r);
        }

        if (showTotals) {
            addTotals(page, y, totalLabour);
        }

        return page;
    }

    // ─── Page N ──────────────────────────────────────────────────────────────────

    private AnchorPane buildPageN(List<TechWorkRow> pageRows,
                                  boolean showTotals,
                                  double totalLabour,
                                  int pageNum) {
        AnchorPane page = blankPage();

        double y = MARGIN;

        // Continued header
        page.getChildren().add(placed(
                boldText("Technician Time Evaluation Summary Report (continued) — page " + pageNum, 10),
                MARGIN, y + 11));
        y += 18;

        // Column headers
        y = addColumnHeaders(page, y);

        // Data rows
        for (TechWorkRow r : pageRows) {
            y = addDataRow(page, y, r);
        }

        if (showTotals) {
            addTotals(page, y, totalLabour);
        }

        return page;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private AnchorPane blankPage() {
        AnchorPane p = new AnchorPane();
        p.setPrefWidth(PAGE_W);
        p.setPrefHeight(PAGE_H);
        p.setStyle("-fx-background-color: white;");
        return p;
    }

    private double addColumnHeaders(AnchorPane page, double y) {
        Rectangle hdr = new Rectangle(MARGIN, y, PAGE_W - MARGIN * 2, HEADER_H);
        hdr.setFill(Color.LIGHTGRAY);
        hdr.setStroke(Color.BLACK);
        hdr.setStrokeWidth(0.5);
        page.getChildren().add(hdr);

        page.getChildren().addAll(
                placed(boldText("WO#",            9), COL_WO,     y + 12),
                placed(boldText("Completed Date", 9), COL_DATE,   y + 12),
                placed(boldText("Labour",         9), COL_LABOUR, y + 12)
        );

        return y + HEADER_H;
    }

    private double addDataRow(AnchorPane page, double y, TechWorkRow r) {
        Rectangle bg = new Rectangle(MARGIN, y, PAGE_W - MARGIN * 2, ROW_H);
        bg.setFill(Color.WHITE);
        bg.setStroke(Color.web("#eeeeee"));
        bg.setStrokeWidth(0.5);
        page.getChildren().add(bg);

        String wo     = String.valueOf(r.getWorkOrderNumber());
        String date   = r.getFinishedDate() != null ? r.getFinishedDate() : "";
        String labour = String.format(CURRENCY_FMT, r.getLabourAmount());

        page.getChildren().addAll(
                placed(normalText(wo,     9), COL_WO,     y + 12),
                placed(normalText(date,   9), COL_DATE,   y + 12),
                placed(normalText(labour, 9), COL_LABOUR, y + 12)
        );

        return y + ROW_H;
    }

    private void addTotals(AnchorPane page, double y, double totalLabour) {
        y += 8;

        Line sep = new Line(MARGIN, y, PAGE_W - MARGIN, y);
        sep.setStroke(Color.DARKGRAY);
        page.getChildren().add(sep);
        y += 6;

        page.getChildren().add(placed(boldText("TOTAL LABOUR:", 11), COL_DATE, y + 12));
        page.getChildren().add(placed(boldText(String.format(CURRENCY_FMT, totalLabour), 11), COL_LABOUR, y + 12));
    }

    private Text placed(Text t, double x, double y) {
        t.setLayoutX(x);
        t.setLayoutY(y);
        return t;
    }

    private Text boldText(String value, double size) {
        Text t = new Text(value);
        t.setFont(Font.font("System", FontWeight.BOLD, size));
        return t;
    }

    private Text normalText(String value, double size) {
        Text t = new Text(value);
        t.setFont(Font.font(size));
        return t;
    }
}
