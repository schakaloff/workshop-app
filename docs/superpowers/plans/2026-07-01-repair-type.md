# Repair Type (In-Shop / In-Home) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a required "Repair Type" dropdown (In-Shop / In-Home) to work order creation, and a matching editable dropdown on the View Order screen, backed by a new `work_order.repair_type` DB column.

**Architecture:** Follows the existing `location`/`po_number` pattern already in this codebase: a plain `String` field on `Skeletons.WorkOrder`, a fixed-choice `MFXComboBox<String>` in each FXML, wired through `NewOrderController`/`ViewOrderController`, persisted via `Controllers.DbRepo.WorkshopQueries`/`ViewControllerQueries`.

**Tech Stack:** JavaFX 21, MaterialFX (`io.github.palexdev.materialfx`), MySQL (manual schema changes, no migration framework in this repo).

## Global Constraints

- Dropdown values are exactly `"In-Shop Repair Check"` and `"In-Home Repair Check"` (spec: `docs/superpowers/specs/2026-07-01-repair-type-design.md`).
- DB column: `work_order.repair_type VARCHAR(30) NOT NULL DEFAULT 'In-Shop Repair Check'`.
- Repair type is a **required** field only at work-order creation (New Order dialog). It is freely editable afterward on View Order — no "Repair Complete" guard.
- No test harness exists for controllers/FXML in this repo (`src/test` is empty, JUnit is a dependency but unused for GUI code). Verification for each task is `mvn -q compile` plus, where noted, a manual DB check over the `teltron-server` SSH connection — not automated unit tests.
- Follow existing code style exactly: same null-guarding pattern as `setLocation`/`getLocation` in `Skeletons/WorkOrder.java`, same SQL/PreparedStatement idioms in `Controllers/DbRepo/*.java`.

---

### Task 1: DB column + `WorkOrder` model field

**Files:**
- Modify: `src/main/java/Skeletons/WorkOrder.java`
- DB: `work_order` table (via SSH `teltron-server`)

**Interfaces:**
- Produces: `WorkOrder.getRepairType(): String`, `WorkOrder.setRepairType(String): void` — used by Tasks 2-5.

- [ ] **Step 1: Add the DB column**

Run over SSH:

```bash
ssh teltron-server "mysql -u <user> -p<password> <database> -e \"ALTER TABLE work_order ADD COLUMN repair_type VARCHAR(30) NOT NULL DEFAULT 'In-Shop Repair Check';\""
```

(Use the same MySQL credentials/database name already configured in `DB/DbConfig.java` — read that file first to fill in `<user>`/`<password>`/`<database>` rather than guessing.)

Expected: command exits 0, no error output.

- [ ] **Step 2: Verify the column exists**

```bash
ssh teltron-server "mysql -u <user> -p<password> <database> -e \"DESCRIBE work_order;\"" | grep repair_type
```

Expected output line: `repair_type    varchar(30)    NO         In-Shop Repair Check`

- [ ] **Step 3: Add the field to `WorkOrder.java`**

In `src/main/java/Skeletons/WorkOrder.java`, add alongside the other "set after construction" fields (near `location`/`poNumber`):

```java
    // line 24, after: private String poNumber;
    private String repairType;
```

In the constructor, alongside `this.poNumber = "";`:

```java
        this.repairType   = "";
```

Add getter alongside `getPoNumber()`:

```java
    public String getRepairType()      { return repairType; }
```

Add setter alongside `setPoNumber(...)`:

```java
    public void setRepairType(String repairType)         { this.repairType = repairType != null ? repairType : ""; }
```

- [ ] **Step 4: Compile**

```bash
mvn -q -pl . compile
```

