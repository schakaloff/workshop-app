package Controllers;

import DB.DbConfig;
import DB.Vendors;
import Skeletons.Customer;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXTableColumn;
import io.github.palexdev.materialfx.controls.MFXTableRow;
import io.github.palexdev.materialfx.controls.MFXTableView;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;

public class CustomersController {

    @FXML private MFXTableView<Customer> table;
    @FXML private MFXComboBox<String> searchCB;
    @FXML private MFXTextField searchTXF;

    private final ObservableList<Customer> data    = FXCollections.observableArrayList();
    private final ObservableList<Customer> allData = FXCollections.observableArrayList();

    private Customer selectedCustomer;
    public Customer getSelectedCustomer() { return selectedCustomer; }

    public void initialize() {
        table.autosizeColumnsOnInitialization();
        loadCustomersTable();
        loadCustomers();
        chooseCustomer();
        setupSearch();
    }

    private void setupSearch() {
        searchCB.setItems(FXCollections.observableArrayList(
                "First Name", "Last Name", "Phone"
        ));
        searchCB.selectItem("First Name");

        searchTXF.setOnAction(e -> onSearchEnter());
        searchTXF.textProperty().addListener((obs, o, n) -> {
            if (n == null || n.isBlank()) table.setItems(allData);
        });
    }

    private void onSearchEnter() {
        String text = searchTXF.getText();
        if (text == null || text.isBlank()) return;

        String trimmed   = text.trim().toLowerCase();
        String condition = searchCB.getValue();

        ObservableList<Customer> filtered = FXCollections.observableArrayList(
                allData.stream().filter(c -> switch (condition) {
                    case "First Name" -> c.getFirstName() != null && c.getFirstName().toLowerCase().contains(trimmed);
                    case "Last Name"  -> c.getLastName()  != null && c.getLastName().toLowerCase().contains(trimmed);
                    case "Phone"      -> c.getPhone()     != null && c.getPhone().contains(trimmed);
                    default           -> false;
                }).toList()
        );

        table.setItems(filtered);
    }

    public void chooseCustomer() {
        table.setTableRowFactory(customer -> {
            MFXTableRow<Customer> row = new MFXTableRow<>(table, customer);
            row.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
                if (e.getClickCount() == 2) {
                    e.consume();
                    selectedCustomer = customer;
                    ((Stage) table.getScene().getWindow()).close();
                }
            });
            return row;
        });
    }

    public void loadCustomersTable() {
        MFXTableColumn<Customer> id        = new MFXTableColumn<>("ID", true);
        MFXTableColumn<Customer> firstName = new MFXTableColumn<>("First Name", true);
        MFXTableColumn<Customer> lastName  = new MFXTableColumn<>("Last Name", true);
        MFXTableColumn<Customer> phoneNu   = new MFXTableColumn<>("Phone", true);
        MFXTableColumn<Customer> town      = new MFXTableColumn<>("Town", true);

        id.setRowCellFactory(c        -> new MFXTableRowCell<>(Customer::getId));
        firstName.setRowCellFactory(c -> new MFXTableRowCell<>(Customer::getFirstName));
        lastName.setRowCellFactory(c  -> new MFXTableRowCell<>(Customer::getLastName));
        phoneNu.setRowCellFactory(c   -> new MFXTableRowCell<>(Customer::getPhone));
        town.setRowCellFactory(c      -> new MFXTableRowCell<>(Customer::getTown) {{ setAlignment(Pos.CENTER_RIGHT); }});

        town.setAlignment(Pos.CENTER_RIGHT);
        table.getTableColumns().addAll(id, firstName, lastName, phoneNu, town);
    }

    public void loadCustomers() {
        data.clear();
        String sql = "SELECT id, first_name, last_name, additional_names, phone, additional_phone, address, postal_code, town FROM customer";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                data.add(new Customer(
                        String.valueOf(rs.getInt("id")),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("additional_names"),
                        rs.getString("phone"),
                        rs.getString("additional_phone"),
                        rs.getString("address"),
                        rs.getString("postal_code"),
                        rs.getString("town")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        allData.setAll(data);
        table.setItems(allData);
    }

    @FXML
    public void addNewCustomer() throws IOException {
        FXMLLoader loader = new FXMLLoader(Vendors.class.getResource("/main/newCustomer.fxml"));
        MFXGenericDialog dialog = loader.load();

        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Customers");
        dialogStage.setScene(new Scene(dialog));
        dialogStage.showAndWait();

        NewCustomerController ctrl = loader.getController();
        Customer created = ctrl.getCustomer();
        if (created != null) {
            allData.add(created);
            table.setItems(allData);
        }
    }
}