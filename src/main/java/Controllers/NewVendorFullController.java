package Controllers;

import Controllers.DbRepo.VendorsQueries;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public class NewVendorFullController {

    @FXML private MFXTextField vendorNameTXF;
    @FXML private MFXTextField vendorAddressTXF;
    @FXML private MFXTextField vendorCityTXF;
    @FXML private MFXTextField vendorProvinceTXF;
    @FXML private MFXTextField vendorPostalTXF;
    @FXML private MFXTextField vendorContactTXF;
    @FXML private MFXTextField vendorPhoneTXF;

    private Runnable onSaved;
    public void setOnSaved(Runnable r) { this.onSaved = r; }

    @FXML
    public void addVendorSQL() {
        String name = vendorNameTXF.getText();
        if (name == null || name.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Vendor name is required.", ButtonType.OK).showAndWait();
            return;
        }

        boolean ok = VendorsQueries.insertVendorFull(
                name,
                nullSafe(vendorAddressTXF.getText()),
                nullSafe(vendorCityTXF.getText()),
                nullSafe(vendorProvinceTXF.getText()),
                nullSafe(vendorPostalTXF.getText()),
                nullSafe(vendorContactTXF.getText()),
                nullSafe(vendorPhoneTXF.getText())
        );

        if (!ok) {
            new Alert(Alert.AlertType.ERROR, "Vendor already exists.", ButtonType.OK).showAndWait();
            return;
        }

        if (onSaved != null) onSaved.run();
        ((Stage) vendorNameTXF.getScene().getWindow()).close();
    }

    private String nullSafe(String s) {
        return s == null ? "" : s.trim();
    }
}