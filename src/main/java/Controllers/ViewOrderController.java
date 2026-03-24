package Controllers;
import Controllers.DbRepo.ViewControllerQueries;
import DB.DbConfig;
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

public class ViewOrderController {
    @FXML private ActualWorkshopController mainController;
    @FXML private MFXGenericDialog dialogInstance;

    @FXML private MFXComboBox<String> vendorId;
    @FXML private MFXTextField warrantyNumber;

    @FXML private MFXComboBox<String> statusCombo;

    @FXML private MFXTextField type;
    @FXML private MFXTextField model;
    @FXML private MFXTextField serialNumber;
    @FXML private TextArea problemDesc;

    @FXML private MFXTextField idTFX;
    @FXML private MFXTextField firstNameTXF;
    @FXML private MFXTextField lastNameTXF;
    @FXML private MFXTextField phoneTFX;
    @FXML private MFXTextField addressTFX;
    @FXML private MFXTextField townTFX;
    @FXML private MFXTextField zipTFX;
    @FXML private MFXTextField depositTXF;

    @FXML private TabPane tabPane;
    @FXML private TextArea serviceNotesTXT;

    //main
    @FXML private MFXTextField mainNumberTFX;

    //labour
    @FXML private MFXTextField customerTFX;
    @FXML private MFXTextField statusTFX;
    @FXML private MFXTextField numberTFX;

    @FXML private MFXTableView<WorkTable> repairTable;
    private final ObservableList<WorkTable> repairData = FXCollections.observableArrayList();

    @FXML private MFXTableView<PartTable> partsTable;
    private final ObservableList<PartTable> partsData = FXCollections.observableArrayList();

    @FXML private MFXListView<FilesHandler> filesList;
    private final ObservableList<FilesHandler> filesData = FXCollections.observableArrayList();

    private final ObservableList<String> techNames = FXCollections.observableArrayList();

    @FXML private MFXComboBox<String> techIdCombo;

    DatePicker picker;

    public static final ObservableList<String> WORK_ORDER_STATUSES = FXCollections.observableArrayList("New", "In Progress", "Waiting Parts", "Repair Complete", "Billing Complete", "Closed");

    //parts
    @FXML private MFXTextField partsCustomerTFX;
    @FXML private MFXTextField partsStatusTFX;
    @FXML private MFXTextField partsNumberTFX;

    public WorkOrder currentWorkOrder;
    public Customer currentCustomer;

    private DeletingFilesMethods deletingMethods;
    private DeletingPartsMethods deletingPartsMethods;
    private DeletingLabourMethods deletingLabourMethods;

    private boolean isDirty = false;
    private boolean isLoading = false;

    public void setMainController(ActualWorkshopController controller) {this.mainController = controller;}
    public void setDialogInstance(MFXGenericDialog dialogInstance) {this.dialogInstance = dialogInstance;}

    public void initialize(){
        tabPane.setFocusTraversable(false);

        statusCombo.setItems(WORK_ORDER_STATUSES);

        techNames.setAll(ViewControllerQueries.loadTechList());

        loadTechList();

        type.textProperty().addListener((obs, o, n) -> {
            if (!isLoading && !n.equals(o)) isDirty = true;
        });

        model.textProperty().addListener((obs, o, n) -> {
            if (!isLoading && !n.equals(o)) isDirty = true;
        });

        serialNumber.textProperty().addListener((obs, o, n) -> {
            if (!isLoading && !n.equals(o)) isDirty = true;
        });

        problemDesc.textProperty().addListener((obs, o, n) -> {
            if (!isLoading && !n.equals(o)) isDirty = true;
        });

        vendorId.textProperty().addListener((obs, o, n) -> {
            if (!isLoading && !n.equals(o)) isDirty = true;
        });

        warrantyNumber.textProperty().addListener((obs, o, n) -> {
            if (!isLoading && !n.equals(o)) isDirty = true;
        });

        serviceNotesTXT.textProperty().addListener((obs, o, n) -> {
            if (!isLoading && !n.equals(o)) isDirty = true;
        });

        techIdCombo.valueProperty().addListener((obs, o, n) -> {
            if (!isLoading && (o == null ? n != null : !o.equals(n))) {
                isDirty = true;
            }
        });

        statusCombo.valueProperty().addListener((obs, o, n) -> {
            if (!isLoading && (o == null ? n != null : !o.equals(n))) {
                isDirty = true;
            }
        });

        repairData.addListener((javafx.collections.ListChangeListener<WorkTable>) change -> {
            if (!isLoading) {
                isDirty = true;
            }
        });

        partsData.addListener((javafx.collections.ListChangeListener<PartTable>) change -> {
            if (!isLoading) {
                isDirty = true;
            }
        });

        techIdCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isLoading) return;
            if (newVal == null || newVal.isBlank()) return;
            if (isBillingComplete()) return;

