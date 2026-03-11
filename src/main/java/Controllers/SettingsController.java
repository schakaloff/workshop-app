package Controllers;

import DB.Vendors;
import Skeletons.Customer;
import Skeletons.Technicians;
import io.github.palexdev.materialfx.controls.MFXTableView;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import utils.TableMethods;

import java.io.IOException;

public class SettingsController {
    @FXML private ActualWorkshopController mainController;
    @FXML private MFXGenericDialog dialogInstance;

    @FXML private MFXTableView<Technicians> techsTable;
    private final ObservableList<Technicians> techData = FXCollections.observableArrayList();


    public void setMainController(ActualWorkshopController controller) {this.mainController = controller;}
    public void setDialogInstance(MFXGenericDialog dialogInstance) {this.dialogInstance = dialogInstance;}


    public void initialize(){
        loadTechs();
    }

    private void loadTechs() {
        techData.setAll(TableMethods.loadTechniciansWithRoles());
        TableMethods.loadTechniciansRolesTable(techsTable, techData);
    }

    @FXML
    public void addNewTech() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/newTech.fxml"));

        MFXGenericDialog dialog = loader.load();

        Stage dialogStage = new Stage();

        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("New Technician");

        Scene scene = new Scene(dialog);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();;
    }

    @FXML
    public void saveTechs() {
        TableMethods.saveAllTechnicianRoles(techData);
        new Alert(Alert.AlertType.INFORMATION, "Technician roles updated successfully.", ButtonType.OK).showAndWait();
    }

    @FXML
    public void closeDialog(){
        mainController.rootStack.getChildren().remove(dialogInstance);
        mainController.contentPane.setEffect(null);
        mainController.contentPane.setDisable(false);
    }
}
