package Controllers;


import Controllers.DbRepo.VendorDAO;
import DB.Vendors;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public class VendorsController {

    @FXML private MFXTextField vendorName;

    private MFXComboBox<String> comboBox;

    /** Called by Vendors.java before the window opens. */
    public void setComboBox(MFXComboBox<String> comboBox) {
        this.comboBox = comboBox;
    }

    @FXML
    public void addNewVendorBtn() {
        String name = vendorName.getText().trim();

        if (name.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Vendor name cannot be empty.", ButtonType.OK).showAndWait();
            return;
        }

        boolean saved = VendorDAO.insertVendor(name);

        if (saved) {
            // Refresh the combo box in the parent window
            if (comboBox != null) {
                Vendors.refreshComboBox(comboBox);
            }
            // Close this window
            ((Stage) vendorName.getScene().getWindow()).close();
        } else {
            new Alert(Alert.AlertType.ERROR,
                    "Could not save vendor. It may already exist.", ButtonType.OK).showAndWait();
        }
    }
}