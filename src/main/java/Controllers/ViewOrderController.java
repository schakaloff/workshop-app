package Controllers;

import DB.DbConfig;
import Skeletons.Customer;
import Skeletons.WorkOrder;
import Skeletons.WorkTable;
import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import print.Print;


import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ViewOrderController {
    @FXML private ActualWorkshopController mainController;
    @FXML private MFXGenericDialog dialogInstance;

    @FXML private MFXComboBox<String> vendorId;
    @FXML private MFXTextField warrantyNumber;

    @FXML private MFXTextField status;
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

    @FXML private MFXTableView<WorkTable> table;
    private final ObservableList<WorkTable> data = FXCollections.observableArrayList();

    //parts
    @FXML private MFXTextField partsCustomerTFX;
    @FXML private MFXTextField partsStatusTFX;
    @FXML private MFXTextField partsNumberTFX;

    public WorkOrder currentWorkOrder;
    public Customer currentCustomer;

    public void setMainController(ActualWorkshopController controller) {this.mainController = controller;}
    public void setDialogInstance(MFXGenericDialog dialogInstance) {this.dialogInstance = dialogInstance;}

    public void initialize(){
        tabPane.setFocusTraversable(false);
        table.setFooterVisible(false);
        loadRepairTable();
        table.setItems(data);

    }

    public void loadRepairTable() {
        table.getTableColumns().clear();
        data.clear();

        MFXTableColumn<WorkTable> dateCol = new MFXTableColumn<>("Date");
        dateCol.setPrefWidth(150);
        dateCol.setRowCellFactory(item -> {
            MFXTableRowCell<WorkTable, LocalDate> cell = new MFXTableRowCell<>(WorkTable::getDate);
            DatePicker picker = new DatePicker();
            cell.setGraphic(picker);
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            return cell;
        });

        MFXTableColumn<WorkTable> techCol = new MFXTableColumn<>("Tech");
        techCol.setRowCellFactory(item->{
            MFXTableRowCell<WorkTable,String> cell = new MFXTableRowCell<>(WorkTable::getTech);
            ComboBox box = new ComboBox();
            cell.setGraphic(box);
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            return cell;
        });

        MFXTableColumn<WorkTable> descCol = new MFXTableColumn<>("Description" );
        descCol.setPrefWidth(300);
        descCol.setRowCellFactory(item->{
            MFXTableRowCell<WorkTable, String> cell = new MFXTableRowCell<>(WorkTable::getDescription);
            TextArea area = new TextArea();
            area.setWrapText(true);
            cell.setGraphic(area);
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            return cell;
        });

        MFXTableColumn<WorkTable> priceCol = new MFXTableColumn<>("Price" );;
        priceCol.setRowCellFactory(item->{
            MFXTableRowCell<WorkTable,Double> cell = new MFXTableRowCell<>(WorkTable::getPrice);
            TextField field = new TextField();
            field.setAlignment(Pos.CENTER_RIGHT);
            cell.setGraphic(field);
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            return cell;
        });

        data.add(new WorkTable(LocalDate.now(), "", "", 0.0));
        table.getTableColumns().addAll(dateCol, techCol, descCol, priceCol);

        table.setItems(data);
    }

    @FXML
    public void onAddStep() {
        data.add(new WorkTable(LocalDate.now(), "", "", 0.0));
    }

    public void initData(WorkOrder wo, Customer co){
        String firstName = co.getFirstName();
        String lastName = co.getLastName();
        String fullName = lastName + "," + firstName;

        //main tab
        status.setText(wo.getStatus());

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

        this.currentWorkOrder = wo;
        this.currentCustomer = co;

        //labour tab
        customerTFX.setText(fullName);
        statusTFX.setText(wo.getStatus());
        numberTFX.setText((String.valueOf(wo.getWorkorderNumber())));

        loadServiceNotes();
        serviceNotesTXT.addEventFilter(MouseEvent.MOUSE_CLICKED, e->{ //attach mouse event to service notes function
            if(e.getButton() == MouseButton.PRIMARY){
                insertNotes(serviceNotesTXT);
                e.consume();
            }
        });

        //parts tab
        partsCustomerTFX.setText(fullName);
        partsStatusTFX.setText(wo.getStatus());
        partsNumberTFX.setText(String.valueOf(wo.getWorkorderNumber()));

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
    public void repairComplete(){
        String sql = "UPDATE work_order SET status = ? where workorder = ?";
        try{
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, "Repaired");
            ps.setInt(2, currentWorkOrder.getWorkorderNumber());
            int rs = ps.executeUpdate();
            if(rs > 0){
                currentWorkOrder.setStatus("Repaired");
                System.out.println("order is closed");
                mainController.LoadOrders();
            }
        }catch (SQLException e){
            System.out.println("error during closing the order");
        }
        status.setText(currentWorkOrder.getStatus());
    }

    @FXML
    public void updateOrder(){ //update the work order
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