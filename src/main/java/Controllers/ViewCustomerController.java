package Controllers;

import DB.DbConfig;
import Skeletons.Customer;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class ViewCustomerController {

    @FXML private MFXTextField firstName;
    @FXML private MFXTextField lastName;
    @FXML private MFXTextField phone;
    @FXML private MFXTextField address;
    @FXML private MFXTextField postalCode;
    @FXML private MFXTextField town;

    private Customer currentCustomer;
    private Runnable onSaved; // callback to refresh parent fields

    public void setCustomerData(Customer customer) {
        this.currentCustomer = customer;
        firstName.setText(customer.getFirstName());
        lastName.setText(customer.getLastName());
        phone.setText(customer.getPhone());
        address.setText(customer.getAddress());
        postalCode.setText(customer.getPostalCode());
        town.setText(customer.getTown());
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    @FXML
    public void updateCustomerInfoDB() {
        if (currentCustomer == null) {
            new Alert(Alert.AlertType.ERROR, "No customer loaded.", ButtonType.OK).showAndWait();
            return;
        }

        String fn = firstName.getText() == null ? "" : firstName.getText().trim();
        String ln = lastName.getText()  == null ? "" : lastName.getText().trim();
        String ph = phone.getText()     == null ? "" : phone.getText().trim();
        String ad = address.getText()   == null ? "" : address.getText().trim();
        String pc = postalCode.getText()== null ? "" : postalCode.getText().trim();
        String tw = town.getText()      == null ? "" : town.getText().trim();

        if (fn.isBlank() || ln.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "First and last name are required.", ButtonType.OK).showAndWait();
            return;
        }

        String sql = """
            UPDATE customer
            SET first_name = ?, last_name = ?, phone = ?, address = ?, postal_code = ?, town = ?
            WHERE id = ?
        """;

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, fn);
            ps.setString(2, ln);
            ps.setString(3, ph);
            ps.setString(4, ad);
            ps.setString(5, pc);
            ps.setString(6, tw);
            ps.setInt(7, Integer.parseInt(currentCustomer.getId()));
            ps.executeUpdate();

            // update the in-memory customer object too
            currentCustomer.setFirstName(fn);
            currentCustomer.setLastName(ln);
            currentCustomer.setPhone(ph);
            currentCustomer.setAddress(ad);
            currentCustomer.setPostalCode(pc);
            currentCustomer.setTown(tw);

            new Alert(Alert.AlertType.INFORMATION, "Customer updated successfully.", ButtonType.OK).showAndWait();

            if (onSaved != null) onSaved.run(); // refresh parent UI
            closeDialog();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to update customer.", ButtonType.OK).showAndWait();
        }
    }

    @FXML
    public void closeDialog() {
        ((Stage) firstName.getScene().getWindow()).close();
    }
}