            int techId = ViewControllerQueries.getTechIdByUsername(newVal);
            currentWorkOrder.setTechId(techId);

            if ("New".equalsIgnoreCase(currentWorkOrder.getStatus())) {
                currentWorkOrder.setStatus("In Progress");
                statusCombo.selectItem("In Progress");
            }
        });

        isLoading = true;

        repairTable.setFooterVisible(false);
        TableMethods.loadRepairsTable(repairTable, repairData, techNames);
        repairTable.setItems(repairData);

        partsTable.setFooterVisible(false);
        TableMethods.loadPartsTable(partsTable, partsData);
        partsTable.setItems(partsData);

        isLoading = false;
        isDirty = false;

        final double COLLAPSED_H = 60;
        final double EXPANDED_H  = 180;
        serviceNotesTXT.setPrefHeight(COLLAPSED_H);
        serviceNotesTXT.setTranslateY(0);

        serviceNotesTXT.focusedProperty().addListener((obs, was, focused) -> {
            if (focused) {
                serviceNotesTXT.setPrefHeight(EXPANDED_H);
                serviceNotesTXT.setTranslateY(-(EXPANDED_H - COLLAPSED_H));
                serviceNotesTXT.toFront();
            } else {
                serviceNotesTXT.setTranslateY(0);
                serviceNotesTXT.setPrefHeight(COLLAPSED_H);
            }
        });

        filesList.addEventFilter(MouseEvent.MOUSE_CLICKED, e ->{
            if(e.getClickCount() == 2){
                e.consume();
                System.out.println("double click");
                onOpenSelectedFile();
            }
        });

        statusCombo.valueProperty().addListener((obs, oldStatus, newStatus) -> {
            if (isLoading) return;
            if (newStatus == null || newStatus.equals(oldStatus)) return;

            if ("Repair Complete".equalsIgnoreCase(newStatus)) {
                saveRepairsToDb();
                if (!ViewControllerQueries.hasAtLeastOneLabourNoteDb(currentWorkOrder.getWorkorderNumber())) {
                    new Alert(Alert.AlertType.WARNING,
                            "Add at least one labour note (Tech + Description) before marking Repair Complete.",
                            ButtonType.OK
                    ).showAndWait();

                    isLoading = true;
                    statusCombo.selectItem(oldStatus);
                    isLoading = false;
                    return;
                }
            }

            currentWorkOrder.setStatus(newStatus);
        });

        deletingMethods = new DeletingFilesMethods(filesList);
        deletingMethods.setOnDelete(e -> {
            boolean deleted = deletingMethods.deleteSelectedFile();
            if (deleted) loadFilesFromDb();
        });

        deletingPartsMethods = new DeletingPartsMethods(partsTable);
        deletingPartsMethods.setOnDelete(e -> {
            if (isBillingComplete()) return;
            boolean removed = deletingPartsMethods.removeSelectedFromTable();
            if (removed) {
                savePartsToDb();
                isLoading = true;
                loadPartsFromDb();
                isLoading = false;
                isDirty = false;
            }
        });

        deletingLabourMethods = new DeletingLabourMethods(repairTable);
        deletingLabourMethods.setOnDelete(e -> {
            if (isBillingComplete()) return;
            boolean removed = deletingLabourMethods.removeSelectedFromTable();
            if (removed) {
                saveRepairsToDb();
                isLoading = true;
                loadRepairsFromDb();
                isLoading = false;
                isDirty = false;
            }
        });
    }

    private void updateStatusInDb(String newStatus) {
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

    public void initData(WorkOrder wo, Customer co){
        isLoading = true;

        this.currentWorkOrder = wo;
        this.currentCustomer = co;

        deletingMethods.setWorkorderNumber(currentWorkOrder.getWorkorderNumber());

        ViewControllerQueries.refreshWorkOrderFromDb(currentWorkOrder);
        loadTechList();

        if (wo.getTechId() > 0) {
            String techUsername = ViewControllerQueries.getTechUsernameById(wo.getTechId());
            techIdCombo.selectItem(techUsername);
        } else {
            techIdCombo.clearSelection();
            techIdCombo.setText("");
        }

        loadFilesFromDb();

        String firstName = co.getFirstName();
        String lastName = co.getLastName();
        String fullName = lastName + "," + firstName;

        statusCombo.selectItem(wo.getStatus());

        type.setText(wo.getType());
        model.setText(wo.getModel());
        serialNumber.setText(wo.getSerialNumber());
        problemDesc.setText(wo.getProblemDesc());

        vendorId.setText(wo.getVendorId());
        warrantyNumber.setText(wo.getWarrantyNumber());

        idTFX.setText(co.getId());
        firstNameTXF.setText(co.getFirstName());
        lastNameTXF.setText(co.getLastName());
        phoneTFX.setText(co.getPhone());
        addressTFX.setText(co.getAddress());
        townTFX.setText(co.getTown());
        zipTFX.setText(co.getPostalCode());

        mainNumberTFX.setText(String.valueOf(wo.getWorkorderNumber()));
        depositTXF.setText("$" + wo.getDepositAmount());

        customerTFX.setText(fullName);
        statusTFX.setText(wo.getStatus());
        numberTFX.setText(String.valueOf(wo.getWorkorderNumber()));

        loadServiceNotes();

        partsCustomerTFX.setText(fullName);
        partsStatusTFX.setText(wo.getStatus());
        partsNumberTFX.setText(String.valueOf(wo.getWorkorderNumber()));

        loadPartsFromDb();
        applyBillingLockUI();
        loadRepairsFromDb();

        Platform.runLater(() -> {
            isLoading = false;
            isDirty = false;
        });
    }

    public void insertNotes(TextArea area){
        String stamp = "[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "]: ";
        String text = area.getText();
        int caret = area.getCaretPosition();

        if (caret > 0 && text.charAt(caret - 1) != '\n') {
            stamp = "\n" + stamp;
        }
        area.insertText(caret, stamp);
        area.positionCaret(caret + stamp.length());
    }

    private void loadTechList() {
        techNames.clear();
        techNames.addAll(ViewControllerQueries.loadTechList());
        techIdCombo.setItems(techNames);
    }

    private void applyBillingLockUI() {
        boolean locked = isBillingComplete();

        statusCombo.setDisable(locked);

        type.setDisable(locked);
        model.setDisable(locked);
        serialNumber.setDisable(locked);
        problemDesc.setDisable(locked);
        vendorId.setDisable(locked);
        warrantyNumber.setDisable(locked);

        partsTable.setDisable(locked);
        repairTable.setDisable(locked);

        serviceNotesTXT.setDisable(false);
    }

    private void attachRepairListeners(WorkTable r) {
        r.techProperty().addListener((obs, o, n) -> {
            if (!isLoading && (o == null ? n != null : !o.equals(n))) isDirty = true;
        });

        r.descriptionProperty().addListener((obs, o, n) -> {
            if (!isLoading && (o == null ? n != null : !o.equals(n))) isDirty = true;
        });

        r.priceProperty().addListener((obs, o, n) -> {
            if (!isLoading && !n.equals(o)) isDirty = true;
        });

        r.dateProperty().addListener((obs, o, n) -> {
            if (!isLoading && (o == null ? n != null : !o.equals(n))) isDirty = true;
        });
    }

    private void attachPartListeners(PartTable p) {
        p.nameProperty().addListener((obs, o, n) -> {
            if (!isLoading && (o == null ? n != null : !o.equals(n))) isDirty = true;
        });

        p.quantityProperty().addListener((obs, o, n) -> {
            if (!isLoading && !n.equals(o)) isDirty = true;
        });

        p.priceProperty().addListener((obs, o, n) -> {
            if (!isLoading && !n.equals(o)) isDirty = true;
        });

        p.totalPriceProperty().addListener((obs, o, n) -> {
            if (!isLoading && !n.equals(o)) isDirty = true;
        });
    }

    public void loadServiceNotes(){
        String notes = ViewControllerQueries.loadServiceNotes(currentWorkOrder.getWorkorderNumber());
        if (notes != null) {
            serviceNotesTXT.setText(notes);
        } else {
            serviceNotesTXT.clear();
        }
    }

    @FXML
    public void onAddStep() {
        if (isBillingComplete()) return;
        WorkTable row = new WorkTable(LocalDate.now(), "", "", 0.0);
        attachRepairListeners(row);
        repairData.add(row);
    }

    @FXML
    public void addPart(){
        if (isBillingComplete()) return;
        PartTable row = new PartTable("",0,0.0,0);
        attachPartListeners(row);
        partsData.add(row);
    }

    @FXML
    public void repairComplete() {
        repairTable.requestFocus();
        tabPane.requestFocus();
        saveRepairsToDb();

        if (!ViewControllerQueries.hasAtLeastOneLabourNoteDb(currentWorkOrder.getWorkorderNumber())) {
            new Alert(Alert.AlertType.WARNING, "Add at least one labour note (Tech + Description) before marking Repair Complete.", ButtonType.OK).showAndWait();
            return;
        }

        updateStatusInDb("Repair Complete");
        statusCombo.selectItem("Repair Complete");
    }

    private void loadRepairsFromDb() {
        repairData.clear();
        for (WorkTable row : ViewControllerQueries.loadRepairsFromDb(currentWorkOrder.getWorkorderNumber())) {
            attachRepairListeners(row);
            repairData.add(row);
        }
    }

    private void saveRepairsToDb() {
        ViewControllerQueries.saveRepairsToDb(currentWorkOrder.getWorkorderNumber(), repairData);
    }

    @FXML
    public void Pay() throws Exception {
        ViewControllerQueries.refreshWorkOrderFromDb(currentWorkOrder);

        String st = currentWorkOrder.getStatus();
        if (st == null || !st.equalsIgnoreCase("Repair Complete")) {
            new Alert(Alert.AlertType.WARNING,
                    "You must set the work order status to 'Repair Complete' before taking final payment.",
                    ButtonType.OK
            ).showAndWait();
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
        pc.setContext(wo, co, invoiceType, owner);

        if (invoiceType == InvoiceType.FINAL) {
            savePartsToDb();
            saveRepairsToDb();

            isLoading = true;
            loadPartsFromDb();
            loadRepairsFromDb();
            isLoading = false;
            isDirty = false;

            pc.setSuggestedAmount(finalDueDb());
        }

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle("Payment");
        stage.setScene(new Scene(root));
        stage.showAndWait();
        loadFilesFromDb();
    }

    @FXML
    public void updateOrder() {
        int woNumber = currentWorkOrder.getWorkorderNumber();
        String newServiceNotes = serviceNotesTXT.getText();

        if (isBillingComplete()) {
            ViewControllerQueries.updateServiceNotesInDb(woNumber, newServiceNotes);
            isDirty = false;
            System.out.println("updated service notes (Billing Complete)");
            return;
        }

        String newType = type.getText();
        String newModel = model.getText();
        String newSerialNumber = serialNumber.getText();
        String newProblemDesc = problemDesc.getText();
        String newVendorId = vendorId.getText();
        String newWarrantyNumber = warrantyNumber.getText();

        int techId = currentWorkOrder.getTechId();
        String status = currentWorkOrder.getStatus();

        if ("Repair Complete".equalsIgnoreCase(status)) {
            if (techId <= 0) {
                new Alert(Alert.AlertType.WARNING,
                        "Please select a technician before marking Repair Complete.",
                        ButtonType.OK
                ).showAndWait();
                return;
            }

            saveRepairsToDb();

            if (!ViewControllerQueries.hasAtLeastOneLabourNoteDb(woNumber)) {
                new Alert(Alert.AlertType.WARNING,
                        "Add at least one labour note (Tech + Description) before marking Repair Complete.",
                        ButtonType.OK
                ).showAndWait();
                return;
            }
        }

        ViewControllerQueries.updateOrderInDb(woNumber, newType, newModel, newSerialNumber,
                newProblemDesc, newVendorId, newWarrantyNumber, newServiceNotes, techId, status);

        savePartsToDb();
        saveRepairsToDb();

        mainController.LoadOrders();

        System.out.println("updated order (FULL SAVE)");
        isDirty = false;
    }

    @FXML
    public void addFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Files");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
        File file = fc.showOpenDialog(dialogInstance.getScene().getWindow());
        if (file == null) return;

        try {
            ViewControllerQueries.addFileToDb(currentWorkOrder.getWorkorderNumber(), file);
            loadFilesFromDb();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onOpenSelectedFile() {
        FilesHandler selected = filesList.getSelectionModel().getSelectedValue();
        if (selected == null) return;
        openFileFromDb(selected.getId());
    }

    public void openFileFromDb(int fileId){
        try {
            Object[] result = ViewControllerQueries.openFileFromDb(fileId);
            if (result == null) return;

            String name = (String) result[0];
            InputStream in = (InputStream) result[1];

            String ext = "";
            int dot = name.lastIndexOf('.');
            if (dot > -1) ext = name.substring(dot);

            File tmp = File.createTempFile("wo_", ext);
            tmp.deleteOnExit();

            Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            in.close();

            openWithDesktop(tmp);
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

    private void savePartsToDb() {
        ViewControllerQueries.savePartsToDb(currentWorkOrder.getWorkorderNumber(), partsData);
    }

    private void loadPartsFromDb() {
        partsData.clear();
        for (PartTable row : ViewControllerQueries.loadPartsFromDb(currentWorkOrder.getWorkorderNumber())) {
            attachPartListeners(row);
            partsData.add(row);
        }
    }

    @FXML
    public void printOrder() throws Exception {
        WorkOrder wo = currentWorkOrder;
        Customer co = currentCustomer;
        Window owner = dialogInstance.getScene().getWindow();
        DocumentOutput.printOrPdf(
                "WO_" + wo.getWorkorderNumber(),
                "/main/printOrder.fxml",
                loader -> {
                    PrinterController pc = loader.getController();
                    pc.initData(wo, co);},
                owner
        );
    }

    public void openWithDesktop(File file){
        Thread thread = new Thread(){
            @Override
            public void run(){
                try{
                    Desktop.getDesktop().open(file);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    private double finalDueDb() {
        int woNumber = currentWorkOrder.getWorkorderNumber();
        double labour = ViewControllerQueries.labourTotalDb(woNumber);
        double parts = ViewControllerQueries.partsTotalDb(woNumber);
        double deposit = ViewControllerQueries.depositFromDb(woNumber);
        return Math.max(0, labour + parts - deposit);
    }

    private boolean isBillingComplete() {
        String s = currentWorkOrder.getStatus();
        if (s == null) return false;
        return s.equalsIgnoreCase("Billing Complete");
    }

    @FXML
    public void closeDialog() {
        if (isDirty) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("You have unsaved changes.");
            alert.setContentText("Do you want to save before closing?");

            ButtonType saveBtn = new ButtonType("Yes (Save)");
            ButtonType discardBtn = new ButtonType("No (Discard)");
            ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(saveBtn, discardBtn, cancelBtn);

            alert.showAndWait().ifPresent(response -> {
                if (response == saveBtn) {
                    updateOrder();
                    actuallyCloseDialog();
                }
                else if (response == discardBtn) {
                    actuallyCloseDialog();
                }
            });

        } else {
            actuallyCloseDialog();
        }
    }

    private void actuallyCloseDialog() {
        mainController.rootStack.getChildren().remove(dialogInstance);
        mainController.contentPane.setEffect(null);
        mainController.contentPane.setDisable(false);
    }
}