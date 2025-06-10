package Controllers;

import DB.Vendors;
import Controllers.ViewOrderController;
import Skeletons.WorkOrder;
import io.github.palexdev.materialfx.controls.MFXCheckbox;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.print.PrinterJob;
import javafx.scene.Parent;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import print.Print;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NewOrderController {
    @FXML private MFXTextField TechNewOrder;

    @FXML private MFXComboBox<String> vendorID;
    @FXML private MFXCheckbox warrantyCheckBox;
    @FXML private MFXTextField warrantyNumber;

    @FXML private MFXTextField type;
    @FXML private MFXTextField model;
    @FXML private MFXTextField serialNumber;
    @FXML private TextArea problemDesc;

    @FXML private ActualWorkshopController mainController;
    @FXML private MFXGenericDialog dialogInstance;

    public void setMainController(ActualWorkshopController controller) {
        this.mainController = controller;
    }
    public void setDialogInstance(MFXGenericDialog dialogInstance) {
        this.dialogInstance = dialogInstance;
    }

    @FXML private StackPane rootStack;
    private WorkOrder currentWorkOrder;

    @FXML
    public void initialize(WorkOrder wo) throws IOException {
        TechNewOrder.setText(LoginController.tech);

        vendorID.setDisable(true);
        warrantyNumber.setDisable(true);

        //vendorID.getItems().setAll(Vendors.loadIntoBox("src/main/resources/VendorsList.txt"));
        Vendors.addNewVendor(vendorID);
        this.currentWorkOrder = wo;

    }

    public void warrantySelected(){
        if(!warrantyCheckBox.isSelected()){
            vendorID.setDisable(true);
            warrantyNumber.setDisable(true);
        }else{
            vendorID.setDisable(false);
            warrantyNumber.setDisable(false);
        }
    }

    @FXML
    public void closeDialog(){
        mainController.rootStack.getChildren().remove(dialogInstance);
        mainController.contentPane.setEffect(null);
        mainController.contentPane.setDisable(false);
    }

    @FXML
    public void makeNewOrder() throws Exception {
        String typeDB         = type.getText();
        String modelDB        = model.getText();
        String serialNumberDB = serialNumber.getText();
        String problemDescDB  = problemDesc.getText();

        int newId = mainController.insertOrderIntoDatabase("New", typeDB, modelDB, serialNumberDB, problemDescDB);
        mainController.loadOrders();

        String createdAtDB = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        WorkOrder wo = new WorkOrder(String.valueOf(newId), "New", typeDB, createdAtDB, modelDB, serialNumberDB, problemDescDB
        );

        Window owner = dialogInstance.getScene().getWindow();
        Print.printWorkOrder(wo, owner);

        closeDialog();
    }

}
