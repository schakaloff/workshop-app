# Repair Type (In-Shop / In-Home) — Design Spec

## Purpose
Work orders need to record whether the repair happens in-shop or at the
customer's location, chosen at creation time and editable afterward.

## Data model
- New column: `work_order.repair_type VARCHAR(30) NOT NULL DEFAULT 'In-Shop Repair Check'`
  - Applied manually via SSH (`teltron-server`) — this repo has no migration
    files; schema changes for `location`/`po_number` were done the same way.
- `Skeletons/WorkOrder.java`: add `repairType` field, getter, setter
  (defaults to `""`, same pattern as `location`).
- Allowed values (fixed dropdown, not free text):
  - `"In-Shop Repair Check"`
  - `"In-Home Repair Check"`

## New Order (`newOrder.fxml`, `NewOrderController`)
- New `MFXComboBox<String>` (`repairTypeCombo`) row inserted between the
  Customer row and the Problem Description box, floatingText "Repair Type",
  items = the two values above.
- `problemDesc` TextArea height reduced 110 → 70 to make room (matches the
  height already used in `viewOrder.fxml`).
- Required field: `makeNewOrder()` blocks save (same warning alert used for
  Type/Model/ID) if no repair type is selected.
- Value flows through `insertOrderIntoDatabase(...)` (new trailing param) →
  `ActualWorkshopController.insertOrderIntoDatabase` →
  `WorkshopQueries.insertOrderIntoDatabase` INSERT statement.

## View Order (`viewOrder.fxml`, `ViewOrderController`)
- New `MFXComboBox<String>` (`repairTypeCombo`) added to the existing
  "Location + PO Number" row (same row, after PO Number).
- Populated in `populateDeviceFields` from `wo.getRepairType()`.
- Included in the dirty-tracking listeners (same pattern as `poNumber`).
- Saved via `ViewControllerQueries.updateOrderInDb` (new param), loaded via
  `refreshWorkOrderFromDb` (new `repair_type` column in the SELECT).
- Not enforced as a "Repair Complete" guard — required only at order
  creation, freely editable afterward like other Main-tab fields.

## Out of scope
- Print templates (`printOrder.fxml`, `repairCompletedNew.fxml`, estimate) —
  not touched.
- No guard preventing status changes if repair type is somehow blank on an
  older order (pre-existing rows get the DB default).

## Files touched
- `Skeletons/WorkOrder.java`
- `Controllers/NewOrderController.java`, `src/main/resources/main/newOrder.fxml`
- `Controllers/ViewOrderController.java`, `src/main/resources/main/viewOrder.fxml`
- `Controllers/DbRepo/WorkshopQueries.java`
- `Controllers/DbRepo/ViewControllerQueries.java`
- `Controllers/ActualWorkshopController.java`
- DB: `ALTER TABLE work_order ADD COLUMN repair_type ...` (manual, via `teltron-server`)
