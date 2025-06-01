package Controllers;

import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.fxml.FXML;

import java.io.IOException;

public class VendorsController {
    @FXML private MFXTextField vendorName;

    public void addNewVendorBtn() throws IOException {
        String vendor = vendorName.getText();
    }

}
