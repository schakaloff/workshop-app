package Controllers;

import Skeletons.Customer;
import Skeletons.WorkOrder;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import javafx.fxml.FXML;
import javafx.scene.text.Text;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FirstPaymentController {
    @FXML private Text cxFullNameTXT;
    @FXML private Text salesPersonTXT;
    @FXML private Text PONumberTXT;
    @FXML private Text paymentMethodTXT;
    @FXML private Text totalAmountTXT;
    @FXML private Text woNumberTXT;
    @FXML private Text dateTXT;


    @FXML private ActualWorkshopController mainController;
    @FXML private MFXGenericDialog dialogInstance;
    @FXML private CustomersController customerCntrl;

    public void setMainController(ActualWorkshopController controller) {
        this.mainController = controller;
    }
    public void setDialogInstance(MFXGenericDialog dialogInstance) {
        this.dialogInstance = dialogInstance;
    }

    public void initData(WorkOrder wo, Customer co, String method, double amount, String tech, String date) {
        cxFullNameTXT.setText(co.getFirstName() + " " + co.getLastName());
        salesPersonTXT.setText(tech);
        PONumberTXT.setText(String.valueOf(wo.getWorkorderNumber()));
        paymentMethodTXT.setText(method);
        totalAmountTXT.setText(String.format("CDN$%.2f", amount));
        woNumberTXT.setText(String.valueOf(wo.getWorkorderNumber()));
        dateTXT.setText(date != null ? date : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

}
