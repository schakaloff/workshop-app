package DB;

import Controllers.DbRepo.VendorsQueries;
import Controllers.VendorsController;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class Vendors {

    /** Loads vendor names from the database into an ObservableList. */
    public static ObservableList<String> loadIntoBox() {
        List<String> data = VendorsQueries.getAllVendorNames();
        return FXCollections.observableArrayList(data);
    }

    /**
     * Populates the combo box from the DB and appends a "+" entry
     * that opens the new-vendor window when selected.
     */
    public static void addNewVendor(MFXComboBox<String> comboBox) {
        refreshComboBox(comboBox);

        comboBox.setOnAction(e -> {
            String selected = comboBox.getValue();
            if ("+".equals(selected)) {
                try {
                    newVendorWindow(comboBox); // pass box so we can refresh after add
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                comboBox.getSelectionModel().clearSelection();
            }
        });
    }

    /** Reloads vendor names from the DB and re-sets the combo box items. */
    public static void refreshComboBox(MFXComboBox<String> comboBox) {
        ObservableList<String> items = loadIntoBox();
        items.add("+");
        comboBox.setItems(items);
    }

    public static void newVendorWindow(MFXComboBox<String> comboBox) throws IOException {
        FXMLLoader loader = new FXMLLoader(Vendors.class.getResource("/main/newVendor.fxml"));
        MFXGenericDialog dialog = loader.load();

        // Give the controller a reference to the combo box so it can refresh it
        VendorsController vc = loader.getController();
        vc.setComboBox(comboBox);

        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("New Vendor");
        dialogStage.setScene(new Scene(dialog));
        dialogStage.showAndWait();
    }
}