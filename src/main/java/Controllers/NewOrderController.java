package Controllers;

import DB.Vendors;
import Skeletons.Customer;
import Skeletons.WorkOrder;
import io.github.palexdev.materialfx.controls.MFXCheckbox;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import print.Print;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NewOrderController {
    @FXML private MFXTextField TechNewOrder;

    @FXML private MFXComboBox<String> vendorId;
    @FXML private MFXCheckbox warrantyCheckBox;
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

    @FXML private ActualWorkshopController mainController;
    @FXML private MFXGenericDialog dialogInstance;
    @FXML private CustomersController customerCntrl;


    public void setMainController(ActualWorkshopController controller) {
        this.mainController = controller;
    }
    public void setDialogInstance(MFXGenericDialog dialogInstance) {
        this.dialogInstance = dialogInstance;
    }


    public void initialize() throws IOException {
        TechNewOrder.setText(LoginController.tech);

        vendorId.setDisable(true);
        warrantyNumber.setDisable(true);

        //vendorID.getItems().setAll(Vendors.loadIntoBox("src/main/resources/VendorsList.txt"));
        Vendors.addNewVendor(vendorId);

    }

    public void warrantySelected(){
        if(!warrantyCheckBox.isSelected()){
            vendorId.setDisable(true);
            warrantyNumber.setDisable(true);
        }else{
            vendorId.setDisable(false);
            warrantyNumber.setDisable(false);
        }
    }

    public void selectCustomer() throws IOException {
        FXMLLoader loader = new FXMLLoader(Vendors.class.getResource("/main/customers.fxml"));
        MFXGenericDialog dialog = loader.load();
        Stage dialogStage = new Stage();
        /*
        we are telling javafx that new stage should be modal
        It will prevent user from interacting with other windows.

        Modality.APPLICATION blocks mouse and keyboard input to all other windows in this app.
         */
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Customers");

        Scene scene = new Scene(dialog);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();

        CustomersController picker = loader.getController();
        Customer cus = picker.getSelectedCustomer();
        idTFX.setText(cus.getId());
        firstNameTXF.setText(cus.getFirstName());
        lastNameTXF.setText(cus.getLastName());
        phoneTFX.setText(cus.getPhone());
        addressTFX.setText(cus.getAddress());
        townTFX.setText(cus.getTown());
        zipTFX.setText(cus.getPostalCode());

    }


    @FXML
    public void makeNewOrder() throws Exception {
        String typeDB = type.getText();
        String modelDB = model.getText();
        String serialNumberDB = serialNumber.getText();
        String problemDescDB = problemDesc.getText();

        String vendorIdDb = vendorId.getText();
        String warrantyDB = warrantyNumber.getText();

        String cusID = idTFX.getText();
        String cusFirstName = firstNameTXF.getText();
        String cusLastName = lastNameTXF.getText();
        String cusPhone = phoneTFX.getText();
        String cusAddress = addressTFX.getText();
        String cusTown = townTFX.getText();
        String cusZIP = zipTFX.getText();



        int newId = mainController.insertOrderIntoDatabase("New", typeDB, vendorIdDb, warrantyDB, modelDB, serialNumberDB, problemDescDB);
        mainController.loadOrdersIntoTable();

        String createdAtDB = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        WorkOrder wo = new WorkOrder(String.valueOf(newId), "New", typeDB, createdAtDB, vendorIdDb, warrantyDB, modelDB, serialNumberDB, problemDescDB
        );
        Customer co = new Customer(cusID, cusFirstName, cusLastName,"", cusPhone, "", cusAddress, cusTown, cusZIP);
        Window owner = dialogInstance.getScene().getWindow();
        Print.printWorkOrder(wo, co, owner);

        closeDialog();
    }

    @FXML
    public void closeDialog(){
        mainController.rootStack.getChildren().remove(dialogInstance);
        mainController.contentPane.setEffect(null);
        mainController.contentPane.setDisable(false);
    }

}