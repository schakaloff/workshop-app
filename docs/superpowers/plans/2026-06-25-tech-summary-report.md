# Technician Summary Report Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a "Print Summary" button to the Tech Stats section of Settings that prints/exports a per-technician work order summary report with multi-page support.

**Architecture:** `PrintTechSummaryController` builds all pages as programmatic `AnchorPane`s (no FXML). `DocumentOutput` gets a new `printPages()` overload for N-page lists. `SettingsController` wires the button to build pages and call `DocumentOutput`.

**Tech Stack:** JavaFX, existing `DocumentOutput` + `WorkshopQueries`, MaterialFX (`MFXButton`)

## Global Constraints

- Paper: A4 portrait, `Printer.MarginType.HARDWARE_MINIMUM` — same as all existing print code
- Page dimensions: `PAGE_W = 595.0`, `PAGE_H = 842.0`
- Shop name hardcoded: `"TELTRONICS SERVICE CENTRE"`
- Only show WOs with status `"Repair Complete"` or `"Billing Complete"`
- Use existing `WorkshopQueries.loadTechWorkByDateRange()` — do NOT write new SQL
- Follow existing `PrintRepairController` patterns for text nodes and layout

---

## File Map

| File | Action | Responsibility |
|------|--------|----------------|
| `src/main/java/Controllers/PrintTechSummaryController.java` | **Create** | Builds all report pages as `List<AnchorPane>` |
| `src/main/java/utils/DocumentOutput.java` | **Modify** | Add `printPages(String title, List<AnchorPane> pages, Window owner)` |
| `src/main/resources/main/settings.fxml` | **Modify** | Add "Print Summary" `MFXButton` in tech stats HBox |
| `src/main/java/Controllers/SettingsController.java` | **Modify** | Add `printTechSummary()` `@FXML` handler |

---

## Task 1: `PrintTechSummaryController` — page builder

**Files:**
- Create: `src/main/java/Controllers/PrintTechSummaryController.java`

**Interfaces:**
- Consumes: `Skeletons.TechWorkRow` (fields: `getWorkOrderNumber()`, `getLabourAmount()`, `getFinishedDate()`)
- Produces: `public List<AnchorPane> buildPages(String techName, LocalDate from, LocalDate to, List<TechWorkRow> rows)`

- [ ] **Step 1: Create the file**

Create `src/main/java/Controllers/PrintTechSummaryController.java` with this full content:

```java
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

        // Always at least one page (even if empty)
        if (pages.isEmpty()) {
            pages.add(buildPage1(techName, from, to, new ArrayList<>(), true, 0.0));
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

        String total = "TOTAL LABOUR:   " + String.format(CURRENCY_FMT, totalLabour);
        page.getChildren().add(placed(boldText(total, 11), COL_DATE, y + 12));
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
```

- [ ] **Step 2: Verify it compiles**

```bash
cd /home/apelsinchik/antigravity/workshop-app && ./mvnw compile -q 2>&1 | tail -20
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/Controllers/PrintTechSummaryController.java
git commit -m "feat: add PrintTechSummaryController for tech summary report pages"
```

---

## Task 2: Extend `DocumentOutput` with N-page print/PDF

**Files:**
- Modify: `src/main/java/utils/DocumentOutput.java`

**Interfaces:**
- Consumes: `List<AnchorPane> pages` from `PrintTechSummaryController.buildPages()`
- Produces: `public static void printPages(String title, List<AnchorPane> pages, Window owner)`

- [ ] **Step 1: Add `printPages` method to `DocumentOutput`**

Open `src/main/java/utils/DocumentOutput.java`. After the existing `generatePdfBytes` method (around line 100), add:

```java
    public static void printPages(String title, List<javafx.scene.layout.AnchorPane> pages, Window owner) throws Exception {
        OutputChoice choice = showChoiceDialog(owner);
        if (choice == OutputChoice.CANCEL) return;

        // Render each page in a hidden stage so CSS/layout is applied
        List<Stage> hiddenStages = new ArrayList<>();
        for (javafx.scene.layout.AnchorPane page : pages) {
            hiddenStages.add(showHiddenStage(page));
        }

        Platform.runLater(() -> {
            try {
                if (choice == OutputChoice.PRINTER) {
                    printAnchorPages(owner, pages);
                } else if (choice == OutputChoice.PDF) {
                    exportAnchorPagesToPdf(title, pages, owner);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                hiddenStages.forEach(Stage::close);
            }
        });
    }

    private static void printAnchorPages(Window owner, List<javafx.scene.layout.AnchorPane> pages) {
        Printer printer = Printer.getDefaultPrinter();
        if (printer == null) {
            new Alert(Alert.AlertType.ERROR, "No default printer found.").showAndWait();
            return;
        }
        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null || !job.showPrintDialog(owner)) return;

        PageLayout layout = printer.createPageLayout(
                Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.HARDWARE_MINIMUM);

        for (javafx.scene.layout.AnchorPane page : pages) {
            printScaled(job, layout, page);
        }
        job.endJob();
    }

    private static void exportAnchorPagesToPdf(String title,
                                                List<javafx.scene.layout.AnchorPane> pages,
                                                Window owner) throws Exception {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        String safeName = (title == null ? "summary" : title)
                .replaceAll("[^a-zA-Z0-9-_ ]", "").trim();
        if (safeName.isBlank()) safeName = "summary";
        fc.setInitialFileName(safeName + ".pdf");

        File file = fc.showSaveDialog(owner);
        if (file == null) return;

        try (PDDocument doc = new PDDocument()) {
            for (javafx.scene.layout.AnchorPane page : pages) {
                appendPageToPdf(doc, page);
            }
            doc.save(file);
        }
    }
```