Expected: `BUILD SUCCESS`, no output on success (add `-e` if it fails and re-read the error).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/Skeletons/WorkOrder.java
git commit -m "feat: add repairType field to WorkOrder model"
```

---

### Task 2: Insert path — `WorkshopQueries` + `ActualWorkshopController`

**Files:**
- Modify: `src/main/java/Controllers/DbRepo/WorkshopQueries.java:137-166`
- Modify: `src/main/java/Controllers/ActualWorkshopController.java:664-669`

**Interfaces:**
- Consumes: nothing new from Task 1 (raw `String` param, not `WorkOrder`).
- Produces: `WorkshopQueries.insertOrderIntoDatabase(String status, String type, String model, String serialNumber, String problemDesc, int customerId, String vendorId, String warrantyNumber, double deposit, String repairType): int` and the same signature on `ActualWorkshopController.insertOrderIntoDatabase(...)` — used by Task 3.

- [ ] **Step 1: Update `WorkshopQueries.insertOrderIntoDatabase`**

Replace the method body (`src/main/java/Controllers/DbRepo/WorkshopQueries.java:137-166`) with:

```java
    public int insertOrderIntoDatabase(String status, String type, String model, String serialNumber,
                                       String problemDesc, int customerId, String vendorId,
                                       String warrantyNumber, double deposit, String repairType) {
        String sql = "INSERT INTO work_order (status, type, model, serialNumber, problemDesc, customer_id, vendorId, warrantyNumber, deposit_amount, repair_type, createdAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, status);
            stmt.setString(2, type);
            stmt.setString(3, model);
            stmt.setString(4, serialNumber);
            stmt.setString(5, problemDesc);
            stmt.setInt(6, customerId);
            stmt.setString(7, vendorId);
            stmt.setString(8, warrantyNumber);
            stmt.setDouble(9, deposit);
            stmt.setString(10, repairType);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }
```

- [ ] **Step 2: Update `ActualWorkshopController.insertOrderIntoDatabase`**

Replace `src/main/java/Controllers/ActualWorkshopController.java:664-669` with:

```java
    public int insertOrderIntoDatabase(String status, String type, String model, String serialNumber,
                                       String problemDesc, int customerId, String vendorId,
                                       String warrantyNumber, double deposit, String repairType) {
        return workshopQueries.insertOrderIntoDatabase(status, type, model, serialNumber,
                problemDesc, customerId, vendorId, warrantyNumber, deposit, repairType);
    }
```

- [ ] **Step 3: Compile**

```bash
mvn -q compile
```

Expected: fails here because `NewOrderController.java:139` still calls the old 9-arg overload — that's expected, it's fixed in Task 3. Confirm the *only* compile error mentions `NewOrderController.java` and `insertOrderIntoDatabase`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/Controllers/DbRepo/WorkshopQueries.java src/main/java/Controllers/ActualWorkshopController.java
git commit -m "feat: thread repairType through insertOrderIntoDatabase"
```

---

### Task 3: New Order UI — `newOrder.fxml` + `NewOrderController`

**Files:**
- Modify: `src/main/resources/main/newOrder.fxml:106-116` (problem-description block) and the region just above it (customer row ends at line 104)
- Modify: `src/main/java/Controllers/NewOrderController.java`

**Interfaces:**
- Consumes: `mainController.insertOrderIntoDatabase(..., String repairType)` from Task 2.
- Produces: `NewOrderController.repairTypeCombo` (`MFXComboBox<String>`) — no other task depends on this directly.

- [ ] **Step 1: Insert the Repair Type row and shrink Problem Description in `newOrder.fxml`**

In `src/main/resources/main/newOrder.fxml`, replace lines 106-116:

```xml
            <!-- ─── Problem description ──────────────────────────────── -->
            <VBox style="-fx-background-color: white; -fx-padding: 12 16 12 16; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;">
               <children>
                  <TextArea fx:id="problemDesc"
                            prefHeight="110" prefWidth="748"
                            promptText="Problem / Complaint"
                            wrapText="true"
                            style="-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 12;" />
               </children>
            </VBox>
```

with:

```xml
            <!-- ─── Repair type row ──────────────────────────────────── -->
            <HBox spacing="12" alignment="CENTER_LEFT"
                  style="-fx-background-color: #fafbfc; -fx-padding: 10 16 10 16; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;">
               <children>
                  <MFXComboBox fx:id="repairTypeCombo"
                               floatMode="BORDER" floatingText="Repair Type"
                               prefHeight="34" prefWidth="220" />
               </children>
            </HBox>

            <!-- ─── Problem description ──────────────────────────────── -->
            <VBox style="-fx-background-color: white; -fx-padding: 12 16 12 16; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;">
               <children>
                  <TextArea fx:id="problemDesc"
                            prefHeight="70" prefWidth="748"
                            promptText="Problem / Complaint"
                            wrapText="true"
                            style="-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 12;" />
               </children>
            </VBox>
```

- [ ] **Step 2: Wire the field in `NewOrderController.java`**

Add the field declaration next to the other `@FXML` fields (after `@FXML private TextArea problemDesc;`, around line 36):

