package Controllers;

import Controllers.DbRepo.ViewControllerQueries;
import DB.Vendors;
import Skeletons.*;
import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import utils.*;
import utils.enums.InvoiceType;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ViewOrderController {

    // ─── FXML FIELDS ────────────────────────────────────────────────────────────

    @FXML private MFXGenericDialog         dialogInstance;
    @FXML private ActualWorkshopController mainController;

    // Device info
    @FXML private MFXTextField         type;
    @FXML private MFXTextField         model;
    @FXML private MFXTextField         serialNumber;
    @FXML private TextArea             problemDesc;
    @FXML private MFXCheckbox          warrantyCheckBox;
    @FXML private MFXComboBox<String>  vendorId;
    @FXML private MFXTextField         warrantyNumber;

    // Location / PO — new fields
    @FXML private MFXTextField         locationTXF;
    @FXML private MFXTextField         poNumber;

    // Customer info
    @FXML private MFXTextField         idTFX;
    @FXML private MFXTextField         firstNameTXF;
    @FXML private MFXTextField         lastNameTXF;
    @FXML private MFXTextField         phoneTFX;
    @FXML private MFXTextField         addressTFX;
    @FXML private MFXTextField         townTFX;
    @FXML private MFXTextField         zipTFX;
    @FXML private MFXTextField         depositTXF;

    // Status / tech / tabs
    @FXML private TabPane              tabPane;
    @FXML private MFXComboBox<String>  statusCombo;
    @FXML private MFXComboBox<String>  techIdCombo;
    @FXML private TextArea             serviceNotesTXT;

    // Main tab header
    @FXML private MFXTextField         mainNumberTFX;

    // Labour tab header
    @FXML private MFXTextField         customerTFX;
    @FXML private MFXTextField         statusTFX;
    @FXML private MFXTextField         numberTFX;

    // Parts tab header
    @FXML private MFXTextField         partsCustomerTFX;
    @FXML private MFXTextField         partsStatusTFX;
    @FXML private MFXTextField         partsNumberTFX;

    // Tables & file list
    @FXML private MFXTableView<WorkTable>   repairTable;
    @FXML private MFXTableView<PartTable>   partsTable;
    @FXML private MFXListView<FilesHandler> filesList;

    // ─── OBSERVABLE DATA ────────────────────────────────────────────────────────

    private final ObservableList<WorkTable>    repairData = FXCollections.observableArrayList();
    private final ObservableList<PartTable>    partsData  = FXCollections.observableArrayList();
    private final ObservableList<FilesHandler> filesData  = FXCollections.observableArrayList();
    private final ObservableList<String>       techNames  = FXCollections.observableArrayList();

    public static final ObservableList<String> WORK_ORDER_STATUSES = FXCollections.observableArrayList(
            "New", "In Progress", "Waiting Parts", "Repair Complete", "Billing Complete", "Closed"
    );

    // ─── STATE ──────────────────────────────────────────────────────────────────

    public WorkOrder currentWorkOrder;
    public Customer  currentCustomer;

    private DeletingFilesMethods  deletingMethods;
    private DeletingPartsMethods  deletingPartsMethods;
    private DeletingLabourMethods deletingLabourMethods;

    private boolean isDirty   = false;
    private boolean isLoading = false;

    private static final long MAX_FILE_SIZE_BYTES = 3 * 1024 * 1024; // 3 MB

    // ─── SETTERS ────────────────────────────────────────────────────────────────

    public void setMainController(ActualWorkshopController controller) { this.mainController = controller; }
    public void setDialogInstance(MFXGenericDialog dialogInstance)      { this.dialogInstance = dialogInstance; }

    // ─── INITIALIZE ─────────────────────────────────────────────────────────────

    public void initialize() {
        tabPane.setFocusTraversable(false);
        statusCombo.setItems(WORK_ORDER_STATUSES);

        refreshTechList();
        setupTables();
        setupSpellCheck();
        setupDirtyListeners();
        setupTechComboListener();
        setupStatusComboListener();
        setupFileDoubleClick();
        setupDeletingHandlers();
        setupCustomerDoubleClick();

        tabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab.intValue() == 3 && currentWorkOrder != null) {
                loadFilesFromDb();
            }
        });
    }

    // ─── SPELL CHECK ────────────────────────────────────────────────────────────

    private void setupSpellCheck() {
        SpellCheckUtil.attach(type);
        SpellCheckUtil.attach(model);
        SpellCheckUtil.attach(serialNumber);
        SpellCheckUtil.attach(warrantyNumber);
        SpellCheckUtil.attach(locationTXF);
        SpellCheckUtil.attach(poNumber);
        SpellCheckUtil.attach(problemDesc);
        SpellCheckUtil.attach(serviceNotesTXT);
    }

    // ─── INIT DATA (called after initialize) ────────────────────────────────────

    public void initData(WorkOrder wo, Customer co) {
        isLoading = true;

        this.currentWorkOrder = wo;
        this.currentCustomer  = co;

        deletingMethods.setWorkorderNumber(wo.getWorkorderNumber());
        ViewControllerQueries.refreshWorkOrderFromDb(currentWorkOrder);

        List<String> techList = ViewControllerQueries.loadTechList();

        Platform.runLater(() -> {
            techNames.clear();
            techNames.addAll(techList);
            techIdCombo.setItems(techNames);

            populateCustomerFields(co);
            populateDeviceFields(wo);
            populateHeaderFields(wo, co);
            loadServiceNotes();
            loadPartsFromDb();
            loadRepairsFromDb();
            applyBillingLockUI();
            isLoading = false;
            isDirty   = false;
        });
    }

    // ─── SETUP HELPERS ──────────────────────────────────────────────────────────

    private void setupTables() {
        isLoading = true;
        repairTable.setFooterVisible(false);
        TableMethods.loadRepairsTable(repairTable, repairData, techNames);
        repairTable.setItems(repairData);

        partsTable.setFooterVisible(false);
        TableMethods.loadPartsTable(partsTable, partsData);
        partsTable.setItems(partsData);

        repairData.clear();
        partsData.clear();

        isLoading = false;
        isDirty   = false;
    }

    private void setupDirtyListeners() {
        // Device fields
        type.textProperty().addListener((obs, o, n)            -> markDirtyIfChanged(o, n));
        model.textProperty().addListener((obs, o, n)           -> markDirtyIfChanged(o, n));
        serialNumber.textProperty().addListener((obs, o, n)    -> markDirtyIfChanged(o, n));
        problemDesc.textProperty().addListener((obs, o, n)     -> markDirtyIfChanged(o, n));
        vendorId.textProperty().addListener((obs, o, n)        -> markDirtyIfChanged(o, n));
        warrantyNumber.textProperty().addListener((obs, o, n)  -> markDirtyIfChanged(o, n));
        serviceNotesTXT.textProperty().addListener((obs, o, n) -> markDirtyIfChanged(o, n));

        // New fields
        locationTXF.textProperty().addListener((obs, o, n)    -> markDirtyIfChanged(o, n));
        poNumber.textProperty().addListener((obs, o, n)        -> markDirtyIfChanged(o, n));

        // Combo boxes
        techIdCombo.valueProperty().addListener((obs, o, n)    -> markDirtyIfChanged(o, n));
        statusCombo.valueProperty().addListener((obs, o, n)    -> markDirtyIfChanged(o, n));

        // Table data
        repairData.addListener((javafx.collections.ListChangeListener<WorkTable>) c -> { if (!isLoading) isDirty = true; });
        partsData.addListener((javafx.collections.ListChangeListener<PartTable>)  c -> { if (!isLoading) isDirty = true; });
    }

    private void setupTechComboListener() {
        techIdCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isLoading || newVal == null || newVal.isBlank() || isBillingComplete()) return;

            int techId = ViewControllerQueries.getTechIdByUsername(newVal);
            currentWorkOrder.setTechId(techId);

            if ("New".equalsIgnoreCase(currentWorkOrder.getStatus())) {
                currentWorkOrder.setStatus("In Progress");
                statusCombo.selectItem("In Progress");
            }
        });
    }

    private void setupStatusComboListener() {
        statusCombo.valueProperty().addListener((obs, oldStatus, newStatus) -> {
            if (isLoading || newStatus == null || newStatus.equals(oldStatus)) return;

            if ("Repair Complete".equalsIgnoreCase(newStatus)) {
                saveRepairsToDb();

                // Guard 1 — labour note required
                if (!ViewControllerQueries.hasAtLeastOneLabourNoteDb(currentWorkOrder.getWorkorderNumber())) {
                    showWarning("Add at least one labour note (Tech + Description) before marking Repair Complete.");
                    isLoading = true;
                    statusCombo.selectItem(oldStatus);
                    isLoading = false;
                    return;
                }

                // Guard 2 — location required
                if (!isLocationFilled()) {
                    showWarning("Please fill in the Location field before marking Repair Complete.");
                    isLoading = true;
                    statusCombo.selectItem(oldStatus);
                    isLoading = false;
                    tabPane.getSelectionModel().select(0);
                    locationTXF.requestFocus();
                    return;
                }

                updateStatusInDb(newStatus);
                return;
            }

            currentWorkOrder.setStatus(newStatus);
        });
    }

    private void setupFileDoubleClick() {
        filesList.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getClickCount() == 2) {
                e.consume();
                onOpenSelectedFile();
            }
        });
    }

    private void setupDeletingHandlers() {
        deletingMethods = new DeletingFilesMethods(filesList);
        deletingMethods.setOnDelete(e -> {
            if (deletingMethods.deleteSelectedFile()) loadFilesFromDb();
        });

        deletingPartsMethods = new DeletingPartsMethods(partsTable);
        deletingPartsMethods.setOnDelete(e -> {
            if (isBillingComplete()) return;
            if (deletingPartsMethods.removeSelectedFromTable()) {
                savePartsToDb();
                reloadParts();
            }
        });

        deletingLabourMethods = new DeletingLabourMethods(repairTable);
        deletingLabourMethods.setOnDelete(e -> {
            if (isBillingComplete()) return;
            if (deletingLabourMethods.removeSelectedFromTable()) {
                saveRepairsToDb();
                reloadRepairs();
            }
        });
    }

    private void setupCustomerDoubleClick() {
        for (MFXTextField field : new MFXTextField[]{
                firstNameTXF, lastNameTXF, phoneTFX, addressTFX, townTFX, zipTFX, idTFX
        }) {
            field.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    try { openViewCustomerDialog(); }
                    catch (Exception ex) { ex.printStackTrace(); }
                }
            });
        }
    }

    private void openViewCustomerDialog() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/viewCustomer.fxml"));
        MFXGenericDialog dialog = loader.load();

        ViewCustomerController vc = loader.getController();
        vc.setCustomerData(currentCustomer);
        vc.setOnSaved(() -> populateCustomerFields(currentCustomer));

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Edit Customer");
        stage.setScene(new Scene(dialog));
        stage.showAndWait();
    }

    // ─── POPULATE UI ────────────────────────────────────────────────────────────

    private void populateCustomerFields(Customer co) {
        idTFX.setText(co.getId());
        firstNameTXF.setText(co.getFirstName());
        lastNameTXF.setText(co.getLastName());
        phoneTFX.setText(co.getPhone());
        addressTFX.setText(co.getAddress());
        townTFX.setText(co.getTown());
        zipTFX.setText(co.getPostalCode());
    }

    private void populateDeviceFields(WorkOrder wo) {
        statusCombo.selectItem(wo.getStatus());
        type.setText(wo.getType());
        model.setText(wo.getModel());
        serialNumber.setText(wo.getSerialNumber());
        problemDesc.setText(wo.getProblemDesc());
        depositTXF.setText("$" + wo.getDepositAmount());

        // Location & PO Number
        locationTXF.setText(wo.getLocation() != null ? wo.getLocation() : "");
        poNumber.setText(wo.getPoNumber()   != null ? wo.getPoNumber()  : "");

        boolean hasWarranty = wo.getVendorId() != null && !wo.getVendorId().isBlank();
        warrantyCheckBox.setSelected(hasWarranty);
        vendorId.setDisable(!hasWarranty);
        warrantyNumber.setDisable(!hasWarranty);

        vendorId.setItems(Vendors.loadIntoBox());
        if (hasWarranty) {
            vendorId.selectItem(wo.getVendorId());
            warrantyNumber.setText(wo.getWarrantyNumber());
        }
    }

    private void populateHeaderFields(WorkOrder wo, Customer co) {
        String fullName = co.getLastName() + "," + co.getFirstName();
        String woNum    = String.valueOf(wo.getWorkorderNumber());

        mainNumberTFX.setText(woNum);

        customerTFX.setText(fullName);
        statusTFX.setText(wo.getStatus());
        numberTFX.setText(woNum);

        partsCustomerTFX.setText(fullName);
        partsStatusTFX.setText(wo.getStatus());
        partsNumberTFX.setText(woNum);

        if (wo.getTechId() > 0) {
            techIdCombo.selectItem(ViewControllerQueries.getTechUsernameById(wo.getTechId()));
        } else {
            techIdCombo.clearSelection();
            techIdCombo.setText("");
        }
    }

    private void applyBillingLockUI() {
        boolean locked      = isBillingComplete();
        boolean hasWarranty = warrantyCheckBox.isSelected();

        statusCombo.setDisable(locked);
        type.setDisable(locked);
        model.setDisable(locked);
        serialNumber.setDisable(locked);
        problemDesc.setDisable(locked);
        warrantyCheckBox.setDisable(locked);
        vendorId.setDisable(locked || !hasWarranty);
        warrantyNumber.setDisable(locked || !hasWarranty);
        partsTable.setDisable(locked);
        repairTable.setDisable(locked);

        // Location and PO locked after billing complete
        locationTXF.setDisable(locked);
        poNumber.setDisable(locked);

        // Service notes always editable
        serviceNotesTXT.setDisable(false);
    }

    // ─── TECH LIST ──────────────────────────────────────────────────────────────

    private void refreshTechList() {
        techNames.clear();
        techNames.addAll(ViewControllerQueries.loadTechList());
        techIdCombo.setItems(techNames);
    }

    @FXML
    public void warrantySelected() {
        if (!warrantyCheckBox.isSelected()) {
            vendorId.setDisable(true);
            warrantyNumber.setDisable(true);
            vendorId.clearSelection();
            vendorId.setText("");
            warrantyNumber.setText("");
        } else {
            vendorId.setDisable(false);
            warrantyNumber.setDisable(false);
        }
    }

    // ─── SERVICE NOTES ──────────────────────────────────────────────────────────

    public void loadServiceNotes() {
        String notes = ViewControllerQueries.loadServiceNotes(currentWorkOrder.getWorkorderNumber());
        serviceNotesTXT.setText(notes != null ? notes : "");
    }

    public void insertNotes(TextArea area) {
        String stamp = "[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "]: ";
        String text  = area.getText();
        int caret    = area.getCaretPosition();

        if (caret > 0 && text.charAt(caret - 1) != '\n') stamp = "\n" + stamp;

        area.insertText(caret, stamp);
        area.positionCaret(caret + stamp.length());
    }

    // ─── REPAIRS ────────────────────────────────────────────────────────────────

    @FXML
    public void onAddStep() {
        if (isBillingComplete()) return;
        WorkTable row = new WorkTable(LocalDate.now(), "", "", 0.0);
        attachRepairListeners(row);
        repairData.add(row);
    }

    @FXML
    public void repairComplete() {
        repairTable.requestFocus();
        tabPane.requestFocus();
        saveRepairsToDb();

        // Guard 1 — at least one labour note
        if (!ViewControllerQueries.hasAtLeastOneLabourNoteDb(currentWorkOrder.getWorkorderNumber())) {
            showWarning("Add at least one labour note (Tech + Description) before marking Repair Complete.");
            return;
        }

        // Guard 2 — location must be filled
        if (!isLocationFilled()) {
            showWarning("Please fill in the Location field before marking Repair Complete.");
            tabPane.getSelectionModel().select(0);
            locationTXF.requestFocus();
            return;
        }

        updateStatusInDb("Repair Complete");
        statusCombo.selectItem("Repair Complete");
    }

    private void loadRepairsFromDb() {
        isLoading = true;
        repairTable.setItems(FXCollections.observableArrayList());
        repairData.clear();
        for (WorkTable row : ViewControllerQueries.loadRepairsFromDb(currentWorkOrder.getWorkorderNumber())) {
            attachRepairListeners(row);
            repairData.add(row);
        }
        repairTable.setItems(repairData);
        isLoading = false;
    }

    private void saveRepairsToDb() {
        ViewControllerQueries.saveRepairsToDb(currentWorkOrder.getWorkorderNumber(), repairData);
    }

    private void reloadRepairs() {
        isLoading = true;
        loadRepairsFromDb();
        isLoading = false;
        isDirty   = false;
    }

    private void attachRepairListeners(WorkTable r) {
        r.techProperty().addListener((obs, o, n)        -> markDirtyIfChanged(o, n));
        r.descriptionProperty().addListener((obs, o, n) -> markDirtyIfChanged(o, n));
        r.dateProperty().addListener((obs, o, n)        -> markDirtyIfChanged(o, n));
        r.priceProperty().addListener((obs, o, n)       -> { if (!isLoading && !n.equals(o)) isDirty = true; });
    }

    // ─── PARTS ──────────────────────────────────────────────────────────────────

    @FXML
    public void addPart() {
        if (isBillingComplete()) return;
        PartTable row = new PartTable("", 0, 0.0, 0);
        attachPartListeners(row);
        partsData.add(row);
    }

    private void loadPartsFromDb() {
        isLoading = true;
        partsTable.setItems(FXCollections.observableArrayList());
        partsData.clear();
        for (PartTable row : ViewControllerQueries.loadPartsFromDb(currentWorkOrder.getWorkorderNumber())) {
            attachPartListeners(row);
            partsData.add(row);
        }
        partsTable.setItems(partsData);
        isLoading = false;
    }

    private void savePartsToDb() {
        ViewControllerQueries.savePartsToDb(currentWorkOrder.getWorkorderNumber(), partsData);
    }

    private void reloadParts() {
        isLoading = true;
        loadPartsFromDb();
        isLoading = false;
        isDirty   = false;
    }

    private void attachPartListeners(PartTable p) {
        p.nameProperty().addListener((obs, o, n)       -> markDirtyIfChanged(o, n));
        p.quantityProperty().addListener((obs, o, n)   -> { if (!isLoading && !n.equals(o)) isDirty = true; });
        p.priceProperty().addListener((obs, o, n)      -> { if (!isLoading && !n.equals(o)) isDirty = true; });
        p.totalPriceProperty().addListener((obs, o, n) -> { if (!isLoading && !n.equals(o)) isDirty = true; });
    }

    // ─── FILES ──────────────────────────────────────────────────────────────────

    @FXML
    public void addFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Files");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
        File file = fc.showOpenDialog(dialogInstance.getScene().getWindow());
        if (file == null) return;

        if (file.length() > MAX_FILE_SIZE_BYTES) {
            showWarning("File is too large. Maximum allowed size is "
                    + (MAX_FILE_SIZE_BYTES / (1024 * 1024)) + " MB.");
            return;
        }

        try {
            ViewControllerQueries.addFileToDb(currentWorkOrder.getWorkorderNumber(), file);
            loadFilesFromDb();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadFilesFromDb() {
        filesList.getItems().clear();
        for (FilesHandler f : ViewControllerQueries.loadFilesFromDb(currentWorkOrder.getWorkorderNumber())) {
            filesList.getItems().add(f);
        }
    }

    public void onOpenSelectedFile() {
        FilesHandler selected = filesList.getSelectionModel().getSelectedValue();
        if (selected == null) return;
        openFileFromDb(selected.getId());
    }

    public void openFileFromDb(int fileId) {
        try {
            Object[] result = ViewControllerQueries.openFileFromDb(fileId);
            if (result == null) return;

            String      name = (String)      result[0];
            InputStream in   = (InputStream) result[1];
            String      ext  = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";

            File tmp = File.createTempFile("wo_", ext);
            tmp.deleteOnExit();
            Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            in.close();

            openWithDesktop(tmp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openWithDesktop(File file) {
        new Thread(() -> {
            try { Desktop.getDesktop().open(file); }
            catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    // ─── SAVE / UPDATE ORDER ────────────────────────────────────────────────────

    @FXML
    public void updateOrder() {
        int woNumber = currentWorkOrder.getWorkorderNumber();

        // Billing complete — only service notes can change
        if (isBillingComplete()) {
            ViewControllerQueries.updateServiceNotesInDb(woNumber, serviceNotesTXT.getText());
            isDirty = false;
            return;
        }

        int    techId = currentWorkOrder.getTechId();
        String status = currentWorkOrder.getStatus();

        if ("Repair Complete".equalsIgnoreCase(status)) {
            if (techId <= 0) {
                showWarning("Please select a technician before marking Repair Complete.");
                return;
            }
            saveRepairsToDb();
            if (!ViewControllerQueries.hasAtLeastOneLabourNoteDb(woNumber)) {
                showWarning("Add at least one labour note (Tech + Description) before marking Repair Complete.");
                return;
            }
            if (!isLocationFilled()) {
                showWarning("Please fill in the Location field before marking Repair Complete.");
                tabPane.getSelectionModel().select(0);
                locationTXF.requestFocus();
                return;
            }
        }

        ViewControllerQueries.updateOrderInDb(
                woNumber,
                type.getText(), model.getText(), serialNumber.getText(),
                problemDesc.getText(), vendorId.getText(), warrantyNumber.getText(),
                serviceNotesTXT.getText(), techId, status,
                locationTXF.getText(),
                poNumber.getText()
        );

        savePartsToDb();
        saveRepairsToDb();
        mainController.LoadOrders();
        isDirty = false;
    }

    // ─── STATUS UPDATE ──────────────────────────────────────────────────────────

    private void updateStatusInDb(String newStatus) {
        System.out.println("[TAX DEBUG] updateStatusInDb called: " + newStatus);

        if ("Repair Complete".equalsIgnoreCase(newStatus) ||
                "Billing Complete".equalsIgnoreCase(newStatus)) {

            boolean hasWarranty = currentWorkOrder.getVendorId() != null
                    && !currentWorkOrder.getVendorId().isBlank();
            boolean hasPstNum = currentCustomer.getPstNumber() != null
                    && !currentCustomer.getPstNumber().isBlank();
            boolean hasGstNum = currentCustomer.getGstNumber() != null
                    && !currentCustomer.getGstNumber().isBlank();

            double labour = ViewControllerQueries.labourTotalDb(currentWorkOrder.getWorkorderNumber());
            double parts  = ViewControllerQueries.partsTotalDb(currentWorkOrder.getWorkorderNumber());

            System.out.println("[TAX DEBUG] labour=" + labour + " parts=" + parts
                    + " warranty=" + hasWarranty + " pstExempt=" + hasPstNum + " gstExempt=" + hasGstNum);

            double[] taxes = DB.ShopSettings.calcTaxes(labour, parts, hasWarranty, hasPstNum, hasGstNum);

            System.out.println("[TAX DEBUG] pst=" + taxes[0] + " gst=" + taxes[1]);

            ViewControllerQueries.saveTaxesToDb(
                    currentWorkOrder.getWorkorderNumber(),
                    taxes[0], taxes[1]
            );
        }

        ViewControllerQueries.updateStatusInDb(
                currentWorkOrder.getWorkorderNumber(),
                newStatus,
                ViewControllerQueries.labourTotalDb(currentWorkOrder.getWorkorderNumber())
        );

        currentWorkOrder.setStatus(newStatus);
        statusTFX.setText(newStatus);
        partsStatusTFX.setText(newStatus);
        mainController.LoadOrders();
        applyBillingLockUI();
    }

    // ─── PAYMENT ────────────────────────────────────────────────────────────────

    @FXML
    public void Pay() throws Exception {
        ViewControllerQueries.refreshWorkOrderFromDb(currentWorkOrder);

        if (!isRepairComplete()) {
            showWarning("You must set the work order status to 'Repair Complete' before taking final payment.");
            return;
        }

        openPaymentDialog(currentWorkOrder, currentCustomer, InvoiceType.FINAL);
    }

    private void openPaymentDialog(WorkOrder wo, Customer co, InvoiceType invoiceType) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/pay.fxml"));
        Parent root = loader.load();

        PaymentController pc = loader.getController();
        pc.setMainController(mainController);

        Window owner = dialogInstance.getScene().getWindow();

        if (invoiceType == InvoiceType.FINAL) {
            savePartsToDb();
            saveRepairsToDb();
            isLoading = true;
            loadPartsFromDb();
            loadRepairsFromDb();
            isLoading = false;
            isDirty   = false;
            pc.setSuggestedAmount(finalDueDb());
        }

        pc.setContext(wo, co, invoiceType, owner);

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle("Payment");
        stage.setScene(new Scene(root));
        stage.showAndWait();

        loadFilesFromDb();
    }

    // ─── PRINT ──────────────────────────────────────────────────────────────────

    @FXML
    public void printOrder() throws Exception {
        Window owner = dialogInstance.getScene().getWindow();
        DocumentOutput.printOrPdf(
                "WO_" + currentWorkOrder.getWorkorderNumber(),
                "/main/printOrder.fxml",
                loader -> {
                    PrinterController pc = loader.getController();
                    pc.initData(currentWorkOrder, currentCustomer);
                },
                owner
        );
    }

    @FXML
    public void printRepaired() throws Exception {
        Window owner = dialogInstance.getScene().getWindow();
        DocumentOutput.printOrPdf(
                "WO_REPAIRED_" + currentWorkOrder.getWorkorderNumber(),
                "/main/repairCompletedNew.fxml",
                loader -> {
                    PrintRepairController pc = loader.getController();
                    pc.initData(currentWorkOrder, currentCustomer, repairData, partsData);
                },
                owner
        );
    }

    @FXML
    public void openPrintOptions() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/printMenu.fxml"));
            Parent root = loader.load();

            PrintOptionsController poc = loader.getController();
            poc.setViewOrderController(this);
            poc.setWoNumber(currentWorkOrder.getWorkorderNumber());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(dialogInstance.getScene().getWindow());
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            stage.setScene(new Scene(root));
            poc.setStage(stage);

            // centre over the dialog
            javafx.geometry.Bounds b = dialogInstance.localToScreen(dialogInstance.getBoundsInLocal());
            if (b != null) {
                stage.setX(b.getMinX() + (b.getWidth()  - 260) / 2);
                stage.setY(b.getMinY() + (b.getHeight() - 185) / 2);
            }

            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void printEstimate() throws Exception {
        Window owner = dialogInstance.getScene().getWindow();
        DocumentOutput.printOrPdf(
                "WO_ESTIMATE_" + currentWorkOrder.getWorkorderNumber(),
                "/main/estimate.fxml",
                loader -> {
                    PrintEstimateController ec = loader.getController();
                    ec.initData(currentWorkOrder, currentCustomer, repairData, partsData);
                },
                owner
        );
    }

    // ─── CLOSE DIALOG ───────────────────────────────────────────────────────────

    @FXML
    public void closeDialog() {
        if (!isDirty) { actuallyCloseDialog(); return; }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("You have unsaved changes.");
        alert.setContentText("Do you want to save before closing?");

        ButtonType saveBtn    = new ButtonType("Yes (Save)");
        ButtonType discardBtn = new ButtonType("No (Discard)");
        ButtonType cancelBtn  = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(saveBtn, discardBtn, cancelBtn);

        alert.showAndWait().ifPresent(response -> {
            if (response == saveBtn)         { updateOrder(); actuallyCloseDialog(); }
            else if (response == discardBtn) { actuallyCloseDialog(); }
        });
    }

    private void actuallyCloseDialog() {
        mainController.rootStack.getChildren().remove(dialogInstance);
        mainController.contentPane.setEffect(null);
        mainController.contentPane.setDisable(false);
    }

    // ─── UTILITY / HELPERS ──────────────────────────────────────────────────────

    private boolean isBillingComplete() {
        String s = currentWorkOrder.getStatus();
        return s != null && s.equalsIgnoreCase("Billing Complete");
    }

    private boolean isRepairComplete() {
        String s = currentWorkOrder.getStatus();
        return s != null && s.equalsIgnoreCase("Repair Complete");
    }

    /** Returns true only when locationTXF contains non-blank text. */
    private boolean isLocationFilled() {
        return locationTXF.getText() != null && !locationTXF.getText().isBlank();
    }

    private double finalDueDb() {
        int      woNumber = currentWorkOrder.getWorkorderNumber();
        double   labour   = ViewControllerQueries.labourTotalDb(woNumber);
        double   parts    = ViewControllerQueries.partsTotalDb(woNumber);
        double   deposit  = ViewControllerQueries.depositFromDb(woNumber);
        double[] taxes    = ViewControllerQueries.taxesFromDb(woNumber);
        return Math.max(0, labour + parts + taxes[0] + taxes[1] - deposit);
    }

    /** Marks isDirty only when not loading and the value actually changed. */
    private void markDirtyIfChanged(Object oldVal, Object newVal) {
        if (!isLoading && (oldVal == null ? newVal != null : !oldVal.equals(newVal))) isDirty = true;
    }

    /** Shows a WARNING alert with a single OK button. */
    private void showWarning(String message) {
        new Alert(Alert.AlertType.WARNING, message, ButtonType.OK).showAndWait();
    }
}