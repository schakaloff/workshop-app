package Controllers;

import Skeletons.Customer;
import Skeletons.WorkOrder;
import io.github.palexdev.materialfx.controls.MFXCheckbox;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Parent;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.stage.Window;
import print.Print;


import java.io.IOException;

public class ViewOrderController {
    @FXML private ActualWorkshopController mainController;
    @FXML private MFXGenericDialog dialogInstance;

    @FXML private MFXComboBox<String> vendorID;
    @FXML private MFXCheckbox warrantyCheckBox;
    @FXML private MFXTextField warrantyNumber;

    @FXML private MFXTextField type;
    @FXML private MFXTextField model;
    @FXML private MFXTextField serialNumber;
    @FXML private TextArea problemDesc;

    private WorkOrder currentWorkOrder;
    private Customer currentCustomer;
    @FXML private TabPane tabPane;

    public void setMainController(ActualWorkshopController controller) {this.mainController = controller;}
    public void setDialogInstance(MFXGenericDialog dialogInstance) {this.dialogInstance = dialogInstance;}

    public void initialize(){tabPane.setFocusTraversable(false);}

    public void initData(WorkOrder wo){
        type.setText(wo.getType());
        model.setText(wo.getModel());
        serialNumber.setText(wo.getSerialNumber());
        problemDesc.setText(wo.getProblemDesc());
        this.currentWorkOrder = wo;
    }
    @FXML
    public void printOrder() throws Exception {

        WorkOrder wo = currentWorkOrder;
        Customer co = currentCustomer;
        Window owner = dialogInstance.getScene().getWindow();
        owner = dialogInstance.getScene().getWindow();
        Print.printWorkOrder(wo, co, owner);

//        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/printOrder.fxml"));
//        Parent printNode = loader.load();
//
//        PrinterController pc = loader.getController();
//        pc.initData(currentWorkOrder);
//        printNode.applyCss();
//        printNode.layout();
//
//        PrinterJob job = PrinterJob.createPrinterJob();
//        Window owner = mainController.rootStack.getScene().getWindow();
//        if (!job.showPrintDialog(owner)) {
//            job.endJob();
//            return;
//        }
//        boolean success = job.printPage(printNode);
//        if (success) {
//            job.endJob();
//        } else {
//            System.err.println("Print failed");
//        }

    }

    @FXML
    public void closeDialog(){
        mainController.rootStack.getChildren().remove(dialogInstance);
        mainController.contentPane.setEffect(null);
        mainController.contentPane.setDisable(false);
    }
}