```java
    @FXML private MFXComboBox<String> repairTypeCombo;
```

In `initialize()`, add after `vendorId.setItems(Vendors.loadIntoBox());` (around line 67):

```java
        repairTypeCombo.setItems(javafx.collections.FXCollections.observableArrayList(
                "In-Shop Repair Check", "In-Home Repair Check"));
```

- [ ] **Step 3: Enforce required + pass value through in `makeNewOrder()`**

In `makeNewOrder()`, replace:

```java
        String vendorIdDb = vendorId.getText();
        String warrantyNumberDb= warrantyNumber.getText();

        String stringId = idTFX.getText();
        Double depositDB = Double.valueOf(depositTXF.getText());

        if(typeDB.isBlank() || modelDB.isBlank() || stringId.isBlank()){
            new Alert(Alert.AlertType.WARNING, "Please fill out the fields", ButtonType.OK).showAndWait();
            return;
        }
```

with:

```java
        String vendorIdDb = vendorId.getText();
        String warrantyNumberDb= warrantyNumber.getText();

        String stringId = idTFX.getText();
        Double depositDB = Double.valueOf(depositTXF.getText());
        String repairTypeDb = repairTypeCombo.getText();

        if(typeDB.isBlank() || modelDB.isBlank() || stringId.isBlank()){
            new Alert(Alert.AlertType.WARNING, "Please fill out the fields", ButtonType.OK).showAndWait();
            return;
        }
        if(repairTypeDb == null || repairTypeDb.isBlank()){
            new Alert(Alert.AlertType.WARNING, "Please select a Repair Type", ButtonType.OK).showAndWait();
            return;
        }
```

Then replace the two lines that build the order:

```java
        int newId = mainController.insertOrderIntoDatabase("New", typeDB, modelDB, serialNumberDB, problemDescDB, customerId, vendorIdDb, warrantyNumberDb, depositDB);

        mainController.reloadOrders();

        WorkOrder wo = new WorkOrder(Integer.valueOf(newId), "New", typeDB, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), vendorIdDb, warrantyNumberDb, modelDB, serialNumberDB, problemDescDB, customerId, depositDB);
```

with:

```java
        int newId = mainController.insertOrderIntoDatabase("New", typeDB, modelDB, serialNumberDB, problemDescDB, customerId, vendorIdDb, warrantyNumberDb, depositDB, repairTypeDb);

        mainController.reloadOrders();

        WorkOrder wo = new WorkOrder(Integer.valueOf(newId), "New", typeDB, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), vendorIdDb, warrantyNumberDb, modelDB, serialNumberDB, problemDescDB, customerId, depositDB);
        wo.setRepairType(repairTypeDb);
```

- [ ] **Step 4: Compile**

```bash
mvn -q compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Manual check**

Run the app (`mvn javafx:run` or the project's existing run command — check `README`/`pom.xml` `exec`/`javafx` plugin config if unsure), open New Order, confirm:
- "Repair Type" dropdown shows both options.
- Trying to create an order without picking one shows the warning and does not create a row.
- Creating an order with a type selected succeeds; check the DB row has the right `repair_type` value:

```bash
ssh teltron-server "mysql -u <user> -p<password> <database> -e \"SELECT workorder, repair_type FROM work_order ORDER BY workorder DESC LIMIT 1;\""
```

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/main/newOrder.fxml src/main/java/Controllers/NewOrderController.java
git commit -m "feat: require Repair Type on New Order creation"
```

---

### Task 4: Persistence path — `ViewControllerQueries`

**Files:**
- Modify: `src/main/java/Controllers/DbRepo/ViewControllerQueries.java:116-144` (`refreshWorkOrderFromDb`)
- Modify: `src/main/java/Controllers/DbRepo/ViewControllerQueries.java:288-334` (`updateOrderInDb`)

**Interfaces:**
- Consumes: `WorkOrder.setRepairType(String)`/`getRepairType()` from Task 1.
- Produces: `ViewControllerQueries.updateOrderInDb(int, String, String, String, String, String, String, String, int, String, String, String, String)` (repairType appended as new last param) — used by Task 5.

- [ ] **Step 1: Update `refreshWorkOrderFromDb`**

Replace lines 116-144 with:

