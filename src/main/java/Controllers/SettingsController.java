package Controllers;

import Skeletons.Technicians;
import io.github.palexdev.materialfx.controls.MFXTableView;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import javafx.fxml.FXML;

public class SettingsController {
    @FXML private ActualWorkshopController mainController;
    @FXML private MFXGenericDialog dialogInstance;

    @FXML private MFXTableView<Technicians> techsTable;


    public void setMainController(ActualWorkshopController controller) {this.mainController = controller;}
    public void setDialogInstance(MFXGenericDialog dialogInstance) {this.dialogInstance = dialogInstance;}


    public void initialize(){

    }


    public void addNewTech(){

    }

    public void saveTechs(){

    }

    @FXML
    public void closeDialog(){
        mainController.rootStack.getChildren().remove(dialogInstance);
        mainController.contentPane.setEffect(null);
        mainController.contentPane.setDisable(false);
    }
}
