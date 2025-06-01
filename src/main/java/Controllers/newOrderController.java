package Controllers;

import DB.Vendors;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;


import java.io.IOException;



public class newOrderController {
//    @FXML private TextField item_description;

    @FXML private MFXTextField TechNewOrder;
    @FXML private MFXComboBox<String> vendorID;

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

        //vendorID.getItems().setAll(Vendors.loadIntoBox("src/main/resources/VendorsList.txt"));
        Vendors.addNewVendor(vendorID);

    }

    @FXML
    public void closeDialog(){
        mainController.rootStack.getChildren().remove(dialogInstance);
        mainController.contentPane.setEffect(null);
    }



    public void makeNewOrder() throws IOException{
//        String desc = item_description.getText();
//        if (desc == null || desc.trim().isEmpty()) {
//            System.out.println("Description is empty!");
//            return;
//        }

//        mainController.insertOrderIntoDatabase("New", desc); // Always set status to "New"
//        mainController.loadOrders();
//
//        // Close dialog after creating order
//        if (dialogInstance != null) {
//            mainController.rootStack.getChildren().remove(dialogInstance);
//            mainController.contentPane.setEffect(null); // remove blur
//        }
    }




}
