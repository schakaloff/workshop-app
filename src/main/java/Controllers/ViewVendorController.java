package Controllers;

import Controllers.DbRepo.VendorsQueries;
import Skeletons.Vendor;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public class ViewVendorController {

    @FXML private MFXTextField vendorNameTXF;
    @FXML private MFXTextField vendorAddressTXF;
    @FXML private MFXTextField vendorCityTXF;
    @FXML private MFXTextField vendorProvinceTXF;
    @FXML private MFXTextField vendorPostalTXF;
    @FXML private MFXTextField vendorContactTXF;
    @FXML private MFXTextField vendorPhoneTXF;

    private Vendor currentVendor;
    private Runnable onSaved;

    public void setOnSaved(Runnable r) { this.onSaved = r; }

    public void setVendorData(Vendor vendor) {
        this.currentVendor = vendor;
        vendorNameTXF.setText(nullSafe(vendor.getName()));
        vendorAddressTXF.setText(nullSafe(vendor.getAddress()));
        vendorCityTXF.setText(nullSafe(vendor.getCity()));
        vendorProvinceTXF.setText(nullSafe(vendor.getProvince()));
        vendorPostalTXF.setText(nullSafe(vendor.getPostal()));
        vendorContactTXF.setText(nullSafe(vendor.getContact()));
        vendorPhoneTXF.setText(nullSafe(vendor.getPhone()));
    }

    @FXML
    public void updateVendorSQL() {
        String name = vendorNameTXF.getText();
        if (name == null || name.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Vendor name is required.", ButtonType.OK).showAndWait();
            return;
        }

        VendorsQueries.updateVendorFull(
                currentVendor.getId(),
                name,
                nullSafe(vendorAddressTXF.getText()),
                nullSafe(vendorCityTXF.getText()),
                nullSafe(vendorProvinceTXF.getText()),
                nullSafe(vendorPostalTXF.getText()),
                nullSafe(vendorContactTXF.getText()),
                nullSafe(vendorPhoneTXF.getText())
        );

        // update in-memory object too
        currentVendor.setName(name);
        currentVendor.setAddress(nullSafe(vendorAddressTXF.getText()));
        currentVendor.setCity(nullSafe(vendorCityTXF.getText()));
        currentVendor.setProvince(nullSafe(vendorProvinceTXF.getText()));
        currentVendor.setPostal(nullSafe(vendorPostalTXF.getText()));
        currentVendor.setContact(nullSafe(vendorContactTXF.getText()));
        currentVendor.setPhone(nullSafe(vendorPhoneTXF.getText()));

        new Alert(Alert.AlertType.INFORMATION, "Vendor updated.", ButtonType.OK).showAndWait();
        if (onSaved != null) onSaved.run();
        ((Stage) vendorNameTXF.getScene().getWindow()).close();
    }

    @FXML
    public void closeDialog() {
        ((Stage) vendorNameTXF.getScene().getWindow()).close();
    }

    private String nullSafe(String s) {
        return s == null ? "" : s.trim();
    }
}