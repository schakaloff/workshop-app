package Controllers;

import DB.DbConfig;
import Skeletons.*;
import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;

import javafx.application.HostServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import print.Print;
import utils.DeletingMethods;
import utils.TableMethods;


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


    DatePicker picker;

    public static final ObservableList<String> WORK_ORDER_STATUSES = FXCollections.observableArrayList("New", "In Progress", "Waiting Parts", "Repair Complete", "Closed");

    //parts
    @FXML private MFXTextField partsCustomerTFX;
    @FXML private MFXTextField partsStatusTFX;
    @FXML private MFXTextField partsNumberTFX;

    public static WorkOrder currentWorkOrder;
    public Customer currentCustomer;

    private DeletingMethods deletingMethods;

    public void setMainController(ActualWorkshopController controller) {this.mainController = controller;}
    public void setDialogInstance(MFXGenericDialog dialogInstance) {this.dialogInstance = dialogInstance;}

    public void initialize(){
        tabPane.setFocusTraversable(false);


        statusCombo.setItems(WORK_ORDER_STATUSES);

        techNames.setAll(TableMethods.loadTechnicianUsernames());

        repairTable.setFooterVisible(false);
        TableMethods.loadRepairsTable(repairTable, repairData, techNames);
        repairTable.setItems(repairData);

        partsTable.setFooterVisible(false);
        TableMethods.loadPartsTable(partsTable, partsData);
        partsTable.setItems(partsData);

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
            if (newStatus == null || newStatus.equals(oldStatus)) return;

            updateStatusInDb(newStatus);
        });

        deletingMethods = new DeletingMethods(filesList);
        deletingMethods.setOnDelete(e -> {
            boolean deleted = deletingMethods.deleteSelectedFile();
            if (deleted)loadFilesFromDb();
        });
    }

    private void updateStatusInDb(String newStatus) {
        String sql = "UPDATE work_order SET status = ? WHERE workorder = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setInt(2, currentWorkOrder.getWorkorderNumber());
            ps.executeUpdate();

            currentWorkOrder.setStatus(newStatus);

            // update read-only fields
            statusTFX.setText(newStatus);
            partsStatusTFX.setText(newStatus);

            mainController.LoadOrders(); // refresh table + colors + counters

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void initData(WorkOrder wo, Customer co){
        this.currentWorkOrder = wo;
        this.currentCustomer = co;
        refreshWorkOrderFromDb();
        loadFilesFromDb();

        String firstName = co.getFirstName();
        String lastName = co.getLastName();
        String fullName = lastName + "," + firstName;

        //main tab
        //status.setText(wo.getStatus());

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

        mainNumberTFX.setText((String.valueOf(wo.getWorkorderNumber())));
        depositTXF.setText("$"+String.valueOf(wo.getDepositAmount()));

        //labour tab
        customerTFX.setText(fullName);
        statusTFX.setText(wo.getStatus());
        numberTFX.setText((String.valueOf(wo.getWorkorderNumber())));
        loadServiceNotes();
        //TableMethods.loadRepairsTable(repairTable, repairData, techNames);

        //parts tab
        partsCustomerTFX.setText(fullName);
        partsStatusTFX.setText(wo.getStatus());
        partsNumberTFX.setText(String.valueOf(wo.getWorkorderNumber()));
        //TableMethods.loadPartsTable(partsTable, partsData);
        loadPartsFromDb();

        //repair
        loadRepairsFromDb();
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

    public void refreshWorkOrderFromDb() {
        String sql = "SELECT status, type, model, serialNumber, problemDesc, vendorId, warrantyNumber FROM work_order WHERE workorder = ?";
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
            }
        } catch (SQLException e) {
            System.out.println("issue during refreshing work order");
        }
    }

    @FXML
    public void Pay() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/pay.fxml"));
        MFXGenericDialog dialog = loader.load();
        Stage dialogStage = new Stage();
        /*
        we are telling javafx that new stage should be modal
        It will prevent user from interacting with other windows.
        Modality.APPLICATION blocks mouse and keyboard input to all other windows in this app.
         */
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Payment");
        Scene scene = new Scene(dialog);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
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
        repairData.add(new WorkTable(LocalDate.now(), "", "", 0.0));
    }

    @FXML
    public void addPart(){
        partsData.add(new PartTable("",0,0.0,0));
    }

    @FXML
    public void repairComplete(){
        String sql = "UPDATE work_order SET status = ? where workorder = ?";
        try{
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement ps = conn.prepareStatement(sql);
            statusCombo.selectItem("Repair Complete");
            ps.setInt(2, currentWorkOrder.getWorkorderNumber());
            int rs = ps.executeUpdate();
            if(rs > 0){
                currentWorkOrder.setStatus("Repair Complete");
                System.out.println("order is closed");
                mainController.LoadOrders();
            }
        }catch (SQLException e){
            System.out.println("error during closing the order");
        }
        statusCombo.setText(currentWorkOrder.getStatus());
    }

    private void loadRepairsFromDb() {
        repairData.clear();
        String sql = "SELECT repair_date, tech, description, price FROM work_order_repairs WHERE workorder_id = ?";
        try{
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentWorkOrder.getWorkorderNumber());
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                LocalDate date       = rs.getDate("repair_date").toLocalDate();
                String    tech       = rs.getString("tech");
                String    desc       = rs.getString("description");
                double    price      = rs.getDouble("price");
                repairData.add(new WorkTable(date, tech, desc, price));
            }
            rs.close();
            ps.close();
            conn.close();
        } catch (SQLException e){
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

    @FXML
    public void updateOrder(){
        String newType = type.getText();
        String newModel = model.getText();
        String newSerialNumber = serialNumber.getText();
        String newProblemDesc = problemDesc.getText();
        String newVendorId = vendorId.getText();
        String newWarrantyNumber = warrantyNumber.getText();
        String newServiceNotes = serviceNotesTXT.getText();
        int woNumber = currentWorkOrder.getWorkorderNumber();

        String newSQL = "update work_order set type = ?, model = ?, serialNumber = ?, problemDesc = ?, vendorId = ?, warrantyNumber = ?, service_notes = ? WHERE workorder = ?";
        try{
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement ps = conn.prepareStatement(newSQL);
            ps.setString(1, newType);
            ps.setString(2, newModel);
            ps.setString(3, newSerialNumber);
            ps.setString(4, newProblemDesc);
            ps.setString(5, newVendorId);
            ps.setString(6, newWarrantyNumber);
            ps.setString(7, newServiceNotes);
             ps.setInt(8, woNumber);
            int updated = ps.executeUpdate();
            ps.close();
            conn.close();
        }catch (SQLException e){
            System.out.println("error during updating order");
        }
        System.out.println("updated order");
        mainController.LoadOrders();

        savePartsToDb();
        saveRepairsToDb();
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
            ps.setBinaryStream(3, fis, (long) file.length()); // send bytes

            ps.executeUpdate();

            fis.close();
            ps.close();
            conn.close();

            loadFilesFromDb(); // refresh list
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

    public void initFilesUI(){
        filesList.setItems(filesData);
        loadFilesFromDb();
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
        String sql = "SELECT part_name, quantity, price, total_price FROM work_order_parts WHERE workorder_id = ?";
        try{
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentWorkOrder.getWorkorderNumber());
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                String name = rs.getString("part_name");
                int qty = rs.getInt("quantity");
                double price = rs.getDouble("price");
                double total = rs.getDouble("total_price");
                partsData.add(new PartTable(name, qty, price, total));
            }
            rs.close();
            ps.close();
            conn.close();
        }catch (SQLException e){
            System.out.println("error during loading parts");
        }
    }

    @FXML
    public void printOrder() throws Exception { //print function
        WorkOrder wo = currentWorkOrder;
        Customer co = currentCustomer;
        Window owner = dialogInstance.getScene().getWindow();
        Print.printWorkOrder(wo, co, owner);
    }

    @FXML
    public void closeDialog(){
        mainController.rootStack.getChildren().remove(dialogInstance);
        mainController.contentPane.setEffect(null);
        mainController.contentPane.setDisable(false);
    }
}