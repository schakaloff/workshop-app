package Controllers;

import DB.DbConfig;
import Skeletons.Customer;
import Skeletons.WorkOrder;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;

import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;
import print.Print;


import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ViewOrderController {
    @FXML private ActualWorkshopController mainController;
    @FXML private MFXGenericDialog dialogInstance;

    @FXML private MFXComboBox<String> vendorId;
    @FXML private MFXTextField warrantyNumber;

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

    @FXML private TabPane tabPane;
    @FXML private TextArea serviceNotesTXT;

    private WorkOrder currentWorkOrder;
    private Customer currentCustomer;

    public void setMainController(ActualWorkshopController controller) {this.mainController = controller;}
    public void setDialogInstance(MFXGenericDialog dialogInstance) {this.dialogInstance = dialogInstance;}

    public void initialize(){tabPane.setFocusTraversable(false);}

    public void initData(WorkOrder wo, Customer co){
        //info tab
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

        this.currentWorkOrder = wo;
        this.currentCustomer = co;

        //work tab

        loadServiceNotes();

        serviceNotesTXT.addEventFilter(MouseEvent.MOUSE_CLICKED, e->{ //attach mouse event to service notes function
            if(e.getButton() == MouseButton.PRIMARY){
                insertNotes(serviceNotesTXT);
                e.consume();
            }
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
            ps.setInt   (8, woNumber);
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