package Controllers;

import DB.Vendors;
import io.github.palexdev.materialfx.controls.MFXCheckbox;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;

import java.io.IOException;

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

    @FXML
    public void initialize() throws IOException {
        TechNewOrder.setText(LoginController.tech);

        vendorID.setDisable(true);
        warrantyNumber.setDisable(true);

        //vendorID.getItems().setAll(Vendors.loadIntoBox("src/main/resources/VendorsList.txt"));
        Vendors.addNewVendor(vendorID);

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
    }

    public void makeNewOrder() throws IOException{
        String typeDB = type.getText();
        String modelDB = model.getText();
        String serialNumberDB = serialNumber.getText();
        String problemDescDB = problemDesc.getText();
//        if (desc == null || desc.trim().isEmpty()) {
//            System.out.println("Description is empty!");
//            return;
//        }
        mainController.insertOrderIntoDatabase("New", typeDB, modelDB, serialNumberDB, problemDescDB); // Always set status to "New"
        mainController.loadOrders();

        // Close dialog after creating order
        if (dialogInstance != null) {
            mainController.rootStack.getChildren().remove(dialogInstance);
            mainController.contentPane.setEffect(null); // remove blur
        }


    }

}
