package Controllers;

import DB.DbConfig;
import DB.Vendors;
import Skeletons.Customer;
import Skeletons.WorkOrder;
import io.github.palexdev.materialfx.controls.MFXTableColumn;
import io.github.palexdev.materialfx.controls.MFXTableView;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;


public class CustomersController {

    @FXML
    private MFXTableView<Customer> table;
    private final ObservableList<Customer> data = FXCollections.observableArrayList();

    public void initialize(){
        table.autosizeColumnsOnInitialization();
        loadTable();
        loadCustomers();
        table.setItems(data);
    }

    public void loadTable(){
        MFXTableColumn<Customer> id = new MFXTableColumn<>("ID", true);
        MFXTableColumn<Customer> firstName = new MFXTableColumn<>("First Name", true);
        MFXTableColumn<Customer> lastName = new MFXTableColumn<>("Last Name", true);
        MFXTableColumn<Customer> phoneNu = new MFXTableColumn<>("Phone", true);
        MFXTableColumn<Customer> town = new MFXTableColumn<>("Town", true);

        id.setRowCellFactory(customer ->  new MFXTableRowCell<>(Customer::getId));
        firstName.setRowCellFactory(customer ->  new MFXTableRowCell<>(Customer::getFirstName));
        lastName.setRowCellFactory(customer ->  new MFXTableRowCell<>(Customer::getLastName));
        phoneNu.setRowCellFactory(customer ->  new MFXTableRowCell<>(Customer::getPhone));
        town.setRowCellFactory(customer -> new MFXTableRowCell<>(Customer::getTown){{setAlignment(Pos.CENTER_RIGHT);}});

        town.setAlignment(Pos.CENTER_RIGHT);
        table.getTableColumns().addAll(id, firstName, lastName, phoneNu, town);
    }

    public void loadCustomers() {
        data.clear();
        String sql = "SELECT id, first_name, last_name, additional_names, phone, additional_phone, address, postal_code, town FROM customer";

        try {
            Connection conn = DriverManager.getConnection(
                    DbConfig.url, DbConfig.user, DbConfig.password
            );
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Customer c = new Customer(
                        String.valueOf(rs.getInt("id")),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("additional_names"),
                        rs.getString("phone"),
                        rs.getString("additional_phone"),
                        rs.getString("address"),
                        rs.getString("postal_code"),
                        rs.getString("town")
                );
                data.add(c);
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            // you might want to show an alert here
        }

        table.setItems(data);
    }


    public void addNewCustomer() throws IOException {
        FXMLLoader loader = new FXMLLoader(Vendors.class.getResource("/main/newCustomer.fxml"));

        MFXGenericDialog dialog = loader.load();

        Stage dialogStage = new Stage();
        /*
        we are telling javafx that new stage should be modal
        It will prevent user from interacting with other windows.

        Modality.APPLICATION blocks mouse and keyboard input to all other windows in this app.
         */
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Customers");


        Scene scene = new Scene(dialog);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();

        NewCustomerController ctrl = loader.getController();
        Customer created = ctrl.getCustomer();
        data.add(created);
        table.setItems(data);
    }
}
