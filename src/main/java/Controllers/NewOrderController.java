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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;
import utils.enums.InvoiceType;

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
    @FXML private MFXComboBox<String> repairTypeCombo;
    @FXML private MFXTextField accessoriesTXF;
    @FXML private MFXTextField conditionTXF;
    @FXML private MFXTextField poNumberTXF;
    @FXML private MFXTextField contactNameTXF;
    @FXML private MFXTextField contactPhoneTXF;

    @FXML private MFXTextField idTFX;
    @FXML private MFXTextField firstNameTXF;
    @FXML private MFXTextField lastNameTXF;
    @FXML private MFXTextField phoneTFX;
    @FXML private MFXTextField addressTFX;
    @FXML private MFXTextField townTFX;
    @FXML private MFXTextField zipTFX;

    @FXML private MFXTextField depositTXF;

    @FXML private ActualWorkshopController mainController;
    @FXML private MFXGenericDialog dialogInstance;
    @FXML private CustomersController customerCntrl;


    public void setMainController(ActualWorkshopController controller) {this.mainController = controller;
    }

    public void setDialogInstance(MFXGenericDialog dialogInstance) {
        this.dialogInstance = dialogInstance;
    }


    public void initialize() throws IOException {
        TechNewOrder.setText(LoginController.tech);

        vendorId.setDisable(true);
        warrantyNumber.setDisable(true);

        vendorId.setItems(Vendors.loadIntoBox());

        repairTypeCombo.setItems(javafx.collections.FXCollections.observableArrayList(
                "In-Shop Repair Check", "In-Home Repair Check"));
        repairTypeCombo.selectItem("In-Shop Repair Check");

        depositTXF.setText("0.00");
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
        dialogStage.setMaximized(true);
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
        String problemDescDB  = problemDesc.getText();

        String vendorIdDb = vendorId.getText();
        String warrantyNumberDb= warrantyNumber.getText();

        String stringId = idTFX.getText();
        String depositTxt = depositTXF.getText();
        Double depositDB = depositTxt == null || depositTxt.isBlank() ? 0.0 : Double.valueOf(depositTxt);
        String repairTypeDb = repairTypeCombo.getText();

        if(typeDB.isBlank() || modelDB.isBlank() || stringId.isBlank()){
            new Alert(Alert.AlertType.WARNING, "Please fill out the fields", ButtonType.OK).showAndWait();
            return;
        }
        if(repairTypeDb == null || repairTypeDb.isBlank()){
            new Alert(Alert.AlertType.WARNING, "Please select a Repair Type", ButtonType.OK).showAndWait();
            return;
        }
        // Condition can't be known for an in-home repair — the unit isn't in the shop to inspect.
        boolean inHome = "In-Home Repair Check".equalsIgnoreCase(repairTypeDb);
        if(!inHome && (conditionTXF.getText() == null || conditionTXF.getText().isBlank())){
            new Alert(Alert.AlertType.WARNING, "Please specify the Condition", ButtonType.OK).showAndWait();
            conditionTXF.requestFocus();
            return;
        }
        int customerId;
        try{
            customerId = Integer.parseInt(idTFX.getText());
        }catch (NumberFormatException e){
            new Alert(Alert.AlertType.ERROR, "Customer ID should be me a number.", ButtonType.OK).showAndWait();
            return;
        }

        String accessoriesDb = accessoriesTXF.getText();
        String conditionDb = conditionTXF.getText();
        String contactNameDb = contactNameTXF.getText();
        String contactPhoneDb = contactPhoneTXF.getText();
        String poNumberDb = poNumberTXF.getText();

        int newId = mainController.insertOrderIntoDatabase("New", typeDB, modelDB, serialNumberDB, problemDescDB, customerId, vendorIdDb, warrantyNumberDb, depositDB, repairTypeDb, accessoriesDb, conditionDb, contactNameDb, contactPhoneDb, poNumberDb);

        mainController.reloadOrders();

        WorkOrder wo = new WorkOrder(Integer.valueOf(newId), "New", typeDB, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), vendorIdDb, warrantyNumberDb, modelDB, serialNumberDB, problemDescDB, customerId, depositDB);
        wo.setRepairType(repairTypeDb);
        wo.setAccessories(accessoriesDb);
        wo.setCondition(conditionDb);
        wo.setContactName(contactNameDb);
        wo.setContactPhone(contactPhoneDb);
        wo.setPoNumber(poNumberDb);
        Customer co = new Customer(String.valueOf(customerId), firstNameTXF.getText(), lastNameTXF.getText(), "", phoneTFX.getText(), "", addressTFX.getText(), townTFX.getText(), zipTFX.getText());

        // No deposit means no payment was actually taken, so there's nothing to invoice.
        if (depositDB > 0) {
            openPaymentDialog(wo, co, InvoiceType.DEPOSIT);
        }

        //print wo
        //Print.printWorkOrder(wo, co, dialogInstance.getScene().getWindow());
        utils.DocumentOutput.printOrPdf(
                "Work Order " + wo.getWorkorderNumber(),
                "/main/printOrder.fxml",
                loader -> {
                    Controllers.PrinterController pc = loader.getController();
                    pc.initData(wo, co);
                },
                dialogInstance.getScene().getWindow()
        );

        closeDialog();
    }

    private void openPaymentDialog(WorkOrder wo, Customer co, InvoiceType invoiceType) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/pay.fxml"));
        Parent root = loader.load();

        PaymentController pc = loader.getController();
        pc.setMainController(mainController);
        pc.setContext(wo, co, invoiceType, dialogInstance.getScene().getWindow()); // owner for print dialog

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Payment");
        stage.setScene(new Scene(root));
        stage.showAndWait();
    }

    // "Recreate WO" (ViewOrderController) — same unit coming back in, so device
    // and customer details carry over, but this is a fresh intake: no warranty
    // carried over. Deposit is left at the form's own default (0.00, editable)
    // rather than copied or pinned — nothing has been paid yet on this new order.
    public void prefillFrom(WorkOrder wo, Customer co) {
        type.setText(wo.getType());
        model.setText(wo.getModel());
        serialNumber.setText(wo.getSerialNumber());
        problemDesc.setText(wo.getProblemDesc());
        accessoriesTXF.setText(wo.getAccessories());
        conditionTXF.setText(wo.getCondition());
        poNumberTXF.setText(wo.getPoNumber());
        if (wo.getRepairType() != null && !wo.getRepairType().isBlank()) {
            repairTypeCombo.selectItem(wo.getRepairType());
        }

        idTFX.setText(co.getId());
        firstNameTXF.setText(co.getFirstName());
        lastNameTXF.setText(co.getLastName());
        phoneTFX.setText(co.getPhone());
        addressTFX.setText(co.getAddress());
        townTFX.setText(co.getTown());
        zipTFX.setText(co.getPostalCode());
    }

    @FXML
    public void closeDialog(){
        mainController.rootStack.getChildren().remove(dialogInstance);
        mainController.contentPane.setEffect(null);
        mainController.contentPane.setDisable(false);
    }

}