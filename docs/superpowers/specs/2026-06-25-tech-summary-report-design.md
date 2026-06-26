# Technician Time Evaluation Summary Report

**Date:** 2026-06-25

## Overview

A printable per-technician summary report, triggered from the Tech Stats section of Settings. Shows all completed work orders in a date range with labour amounts and a total footer. Supports multi-page output.

## Data

- Source: `WorkshopQueries.loadTechWorkByDateRange(techName, from, to)`
- Returns: `List<TechWorkRow>` — each row has WO#, type, status, labour amount, finished date
- Filter: only rows where status is `Repair Complete` or `Billing Complete`
- Footer total: sum of `labourAmount` across all filtered rows

## Page Layout

### Page 1
```
TELTRONICS SERVICE CENTRE
Technician Time Evaluation Summary Report
Tech: [name]    From: [date]    To: [date]

─────────────────────────────────────────────
WO#     │  Completed Date   │  Labour Amount
─────────────────────────────────────────────
1042    │  Jun 10, 2026     │  $85.00
1043    │  Jun 12, 2026     │  $120.00
...
─────────────────────────────────────────────
TOTAL LABOUR:                   $205.00   ← last page only
```

### Page 2+
- Light "continued" header (no shop name repeated)
- Same table columns
- Totals box on last page only

## Components

### 1. `settings.fxml`
Add `MFXButton` with `onAction="#printTechSummary"` text "Print Summary" next to the existing "Calculate" button in the tech stats HBox.

### 2. `SettingsController.java`
Add `printTechSummary()` method:
- Validate: techCombo, fromDP, toDP must all be set
- Load rows via `WorkshopQueries.loadTechWorkByDateRange()`
- Filter for completed statuses
- Call `Print.printTechSummary(techName, from, to, rows, window)`

### 3. `PrintTechSummaryController.java` (new)
Pure programmatic `AnchorPane` builder — no FXML. Follows `PrintRepairController` pattern.

- Constants: `PAGE_W=595`, `PAGE_H=842`, `PAGE_CUTOFF=720`, `ROW_H=18`
- `buildPage1(techName, from, to, rows)` → `AnchorPane`
  - Header block: shop name, report title, tech + date range
  - Column headers row
  - Table rows until PAGE_CUTOFF; overflow rows stored in list
  - Hides totals if overflow exists
- `buildPageN(overflowRows, showTotals, totalLabour)` → `AnchorPane`
  - "...continued" header
  - Rows from overflow list (next batch up to PAGE_CUTOFF)
  - Totals block if `showTotals=true`
- `hasOverflow()` → boolean
- `getOverflowRows()` → remaining rows for next page

### 4. `Print.java`
Add static `printTechSummary(techName, from, to, rows, owner)`:
- Create `PrintTechSummaryController`
- Build page 1
- Loop: while overflow exists, build next page
- For each page: `job.printPage(pageLayout, page)`
- `job.endJob()` after all pages

## Pagination Logic

Matches existing `PrintRepairController` pattern:
- Track `currentY` as rows are added
- When `currentY >= PAGE_CUTOFF`, remaining rows go to overflow list
- Each call to `buildPageN` processes one page worth of overflow
- Totals only rendered on the final page

## Hardcoded Values

- Shop name: `"TELTRONICS SERVICE CENTRE"` (to be made configurable later)
- Paper: A4 portrait, `HARDWARE_MINIMUM` margins (same as existing print code)
