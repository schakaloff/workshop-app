package Controllers;
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
import java.sql.*;
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

    //@FXML private MFXTextField status;
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

        techNames.setAll(TableMethods.loadTechnicianUsernames());

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


// 🔥 COMBO BOXES
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


// 🔥 REPAIR TABLE (list changes: add/remove)
        repairData.addListener((javafx.collections.ListChangeListener<WorkTable>) change -> {
            if (!isLoading) {
                isDirty = true;
            }
        });

// 🔥 PARTS TABLE (list changes: add/remove)
        partsData.addListener((javafx.collections.ListChangeListener<PartTable>) change -> {
            if (!isLoading) {
                isDirty = true;
            }
        });

        techIdCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isLoading) return;
            if (newVal == null || newVal.isBlank()) return;
            if (isBillingComplete()) return;

            int techId = getTechIdByUsername(newVal);
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

//        serviceNotesTXT.addEventFilter(MouseEvent.MOUSE_CLICKED, e->{
//            if(e.getButton() == MouseButton.PRIMARY){
//                insertNotes(serviceNotesTXT);
//                e.consume();
//            }
//        });

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
                if (!hasAtLeastOneLabourNoteDb()) {
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
            if (deleted)loadFilesFromDb();
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
        String sql = """
        UPDATE work_order
        SET status = ?,
            finished_at = CASE
                WHEN ? IN ('Repair Complete', 'Billing Complete') AND finished_at IS NULL THEN NOW()
                ELSE finished_at
            END,
            labour_amount = ?
        WHERE workorder = ?
    """;

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setString(2, newStatus);
            ps.setDouble(3, labourTotalDb());
            ps.setInt(4, currentWorkOrder.getWorkorderNumber());

            ps.executeUpdate();

            currentWorkOrder.setStatus(newStatus);

            statusTFX.setText(newStatus);
            partsStatusTFX.setText(newStatus);

            mainController.LoadOrders();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        applyBillingLockUI();
    }

    public void initData(WorkOrder wo, Customer co){
        isLoading = true; // 🔥 START LOADING

        this.currentWorkOrder = wo;
        this.currentCustomer = co;

        deletingMethods.setWorkorderNumber(currentWorkOrder.getWorkorderNumber());

        refreshWorkOrderFromDb();
        loadTechList();

        if (wo.getTechId() > 0) {
            String techUsername = getTechUsernameById(wo.getTechId());
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

    public void insertNotes(TextArea area){  //service notes function
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

        String sql = "SELECT username FROM technician";
        try {
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                techNames.add(rs.getString("username"));
            }

            rs.close();
            ps.close();
            conn.close();

        } catch (SQLException e) {
            System.out.println("error during loading tech list");
        }

        techIdCombo.setItems(techNames);
    }

    private int getTechIdByUsername(String username) {
        int id = 0;
        String sql = "SELECT id FROM technician WHERE username = ?";

        try {
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                id = rs.getInt("id");
            }

            rs.close();
            ps.close();
            conn.close();

        } catch (SQLException e) {
            System.out.println("error during loading tech id");
        }

        return id;
    }

    private String getTechUsernameById(int techId) {
        String username = "";
        String sql = "SELECT username FROM technician WHERE id = ?";

        try {
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, techId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                username = rs.getString("username");
            }

            rs.close();
            ps.close();
            conn.close();

        } catch (SQLException e) {
            System.out.println("error during loading tech username");
        }

        return username;
    }

    private void updateTechIdInDb(int techId) {
        String sql = "UPDATE work_order SET tech_id = ? WHERE workorder = ?";
        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, techId);
            ps.setInt(2, currentWorkOrder.getWorkorderNumber());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void refreshWorkOrderFromDb() {
        String sql = "SELECT status, type, model, serialNumber, problemDesc, vendorId, warrantyNumber, tech_id FROM work_order WHERE workorder = ?";
        try {
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement ps = conn.prepareStatement(sql);
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

                int techId = rs.getInt("tech_id");
                if (rs.wasNull()) techId = 0;
                currentWorkOrder.setTechId(techId);
            }
        } catch (SQLException e) {
            System.out.println("issue during refreshing work order");
        }
    }


    @FXML
    public void Pay() throws Exception {
        refreshWorkOrderFromDb();

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

//        if (invoiceType == InvoiceType.FINAL) {
//            savePartsToDb();
//            saveRepairsToDb();
//            loadPartsFromDb();
//            loadRepairsFromDb();
//            pc.setSuggestedAmount(finalDueDb());
//        }

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

    private void applyBillingLockUI() {
        boolean locked = isBillingComplete();

        // status changes should be blocked
        statusCombo.setDisable(locked);

        // main fields should be blocked
        type.setDisable(locked);
        model.setDisable(locked);
        serialNumber.setDisable(locked);
        problemDesc.setDisable(locked);
        vendorId.setDisable(locked);
        warrantyNumber.setDisable(locked);

        // parts + labour tables should be blocked
        partsTable.setDisable(locked);
        repairTable.setDisable(locked);

        // files should be blocked
        //filesList.setDisable(locked);

        // but service notes must still be editable
        serviceNotesTXT.setDisable(false);
    }

    private void attachRepairListeners(WorkTable r) {

        r.techProperty().addListener((obs, o, n) -> {
            if (!isLoading && (o == null ? n != null : !o.equals(n))) {
                isDirty = true;
            }
        });

        r.descriptionProperty().addListener((obs, o, n) -> {
            if (!isLoading && (o == null ? n != null : !o.equals(n))) {
                isDirty = true;
            }
        });

        r.priceProperty().addListener((obs, o, n) -> {
            if (!isLoading && !n.equals(o)) {
                isDirty = true;
            }
        });

        r.dateProperty().addListener((obs, o, n) -> {
            if (!isLoading && (o == null ? n != null : !o.equals(n))) {
                isDirty = true;
            }
        });
    }

    private void attachPartListeners(PartTable p) {

        p.nameProperty().addListener((obs, o, n) -> {
            if (!isLoading && (o == null ? n != null : !o.equals(n))) {
                isDirty = true;
            }
        });

        p.quantityProperty().addListener((obs, o, n) -> {
            if (!isLoading && !n.equals(o)) {
                isDirty = true;
            }
        });

        p.priceProperty().addListener((obs, o, n) -> {
            if (!isLoading && !n.equals(o)) {
                isDirty = true;
            }
        });

        p.totalPriceProperty().addListener((obs, o, n) -> {
            if (!isLoading && !n.equals(o)) {
                isDirty = true;
            }
        });
    }


    public void loadServiceNotes(){
        String sql = "SELECT service_notes FROM work_order WHERE workorder = ?";
        try{
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentWorkOrder.getWorkorderNumber());
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                serviceNotesTXT.setText(rs.getString("service_notes"));
            }else{
                serviceNotesTXT.clear();
            }
        }catch (SQLException e){
            System.out.println("issue during loading service notes");
        }
    }
    @FXML
    public void onAddStep() {
        if (isBillingComplete()) return;
        //repairData.add(new WorkTable(LocalDate.now(), "", "", 0.0));
        WorkTable row = new WorkTable(LocalDate.now(), "", "", 0.0);
        attachRepairListeners(row);   // 🔥 IMPORTANT
        repairData.add(row);
    }

    @FXML
    public void addPart(){
        if (isBillingComplete()) return;
        //partsData.add(new PartTable("",0,0.0,0));
        PartTable row = new PartTable("",0,0.0,0);
        attachPartListeners(row);
        partsData.add(row);
    }

    @FXML
    public void repairComplete() {
        repairTable.requestFocus();
        tabPane.requestFocus();
        saveRepairsToDb();

        if (!hasAtLeastOneLabourNoteDb()) {
            new Alert(Alert.AlertType.WARNING, "Add at least one labour note (Tech + Description) before marking Repair Complete.", ButtonType.OK).showAndWait();
            return;
        }

        updateStatusInDb("Repair Complete");
        statusCombo.selectItem("Repair Complete");
    }

    private boolean hasAtLeastOneLabourNoteDb() {
        boolean ok = false;
        String sql = "SELECT tech, description FROM work_order_repairs WHERE workorder_id = ?";
        try {
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentWorkOrder.getWorkorderNumber());

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String tech = rs.getString("tech");
                String desc = rs.getString("description");

                if (tech != null && !tech.equals("") && desc != null && !desc.equals("")) {
                    ok = true;
                    break;
                }
            }
            rs.close();
            ps.close();
            conn.close();

        } catch (SQLException e) {
            System.out.println("error during checking labour notes");
        }
        return ok;
    }

    private void loadRepairsFromDb() {
        repairData.clear();

        String sql = "SELECT repair_date, tech, description, price FROM work_order_repairs WHERE workorder_id = ?";
        try {
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentWorkOrder.getWorkorderNumber());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                LocalDate date = rs.getDate("repair_date").toLocalDate();
                String tech = rs.getString("tech");
                String desc = rs.getString("description");
                double price = rs.getDouble("price");

                WorkTable row = new WorkTable(date, tech, desc, price);
                attachRepairListeners(row);
                repairData.add(row);
            }

            rs.close();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println("error during loading repairs");
        }
    }

    private void saveRepairsToDb() {
        String deleteSQL = "DELETE FROM work_order_repairs WHERE workorder_id = ?";
        String insertSQL = "INSERT INTO work_order_repairs (workorder_id, repair_date, tech, description, price) VALUES (?, ?, ?, ?, ?)";
        try{
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement del = conn.prepareStatement(deleteSQL);
            del.setInt(1, currentWorkOrder.getWorkorderNumber());
            del.executeUpdate();
            del.close();

            PreparedStatement ps = conn.prepareStatement(insertSQL);
            for (WorkTable r : repairData) {
                if (r.getTech() == null || r.getTech().isBlank()) {
                    continue;
                }
                ps.setInt(1, currentWorkOrder.getWorkorderNumber());
                ps.setDate(2, java.sql.Date.valueOf(r.getDate()));
                ps.setString(3, r.getTech());
                ps.setString(4, r.getDescription());
                ps.setDouble(5, r.getPrice());
                ps.addBatch();
            }
            ps.executeBatch();
            ps.close();
            conn.close();
        } catch (SQLException e){
            System.out.println("error during saving repairs");
        }
    }

    private double depositFromDb() {
        double deposit = 0.0;
        String sql = "SELECT deposit_amount FROM work_order WHERE workorder = ?";
        try {
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentWorkOrder.getWorkorderNumber());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                deposit = rs.getDouble("deposit_amount");
            }
            rs.close();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println("error during loading deposit");
        }

        return deposit;
    }

    private double labourTotalDb() {
        double total = 0.0;
        String sql = "SELECT SUM(price) FROM work_order_repairs WHERE workorder_id = ?";
        try {
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentWorkOrder.getWorkorderNumber());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                total = rs.getDouble(1);
            }
            rs.close();
            ps.close();
            conn.close();

        } catch (SQLException e) {
            System.out.println("error during loading labour total");
        }

        return total;
    }

    private double partsTotalDb() {
        double total = 0.0;
        String sql = "SELECT SUM(total_price) FROM work_order_parts WHERE workorder_id = ?";
        try {
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentWorkOrder.getWorkorderNumber());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                total = rs.getDouble(1);
            }
            rs.close();
            ps.close();
            conn.close();

        } catch (SQLException e) {
            System.out.println("error during loading parts total");
        }

        return total;
    }

    @FXML
    public void updateOrder() {
        int woNumber = currentWorkOrder.getWorkorderNumber();

        String newServiceNotes = serviceNotesTXT.getText();

        // If Billing Complete → only allow notes
        if (isBillingComplete()) {
            String sql = "UPDATE work_order SET service_notes = ? WHERE workorder = ?";
            try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, newServiceNotes);
                ps.setInt(2, woNumber);
                ps.executeUpdate();

            } catch (SQLException e) {
                System.out.println("error during updating service notes");
                e.printStackTrace();
            }

            isDirty = false;
            System.out.println("updated service notes (Billing Complete)");
            return;
        }

        // 🔹 Read values from UI
        String newType = type.getText();
        String newModel = model.getText();
        String newSerialNumber = serialNumber.getText();
        String newProblemDesc = problemDesc.getText();
        String newVendorId = vendorId.getText();
        String newWarrantyNumber = warrantyNumber.getText();

        int techId = currentWorkOrder.getTechId();
        String status = currentWorkOrder.getStatus();

        // 🔥 Validation BEFORE saving
        if ("Repair Complete".equalsIgnoreCase(status)) {

            if (techId <= 0) {
                new Alert(Alert.AlertType.WARNING,
                        "Please select a technician before marking Repair Complete.",
                        ButtonType.OK
                ).showAndWait();
                return;
            }

            saveRepairsToDb();

            if (!hasAtLeastOneLabourNoteDb()) {
                new Alert(Alert.AlertType.WARNING,
                        "Add at least one labour note (Tech + Description) before marking Repair Complete.",
                        ButtonType.OK
                ).showAndWait();
                return;
            }
        }

        // 🔥 MAIN UPDATE (everything saved here)
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
            status = ?
        WHERE workorder = ?
    """;

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newType);
            ps.setString(2, newModel);
            ps.setString(3, newSerialNumber);
            ps.setString(4, newProblemDesc);
            ps.setString(5, newVendorId);
            ps.setString(6, newWarrantyNumber);
            ps.setString(7, newServiceNotes);

            ps.setInt(8, techId);                 // 🔥 technician
            ps.setString(9, status);              // 🔥 status

            ps.setInt(10, woNumber);

            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println("error during updating order");
            e.printStackTrace();
        }

        // 🔹 Save tables AFTER main update
        savePartsToDb();
        saveRepairsToDb();

        // 🔹 Refresh UI
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

        String sql = "INSERT INTO work_order_files (workorder_id, file_name, file_data) VALUES (?, ?, ?)";
        try {
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement ps = conn.prepareStatement(sql);
            FileInputStream fis = new FileInputStream(file);

            ps.setInt(1, currentWorkOrder.getWorkorderNumber());
            ps.setString(2, file.getName());
            ps.setBinaryStream(3, fis, (long) file.length());

            ps.executeUpdate();

            fis.close();
            ps.close();
            conn.close();

            loadFilesFromDb();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onOpenSelectedFile() {
        FilesHandler selected = filesList.getSelectionModel().getSelectedValue();
        if(selected == null)return;
        openFileFromDb(selected.getId());
    }

    public void openFileFromDb(int fileId){
        String sql = "SELECT file_name, file_data FROM work_order_files WHERE id = ?";
        try {
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, fileId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String name = rs.getString("file_name");
                InputStream in = rs.getBinaryStream("file_data");

                String ext = "";
                int dot = name.lastIndexOf('.');
                if (dot > -1){
                    ext = name.substring(dot);
                }
                File tmp = File.createTempFile("wo_", ext);
                tmp.deleteOnExit();

                Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                in.close();

                openWithDesktop(tmp);
            }
            rs.close();
            ps.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadFilesFromDb() {
        filesList.getItems().clear();
        String sql = "SELECT id, file_name FROM work_order_files WHERE workorder_id = ?";
        try {
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentWorkOrder.getWorkorderNumber()); // make sure this is set!
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                filesList.getItems().add(new FilesHandler(rs.getInt("id"), rs.getString("file_name")));
            }
            rs.close(); ps.close(); conn.close();
        } catch (SQLException e) {
            System.out.println("issue during loading files");
        }
    }

    private void savePartsToDb() {
        String deleteSQL = "DELETE FROM work_order_parts WHERE workorder_id = ?";
        String insertSQL = "INSERT INTO work_order_parts (workorder_id, part_name, quantity, price, total_price) VALUES (?, ?, ?, ?, ?)";
        try{
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement del = conn.prepareStatement(deleteSQL);
            del.setInt(1, currentWorkOrder.getWorkorderNumber());
            del.executeUpdate();
            del.close();
            PreparedStatement ps = conn.prepareStatement(insertSQL);
            for(PartTable part : partsData) {
                if (part.getName() == null || part.getName().isBlank()) {
                    continue;
                }
                ps.setInt(1, currentWorkOrder.getWorkorderNumber());
                ps.setString(2, part.getName());
                ps.setInt(3, part.getQuantity());
                ps.setDouble(4, part.getPrice());
                ps.setDouble(5, part.getTotalPrice());
                ps.addBatch();
            }
            ps.executeBatch();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println("error during saving parts");
        }
    }

    private void loadPartsFromDb() {
        partsData.clear();

        String sql = "SELECT id, part_name, quantity, price, total_price FROM work_order_parts WHERE workorder_id = ?";
        try {
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentWorkOrder.getWorkorderNumber());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("part_name");
                int qty = rs.getInt("quantity");
                double price = rs.getDouble("price");
                double total = rs.getDouble("total_price");

                PartTable row = new PartTable(id, name, qty, price, total);
                attachPartListeners(row);
                partsData.add(row);
            }

            rs.close();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println("error during loading parts");
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

//    public void initFilesUI(){
//        filesList.setItems(filesData);
//        loadFilesFromDb();
//    }

    private double finalDueDb() {
        double labour = labourTotalDb();
        double parts = partsTotalDb();
        double deposit = depositFromDb();
        double due = labour + parts - deposit;
        return Math.max(0, due);
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
                    updateOrder(); // ✅ SAVE
                    actuallyCloseDialog();
                }
                else if (response == discardBtn) {
                    actuallyCloseDialog(); // ❌ DON'T SAVE
                }
                // Cancel → do nothing
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