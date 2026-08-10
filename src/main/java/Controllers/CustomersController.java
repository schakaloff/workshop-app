package Controllers;

import DB.DbConfig;
import DB.Vendors;
import Skeletons.Customer;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXPaginatedTableView;
import io.github.palexdev.materialfx.controls.MFXTableColumn;
import io.github.palexdev.materialfx.controls.MFXTableRow;
import io.github.palexdev.materialfx.controls.MFXTableView;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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

    private static final int ROWS_PER_PAGE = 15;
    private static final int SHOWN_LIMIT   = 75; // 5 pages @ 15/page — MFXPaginatedTableView breaks past this

    @FXML private MFXPaginatedTableView<Customer> table;
    @FXML private MFXTableView<Customer> tableScrollable;
    @FXML private MFXComboBox<String> searchCB;
    @FXML private MFXTextField searchTXF;

    private final ObservableList<Customer> allData = FXCollections.observableArrayList();

    private Customer selectedCustomer;
    public Customer getSelectedCustomer() { return selectedCustomer; }

    public void initialize() {
        table.setRowsPerPage(ROWS_PER_PAGE);
        loadCustomersTable();
        loadCustomers();
        chooseCustomer();
        setupSearch();
    }

    // Routes to the paginated table (≤75 rows) or the plain scrollable one (more —
    // MFXPaginatedTableView's pagination bar breaks/crashes past a certain page count).
    private void setTableItems(ObservableList<Customer> items) {
        boolean big = items.size() > SHOWN_LIMIT;
        Platform.runLater(() -> {
            try {
                table.setVisible(!big);
                table.setManaged(!big);
                tableScrollable.setVisible(big);
                tableScrollable.setManaged(big);
                if (big) {
                    tableScrollable.setItems(items);
                } else {
                    table.setRowsPerPage(ROWS_PER_PAGE);
                    table.setItems(items);
                    table.setCurrentPage(1);
                    table.goToPage(1);
                }
            } catch (Exception ignored) {}
        });
    }

    private void showDefaultItems() {
        setTableItems(FXCollections.observableArrayList(
                allData.stream().limit(SHOWN_LIMIT).toList()
        ));
    }

    private void setupSearch() {
        searchCB.setItems(FXCollections.observableArrayList(
                "First Name", "Last Name", "Phone", "ID"
        ));
        searchCB.selectItem("First Name");

        searchTXF.setOnAction(e -> onSearchEnter());
        searchTXF.textProperty().addListener((obs, o, n) -> {
            if (n == null || n.isBlank()) showDefaultItems();
        });
    }

    // Strips everything but digits so "(778)363-1671" and "7783631671" match the same way.
    private static String digitsOnly(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    private void onSearchEnter() {
        String text = searchTXF.getText();
        if (text == null || text.isBlank()) return;

        String trimmed       = text.trim().toLowerCase();
        String trimmedDigits = digitsOnly(text);
        String condition     = searchCB.getValue();

        ObservableList<Customer> filtered = FXCollections.observableArrayList(
                allData.stream().filter(c -> switch (condition) {
                    case "First Name" -> c.getFirstName() != null && c.getFirstName().toLowerCase().contains(trimmed);
                    case "Last Name"  -> c.getLastName()  != null && c.getLastName().toLowerCase().contains(trimmed);
                    case "Phone"      -> !trimmedDigits.isEmpty() && digitsOnly(c.getPhone()).contains(trimmedDigits);
                    case "ID"         -> c.getId() != null && c.getId().contains(trimmed);
                    default           -> false;
                }).toList()
        );

        setTableItems(filtered);
    }

    public void chooseCustomer() {
        wireRowOpen(table);
        wireRowOpen(tableScrollable);
    }

    private void wireRowOpen(MFXTableView<Customer> targetTable) {
        targetTable.setTableRowFactory(customer -> {
            MFXTableRow<Customer> row = new MFXTableRow<>(targetTable, customer);
            row.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
                if (e.getClickCount() == 2) {
                    e.consume();
                    // Read the row's CURRENT data, not the factory lambda's captured
                    // "customer" — MaterialFX reuses row instances across virtualized
                    // scroll, so that closure variable goes stale once a row is recycled
                    // to a different item, causing the wrong customer to get selected.
                    Customer current = row.getData();
                    if (current == null) return;
                    selectedCustomer = current;
                    ((Stage) targetTable.getScene().getWindow()).close();
                }
            });
            return row;
        });
    }

    // Columns can't be shared between two table instances, so build a fresh set for each.
    private void setupCustomerColumns(MFXTableView<Customer> targetTable) {
        MFXTableColumn<Customer> id        = new MFXTableColumn<>("ID", true);
        MFXTableColumn<Customer> firstName = new MFXTableColumn<>("First Name", true);
        MFXTableColumn<Customer> lastName  = new MFXTableColumn<>("Last Name", true);
        MFXTableColumn<Customer> phoneNu   = new MFXTableColumn<>("Phone", true);
        MFXTableColumn<Customer> town      = new MFXTableColumn<>("Town", true);

        id.setMinWidth(70);
        firstName.setMinWidth(120);
        lastName.setMinWidth(120);
        phoneNu.setMinWidth(140);
        town.setMinWidth(120);

        id.setRowCellFactory(c        -> new MFXTableRowCell<>(Customer::getId));
        firstName.setRowCellFactory(c -> new MFXTableRowCell<>(Customer::getFirstName));
        lastName.setRowCellFactory(c  -> new MFXTableRowCell<>(Customer::getLastName));
        phoneNu.setRowCellFactory(c   -> new MFXTableRowCell<>(Customer::getPhone));
        town.setRowCellFactory(c      -> new MFXTableRowCell<>(Customer::getTown) {{ setAlignment(Pos.CENTER_RIGHT); }});

        town.setAlignment(Pos.CENTER_RIGHT);
        targetTable.getTableColumns().addAll(id, firstName, lastName, phoneNu, town);
    }

    public void loadCustomersTable() {
        setupCustomerColumns(table);
        setupCustomerColumns(tableScrollable);
    }

    private static ObservableList<Customer> queryCustomers(String extraSql) {
        ObservableList<Customer> result = FXCollections.observableArrayList();
        String sql = "SELECT id, first_name, last_name, additional_names, phone, additional_phone, address, postal_code, town FROM customer"
                + extraSql;

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                result.add(new Customer(
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
        return result;
    }

    // Shows the first 75 immediately (fast), then loads the full table in the
    // background for search to use — the visible table always stays capped at 75
    // by default, same as the dashboard's work order table.
    public void loadCustomers() {
        ObservableList<Customer> firstBatch = queryCustomers(" ORDER BY id LIMIT " + SHOWN_LIMIT);
        allData.setAll(firstBatch);
        showDefaultItems();

        Task<ObservableList<Customer>> task = new Task<>() {
            @Override protected ObservableList<Customer> call() { return queryCustomers(" ORDER BY id"); }
        };
        task.setOnSucceeded(ev -> {
            allData.setAll(task.getValue());
            showDefaultItems();
        });
        task.setOnFailed(ev -> task.getException().printStackTrace());
        new Thread(task).start();
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
            showDefaultItems();
        }
    }
}