```java
    public static void refreshWorkOrderFromDb(WorkOrder currentWorkOrder) {
        String sql = "SELECT status, type, model, serialNumber, problemDesc, vendorId, warrantyNumber, tech_id, location, po_number, repair_type FROM work_order WHERE workorder = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, currentWorkOrder.getWorkorderNumber());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                currentWorkOrder.setStatus(rs.getString("status"));
                currentWorkOrder.setType(rs.getString("type"));
                currentWorkOrder.setModel(rs.getString("model"));
                currentWorkOrder.setSerialNumber(rs.getString("serialNumber"));
                currentWorkOrder.setProblemDesc(rs.getString("problemDesc"));
                currentWorkOrder.setVendorId(rs.getString("vendorId"));
                currentWorkOrder.setWarrantyNumber(rs.getString("warrantyNumber"));
                currentWorkOrder.setLocation(rs.getString("location"));
                currentWorkOrder.setPoNumber(rs.getString("po_number"));
                currentWorkOrder.setRepairType(rs.getString("repair_type"));

                int techId = rs.getInt("tech_id");
                if (rs.wasNull()) techId = 0;
                currentWorkOrder.setTechId(techId);
            }

        } catch (SQLException e) {
            System.out.println("issue during refreshing work order");
        }
    }
```

- [ ] **Step 2: Update `updateOrderInDb`**

Replace lines 288-334 with:

```java
    public static void updateOrderInDb(int workorderNumber, String type, String model, String serialNumber,
                                       String problemDesc, String vendorId, String warrantyNumber,
                                       String serviceNotes, int techId, String status,
                                       String location, String poNumber, String repairType) {
        String sql = """
        UPDATE work_order
        SET type = ?,
            model = ?,
            serialNumber = ?,
            problemDesc = ?,
            vendorId = ?,
            warrantyNumber = ?,
            service_notes = ?,
            tech_id = ?,
            status = ?,
            location = ?,
            po_number = ?,
            repair_type = ?
        WHERE workorder = ?
    """;

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, type);
            ps.setString(2, model);
            ps.setString(3, serialNumber);
            ps.setString(4, problemDesc);
            ps.setString(5, vendorId);
            ps.setString(6, warrantyNumber);
            ps.setString(7, serviceNotes);
            if (techId > 0) {           // ← NULL when no tech assigned
                ps.setInt(8, techId);
            } else {
                ps.setNull(8, java.sql.Types.INTEGER);
            }
            ps.setString(9, status);
            ps.setString(10, location);
            ps.setString(11, poNumber);
            ps.setString(12, repairType);
            ps.setInt(13, workorderNumber);

            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println("error during updating order");
            e.printStackTrace();
        }
    }
```

- [ ] **Step 3: Compile**

```bash
mvn -q compile
```

Expected: fails at `ViewOrderController.java:684` (`updateOrderInDb` call still has the old arg count) — expected, fixed in Task 5. Confirm that's the only error.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/Controllers/DbRepo/ViewControllerQueries.java
git commit -m "feat: thread repairType through ViewControllerQueries read/write"
```

---

### Task 5: View Order UI — `viewOrder.fxml` + `ViewOrderController`

**Files:**
- Modify: `src/main/resources/main/viewOrder.fxml:93-105` (Location + PO Number row)
- Modify: `src/main/java/Controllers/ViewOrderController.java`

**Interfaces:**
- Consumes: `ViewControllerQueries.updateOrderInDb(..., String repairType)` and `refreshWorkOrderFromDb` populating `getRepairType()` from Task 4; `WorkOrder.getRepairType()/setRepairType()` from Task 1.
- Produces: nothing consumed by later tasks — this is the last task.

- [ ] **Step 1: Add the combo box to `viewOrder.fxml`**

Replace lines 93-105:

```xml
                              <!-- ── Location + PO Number row ──────────────────────────── -->
                              <HBox spacing="12" alignment="CENTER_LEFT"
                                    style="-fx-background-color: #fafbfc; -fx-padding: 10 16 10 16; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;">
                                 <children>
                                    <MFXTextField fx:id="locationTXF"
                                                  floatMode="BORDER" floatingText="Location  (required for Repair Complete)"
                                                  prefHeight="34" prefWidth="340"
                                                  style="-fx-border-color: #0097A7; -fx-border-radius: 4; -fx-background-radius: 4;" />
                                    <MFXTextField fx:id="poNumber"
                                                  floatMode="BORDER" floatingText="PO Number"
                                                  prefHeight="34" prefWidth="180" />
                                 </children>
                              </HBox>