Also add `import java.util.List;` at the top if not already present (it should already be there from existing code).

- [ ] **Step 2: Verify it compiles**

```bash
cd /home/apelsinchik/antigravity/workshop-app && ./mvnw compile -q 2>&1 | tail -20
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/utils/DocumentOutput.java
git commit -m "feat: add printPages N-page overload to DocumentOutput"
```

---

## Task 3: Add "Print Summary" button to `settings.fxml`

**Files:**
- Modify: `src/main/resources/main/settings.fxml`

**Interfaces:**
- Produces: `onAction="#printTechSummary"` button wired to `SettingsController`

- [ ] **Step 1: Add the button**

In `src/main/resources/main/settings.fxml`, find the tech stats HBox (around line 199) that ends with the "Calculate" `MFXButton`:

```xml
                                                <MFXButton onAction="#calculate"
                                                           prefHeight="36" prefWidth="100"
                                                           text="Calculate"
                                                           style="-fx-background-color: #0097A7; -fx-text-fill: white; -fx-background-radius: 6;" />
```

Add a new button immediately after it (before `</children>`):

```xml
                                                <MFXButton onAction="#printTechSummary"
                                                           prefHeight="36" prefWidth="130"
                                                           text="Print Summary"
                                                           style="-fx-background-color: #37474F; -fx-text-fill: white; -fx-background-radius: 6;" />
```

- [ ] **Step 2: Verify app still starts (compile check)**

```bash
cd /home/apelsinchik/antigravity/workshop-app && ./mvnw compile -q 2>&1 | tail -20
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/main/settings.fxml
git commit -m "feat: add Print Summary button to tech stats in settings"
```

---

## Task 4: Wire `printTechSummary()` in `SettingsController`

**Files:**
- Modify: `src/main/java/Controllers/SettingsController.java`

**Interfaces:**
- Consumes: `PrintTechSummaryController.buildPages(String, LocalDate, LocalDate, List<TechWorkRow>)`
- Consumes: `DocumentOutput.printPages(String, List<AnchorPane>, Window)`
- Consumes: existing `techCombo`, `fromDP`, `toDP` fields already in controller

- [ ] **Step 1: Add imports to `SettingsController`**

At the top of `src/main/java/Controllers/SettingsController.java`, add these imports (after existing imports):

```java
import Controllers.PrintTechSummaryController;
import utils.DocumentOutput;
import javafx.scene.layout.AnchorPane;
import java.util.List;
```

- [ ] **Step 2: Add the `printTechSummary` method**

In `src/main/java/Controllers/SettingsController.java`, add this method in the `// ─── TECH STATS ──────────────────────────────────────────────────────────────` section, after the `calculate()` method:

```java
    @FXML
    public void printTechSummary() {
        String techName  = techCombo.getValue();
        LocalDate fromDate = fromDP.getValue();
        LocalDate toDate   = toDP.getValue();

        if (techName == null || techName.isBlank() || fromDate == null || toDate == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Select a technician and date range first.", ButtonType.OK).showAndWait();
            return;
        }
        if (fromDate.isAfter(toDate)) {
            new Alert(Alert.AlertType.WARNING,
                    "\"From\" date must be before \"To\" date.", ButtonType.OK).showAndWait();
            return;
        }

        try {
            WorkshopQueries workshopQueries = new WorkshopQueries();
            List<TechWorkRow> rows = workshopQueries.loadTechWorkByDateRange(techName, fromDate, toDate);

            PrintTechSummaryController builder = new PrintTechSummaryController();
            List<AnchorPane> pages = builder.buildPages(techName, fromDate, toDate, rows);

            String title = "Tech Summary - " + techName;
            DocumentOutput.printPages(title, pages,
                    dialogInstance.getScene().getWindow());
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "Failed to generate report: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }
```

- [ ] **Step 3: Verify it compiles**

```bash
cd /home/apelsinchik/antigravity/workshop-app && ./mvnw compile -q 2>&1 | tail -20
```

Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/Controllers/SettingsController.java
git commit -m "feat: wire printTechSummary handler in SettingsController"
```

---

## Task 5: Smoke test end-to-end

- [ ] **Step 1: Build the app**

```bash
cd /home/apelsinchik/antigravity/workshop-app && ./mvnw package -q -DskipTests 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`

- [ ] **Step 2: Manual test checklist**

Launch the app and verify:

1. Open Settings → Tech Stats tab
2. "Print Summary" button is visible next to "Calculate"
3. Click "Print Summary" with no tech/dates selected → warning dialog appears
4. Select a tech, set from/to dates, click "Print Summary"
5. Print/PDF choice dialog appears
6. Choose PDF → file save dialog appears → PDF saves successfully
7. Open PDF → verify header shows shop name, report title, tech name, date range
8. Verify table rows show WO#, completed date, labour amount
9. Verify total labour at bottom matches sum of all rows
10. If >35 rows: verify second page has "(continued)" header and totals only on last page

- [ ] **Step 3: Commit if any fixes needed**

```bash
git add -p
git commit -m "fix: <describe what you fixed>"
```
