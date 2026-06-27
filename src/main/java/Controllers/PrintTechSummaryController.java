package Controllers;

import Skeletons.TechWorkRow;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PrintTechSummaryController {

    @FXML private Text techInfoTXT;
    @FXML private VBox rowsVBox;

    // VBox starts at Y=84, page cutoff ~Y=792 → available height=708px
    // Reserve 30px for totals (sep 10px + total row 20px), leaving 678px for rows
    // 678 / 18 = 37 rows max
    private static final double ROW_H           = 18.0;
    private static final int    MAX_ROWS         = 37;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final String            CURRENCY  = "$%.2f";

    // Overflow state — set by initData, consumed by buildAdditionalPages
    private final List<List<TechWorkRow>> overflowBatches = new ArrayList<>();
    private double    totalLabour;
    private String    techName;
    private LocalDate from;
    private LocalDate to;

    // ─── Page 1 ──────────────────────────────────────────────────────────────────

    public void initData(String techName, LocalDate from, LocalDate to, List<TechWorkRow> rows) {
        this.techName = techName;
        this.from     = from;
        this.to       = to;

        techInfoTXT.setText(infoLine(null));

        List<TechWorkRow> completed = filterCompleted(rows);
        totalLabour = completed.stream().mapToDouble(TechWorkRow::getLabourAmount).sum();

        boolean hasOverflow = completed.size() > MAX_ROWS;
        int     page1Count  = hasOverflow ? MAX_ROWS : completed.size();

        for (int i = 0; i < page1Count; i++) {
            rowsVBox.getChildren().add(buildRow(completed.get(i)));
        }

        if (hasOverflow) {
            partitionIntoBatches(new ArrayList<>(completed.subList(page1Count, completed.size())));
        } else {
            addTotals();
        }
    }

    // ─── Continuation page init (called on a fresh FXML instance) ────────────────

    public void initContinuation(List<TechWorkRow> rows, int pageNum,
                                 boolean isLast, double total) {
        totalLabour = total;
        techInfoTXT.setText(infoLine(pageNum));

        int count = isLast ? Math.min(MAX_ROWS, rows.size()) : Math.min(MAX_ROWS, rows.size());
        for (int i = 0; i < count; i++) {
            rowsVBox.getChildren().add(buildRow(rows.get(i)));
        }
        if (isLast) addTotals();
    }

    // ─── Build overflow FXML pages ────────────────────────────────────────────────

    public List<Parent> buildAdditionalPages() throws Exception {
        List<Parent> result = new ArrayList<>();
        for (int i = 0; i < overflowBatches.size(); i++) {
            boolean isLast = (i == overflowBatches.size() - 1);
            FXMLLoader loader = new FXMLLoader(
                    PrintTechSummaryController.class.getResource("/main/techSummary.fxml"));
            Parent p = loader.load();
            PrintTechSummaryController ctrl = loader.getController();
            ctrl.techName = this.techName;
            ctrl.from     = this.from;
            ctrl.to       = this.to;
            ctrl.initContinuation(overflowBatches.get(i), i + 2, isLast, totalLabour);
            result.add(p);
        }
        return result;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private String infoLine(Integer pageNum) {
        String fromStr = from != null ? from.format(DATE_FMT) : "";
        String toStr   = to   != null ? to.format(DATE_FMT)   : "";
        String base = "Tech: " + techName + "    From: " + fromStr + "    To: " + toStr;
        return pageNum != null ? base + "    (page " + pageNum + ")" : base;
    }

    private List<TechWorkRow> filterCompleted(List<TechWorkRow> rows) {
        List<TechWorkRow> result = new ArrayList<>();
        for (TechWorkRow r : rows) {
            String s = r.getStatus();
            if (s != null && (s.equalsIgnoreCase("Repair Complete")
                    || s.equalsIgnoreCase("Billing Complete"))) {
                result.add(r);
            }
        }
        return result;
    }

    private void partitionIntoBatches(List<TechWorkRow> rows) {
        for (int i = 0; i < rows.size(); i += MAX_ROWS) {
            overflowBatches.add(new ArrayList<>(
                    rows.subList(i, Math.min(i + MAX_ROWS, rows.size()))));
        }
    }

    private AnchorPane buildRow(TechWorkRow r) {
        AnchorPane row = new AnchorPane();
        row.setPrefWidth(555);
        row.setPrefHeight(ROW_H);
        row.setStyle("-fx-border-color: #eeeeee; -fx-border-width: 0 0 0.5 0;");

        String wo     = String.valueOf(r.getWorkOrderNumber());
        String date   = r.getFinishedDate() != null ? r.getFinishedDate() : "";
        String labour = String.format(CURRENCY, r.getLabourAmount());

        row.getChildren().addAll(
                pinText(wo,     0.0,   9, false),
                pinText(date,   100.0, 9, false),
                pinText(labour, 430.0, 9, false)
        );
        return row;
    }

    private void addTotals() {
        AnchorPane sepRow = new AnchorPane();
        sepRow.setPrefHeight(10);
        sepRow.setPrefWidth(555);
        Rectangle sep = new Rectangle(0, 5, 555, 1);
        sep.setFill(Color.DARKGRAY);
        sepRow.getChildren().add(sep);
        rowsVBox.getChildren().add(sepRow);

        AnchorPane totalRow = new AnchorPane();
        totalRow.setPrefWidth(555);
        totalRow.setPrefHeight(20);
        totalRow.getChildren().addAll(
                pinText("TOTAL LABOUR:", 200.0, 11, true),
                pinText(String.format(CURRENCY, totalLabour), 430.0, 11, true)
        );
        rowsVBox.getChildren().add(totalRow);
    }

    private Text pinText(String value, double x, double size, boolean bold) {
        Text t = new Text(value);
        t.setFont(bold
                ? Font.font("System", FontWeight.BOLD, size)
                : Font.font(size));
        AnchorPane.setLeftAnchor(t, x);
        AnchorPane.setTopAnchor(t, 3.0);
        return t;
    }
}