```

with:

```xml
                              <!-- ── Location + PO Number + Repair Type row ─────────────── -->
                              <HBox spacing="12" alignment="CENTER_LEFT"
                                    style="-fx-background-color: #fafbfc; -fx-padding: 10 16 10 16; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;">
                                 <children>
                                    <MFXTextField fx:id="locationTXF"
                                                  floatMode="BORDER" floatingText="Location  (required for Repair Complete)"
                                                  prefHeight="34" prefWidth="340"
                                                  style="-fx-border-color: #0097A7; -fx-border-radius: 4; -fx-background-radius: 4;" />
                                    <MFXTextField fx:id="poNumber"
                                                  floatMode="BORDER" floatingText="PO Number"
                                                  prefHeight="34" prefWidth="180" />
                                    <MFXComboBox fx:id="repairTypeCombo"
                                                 floatMode="BORDER" floatingText="Repair Type"
                                                 prefHeight="34" prefWidth="200" />
                                 </children>
                              </HBox>
```

- [ ] **Step 2: Field declaration + item list in `ViewOrderController.java`**

Add next to `@FXML private MFXTextField poNumber;` (around line 58):

```java
    @FXML private MFXComboBox<String>  repairTypeCombo;
```

In `initialize()`, add after `statusCombo.setItems(WORK_ORDER_STATUSES);` (around line 129):

```java
        repairTypeCombo.setItems(FXCollections.observableArrayList(
                "In-Shop Repair Check", "In-Home Repair Check"));
```

- [ ] **Step 3: Populate on load**

In `populateDeviceFields`, add after `poNumber.setText(wo.getPoNumber() != null ? wo.getPoNumber() : "");` (around line 386):

```java
        repairTypeCombo.selectItem(wo.getRepairType() != null && !wo.getRepairType().isBlank()
                ? wo.getRepairType() : "In-Shop Repair Check");
```

- [ ] **Step 4: Dirty-tracking**

In `setupDirtyListeners()`, add after `poNumber.textProperty().addListener((obs, o, n) -> markDirtyIfChanged(o, n));` (around line 242):

```java
        repairTypeCombo.valueProperty().addListener((obs, o, n)  -> markDirtyIfChanged(o, n));
```

- [ ] **Step 5: Save on update**

In `updateOrder()`, replace the `updateOrderInDb` call:

```java
        ViewControllerQueries.updateOrderInDb(
                woNumber,
                type.getText(), model.getText(), serialNumber.getText(),
                problemDesc.getText(), vendorId.getText(), warrantyNumber.getText(),
                serviceNotesTXT.getText(), techId, status,
                locationTXF.getText(),
                poNumber.getText()
        );
```

with:

```java
        ViewControllerQueries.updateOrderInDb(
                woNumber,
                type.getText(), model.getText(), serialNumber.getText(),
                problemDesc.getText(), vendorId.getText(), warrantyNumber.getText(),
                serviceNotesTXT.getText(), techId, status,
                locationTXF.getText(),
                poNumber.getText(),
                repairTypeCombo.getText()
        );
```

- [ ] **Step 6: Compile**

```bash
mvn -q compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Manual check**

Run the app, open an existing work order (View Order):
- Confirm "Repair Type" shows on the Location/PO row, pre-selected to whatever was stored (or "In-Shop Repair Check" if the row predates this change).
- Change it, hit Save, close and reopen the same order — confirm the new value persisted.
- Confirm DB directly:

```bash
ssh teltron-server "mysql -u <user> -p<password> <database> -e \"SELECT workorder, repair_type FROM work_order WHERE workorder = <the woNumber you just edited>;\""
```

- [ ] **Step 8: Commit**

```bash
git add src/main/resources/main/viewOrder.fxml src/main/java/Controllers/ViewOrderController.java
git commit -m "feat: show and save Repair Type on View Order"
```

---

## Self-Review Notes

- **Spec coverage:** DB column (Task 1), New Order required dropdown + layout change (Task 3), View Order dropdown on PO row (Task 5), persistence both directions (Tasks 2 & 4) — all spec sections covered. Print templates intentionally untouched per spec's "Out of scope".
- **Placeholder scan:** no TBD/TODO; all code blocks are complete, copy-pasteable.
- **Type consistency:** `repairType`/`getRepairType`/`setRepairType`/`repairTypeCombo` used identically across all five tasks; `updateOrderInDb` new signature (13 args, `repairType` last) matches between Task 4's definition and Task 5's call site.
