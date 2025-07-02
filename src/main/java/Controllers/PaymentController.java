package Controllers;
import Skeletons.Customer;
import Skeletons.WorkOrder;
import io.github.palexdev.materialfx.controls.MFXCheckbox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class PaymentController {
    @FXML private ActualWorkshopController mainController;

    public WorkOrder currentWorkOrder;
    public Customer currentCustomer;

    public void setMainController(ActualWorkshopController controller){this.mainController = controller;}

    @FXML private MFXTextField techIDTXF;
    @FXML private MFXTextField dateTXF;


    @FXML private MFXCheckbox visaCB;
    @FXML private MFXCheckbox debitCB;
    @FXML private MFXCheckbox masterCardCB;
    @FXML private MFXCheckbox cashCB;

    @FXML private MFXTextField visaTXF;
    @FXML private MFXTextField debitTXF;
    @FXML private MFXTextField masterCardTXF;
    @FXML private MFXTextField cashTXF;

    public void initialize(){
        techIDTXF.setText(LoginController.tech);
        dateTXF.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        visaTXF.setDisable(true);
        debitTXF.setDisable(true);
        masterCardTXF.setDisable(true);
        cashTXF.setDisable(true);


    }

    @FXML
    public void paymentTypeSelection(ActionEvent e) {
        MFXCheckbox cb = (MFXCheckbox) e.getSource();
        boolean selected = cb.isSelected();

        switch (cb.getId()) {
            case "visaCB":
                visaTXF.setDisable(!selected);
                if (selected) visaTXF.setText("CDN$");
                else visaTXF.clear();
                break;

            case "debitCB":
                debitTXF.setDisable(!selected);
                if (selected) debitTXF.setText("CDN$");
                else debitTXF.clear();
                break;

            case "masterCardCB":
                masterCardTXF.setDisable(!selected);
                if (selected) masterCardTXF.setText("CDN$");
                else masterCardTXF.clear();
                break;

            case "cashCB":
                cashTXF.setDisable(!selected);
                if (selected) cashTXF.setText("CDN$");
                else cashTXF.clear();
                break;
        }
    }



}
