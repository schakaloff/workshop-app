package Controllers;

import DB.DbConfig;
import Controllers.CustomersController;
import Skeletons.Customer;
import Skeletons.WorkOrder;
import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.filter.IntegerFilter;
import io.github.palexdev.materialfx.filter.StringFilter;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import javafx.stage.Stage;


import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import main.Main;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

public class ActualWorkshopController{
    @FXML private Label welcomeTech;
    @FXML private Circle techAvatar;
    @FXML public StackPane rootStack;
    @FXML public BorderPane contentPane;
    @FXML private Button btnOldNew;
    @FXML private Button btnRepairedNotPaid;

    @FXML private MFXTextField searchTxtField;

    @FXML private MFXPaginatedTableView<WorkOrder> table;

    private static final DateTimeFormatter DB_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final ObservableList<WorkOrder> data = FXCollections.observableArrayList(); //extension of List that updates UI automatically

    private final ObservableList<WorkOrder> allData = FXCollections.observableArrayList();
    private boolean oldNewFilterEnabled = false;
    private boolean repairedNotBilledFilterEnabled = false;

    private MFXGenericDialog viewOrderDialog;
    private ViewOrderController viewOrderController;

    public void initialize(){
        welcomeTech.setText(LoginController.tech); //welcome tech's name
        //avatar(techAvatar); //set avatar's pic
        table.setRowsPerPage(15);
        table.setPagesToShow(5);
        LoadOrders();

        preloadViewOrderDialog();


        searchTxtField.setOnAction(e -> onSearchEnter());

    }

    public void onSearchEnter() {
        String text = searchTxtField.getText();
        if (text == null || text.isBlank()) return;

        int woNumber;
        try {
            woNumber = Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            return;
        }

        WorkOrder wo = allData.stream().filter(w -> w.getWorkorderNumber() == woNumber).findFirst().orElse(null);

        UILoader(wo);


    }

    private void UILoader(WorkOrder wo){
        Task<Customer> task = new Task<>() {
            @Override
            protected Customer call() {
                return getCustomerById(wo.getCustomerId());
            }
        };
        task.setOnSucceeded(ev -> {
            Customer customer = task.getValue();
            if (customer != null) {
                try {
                    openWorkOrderFast(wo, customer);
                    searchTxtField.clear();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        task.setOnFailed(ev -> task.getException().printStackTrace());

        Thread t = new Thread(task);
        t.start();
    }

    public void openWorkOrderFast(WorkOrder order, Customer co) {
        contentPane.setEffect(new GaussianBlur(4));
        contentPane.setDisable(true);
        viewOrderController.initData(order, co);
        if (!rootStack.getChildren().contains(viewOrderDialog)) {
            viewOrderDialog.setOpacity(0);
            viewOrderDialog.setScaleX(0.8);
            viewOrderDialog.setScaleY(0.8);
            rootStack.getChildren().add(viewOrderDialog);
            playShowAnimation(viewOrderDialog);
        } else {
            viewOrderDialog.toFront();
        }
    }

    public void LoadOrders() {
        table.getTableColumns().clear();
        table.getItems().clear();
        //table.autosizeColumnsOnInitialization();

        loadOrdersTable();

        loadOrdersIntoTable();
        allData.setAll(data);

        table.setItems(allData);
        viewOrder(table);

        updateOldNewButtonCount();
        updateRepairedNotBilledButtonCount();
    }

    public void LoadCustomers(){
        table.getTableColumns().clear();
        table.getItems().clear();
        table.autosizeColumnsOnInitialization(); //autosize table columns
    }

    public void LoadInvoices(){}

    private boolean isStatusNew(String status) {
        if (status == null) return false;
        String s = status.trim().toLowerCase();
        return s.equals("new");
    }

    private boolean isStatusComplete(String status) {
        if (status == null) return false;
        String s = status.trim().toLowerCase();
        return s.equals("repair complete");
    }

    private int countOldNewOver10() {
        return (int) allData.stream().filter(wo -> isStatusNew(wo.getStatus())).filter(wo -> ageDays(wo) > 10).count();
    }

    private int countRepairedNotBilled() {
        return (int) allData.stream().filter(wo -> isStatusComplete(wo.getStatus())).count();
    }

    @FXML
    public void showOldNewOver10() {
        oldNewFilterEnabled = !oldNewFilterEnabled;
        if (oldNewFilterEnabled) {
            ObservableList<WorkOrder> filtered = FXCollections.observableArrayList(
                    allData.stream().filter(wo -> isStatusNew(wo.getStatus())).filter(wo -> ageDays(wo) > 10).toList()
            );
            table.setItems(filtered);
            btnOldNew.setText("SHOWING OLD NEW WO (>10d): " + filtered.size());
            btnOldNew.setStyle("-fx-background-color: rgba(255,0,0,0.35);");
        } else {
            table.setItems(allData);
            updateOldNewButtonCount();
        }
    }

    @FXML
    public void showRepairedNotPaid() {
        repairedNotBilledFilterEnabled = !repairedNotBilledFilterEnabled;
        if (repairedNotBilledFilterEnabled) {
            oldNewFilterEnabled = false;
            updateOldNewButtonCount();
            btnOldNew.setStyle("");
        }
        if (repairedNotBilledFilterEnabled) {
            ObservableList<WorkOrder> filtered = FXCollections.observableArrayList(allData.stream().filter(wo -> isStatusComplete(wo.getStatus())).toList());
            table.setItems(filtered);
            btnRepairedNotPaid.setText("SHOWING REPAIRED NOT BILLED: " + filtered.size());
            btnRepairedNotPaid.setStyle("-fx-background-color: rgba(0, 120, 255, 0.35);");
        } else {
            table.setItems(allData);
            updateRepairedNotBilledButtonCount();
        }
    }

    private void updateOldNewButtonCount() {
        if (btnOldNew == null) return;
        int count = countOldNewOver10();
        btnOldNew.setText("OLD OPENED WO: " + count);
        if (count > 0) {
            btnOldNew.setStyle("-fx-background-color: rgba(255,0,0,0.20);");
        } else {
            btnOldNew.setStyle("");
        }
    }

    private void updateRepairedNotBilledButtonCount() {
        if (btnRepairedNotPaid == null) return;
        int count = countRepairedNotBilled();
        btnRepairedNotPaid.setText("REPAIRED NOT BILLED: " + count);

        if (count > 0) {
            btnRepairedNotPaid.setStyle("-fx-background-color: rgba(0, 120, 255, 0.20);");
        } else {
            btnRepairedNotPaid.setStyle("");
        }
    }

    private boolean isAgingStatus(String status) {
        if (status == null) return false;
        String s = status.trim().toLowerCase();
        return s.equals("new") || s.equals("in progress") || s.equals("waiting parts");
    }

    private long ageDays(WorkOrder wo) {
        try {
            LocalDateTime created = LocalDateTime.parse(wo.getCreatedAt(), DB_DT);
            LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
            return ChronoUnit.DAYS.between(created, now);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void preloadViewOrderDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/viewOrder.fxml"));
            viewOrderDialog = loader.load();
            viewOrderController = loader.getController();

            viewOrderController.setMainController(this);
            viewOrderController.setDialogInstance(viewOrderDialog);

        } catch (IOException e) {
            throw new RuntimeException("Failed to preload viewOrder.fxml", e);
        }
    }

    private void applyRowStyle(MFXTableRow<WorkOrder> row, WorkOrder wo) {
        row.setStyle("");
        if (wo == null) return;

        if (isStatusComplete(wo.getStatus())) {
            row.setStyle("-fx-background-color: rgba(0, 120, 255, 0.22);");
            return;
        }

        if (isAgingStatus(wo.getStatus())) {
            long days = ageDays(wo);
            if (days > 10) {
                row.setStyle("-fx-background-color: rgba(255, 0, 0, 0.25);");
            } else if (days > 5) {
                row.setStyle("-fx-background-color: rgba(255, 215, 0, 0.25);");
            }
        }
    }

    public void viewOrder(MFXTableView<WorkOrder> table) {
        table.setTableRowFactory(wo -> {
            MFXTableRow<WorkOrder> row = new MFXTableRow<>(table, wo);

            applyRowStyle(row, wo);
            try {row.dataProperty().addListener((obs, oldVal, newVal) -> applyRowStyle(row, newVal));} catch (Exception ignored) {}

            row.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
                if (e.getClickCount() == 2) {
                    e.consume();
                    Customer customer = getCustomerById(wo.getCustomerId());
                    openWorkOrderFast(wo, customer);
                }
            });

            return row;
        });
    }

    public Customer getCustomerById(int customerId) {
        String sql = "SELECT * FROM customer WHERE id = ?";
        try {
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, customerId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Customer c = new Customer(
                        rs.getString("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        "",
                        rs.getString("phone"),
                        "",
                        rs.getString("address"),
                        rs.getString("postal_code"),
                        rs.getString("town")
                );
                rs.close();
                stmt.close();
                conn.close();
                return c;
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void loadOrdersIntoTable() {
        String sql = "SELECT workorder, status, type, DATE_FORMAT(createdAt, '%Y-%m-%d %H:%i') AS createdAt, vendorId, warrantyNumber, model, serialNumber, problemDesc, customer_id, deposit_amount FROM work_order ORDER BY work_order.createdAt DESC";        data.clear();
        try {
            Connection connection = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                WorkOrder wo = new WorkOrder(
                        rs.getInt("workorder"),
                        rs.getString("status"),
                        rs.getString("type"),
                        rs.getString("createdAt"),
                        rs.getString("vendorId"),
                        rs.getString("warrantyNumber"),
                        rs.getString("model"),
                        rs.getString("serialNumber"),
                        rs.getString("problemDesc"),
                        rs.getInt("customer_id"),
                        rs.getDouble("deposit_amount")
                );
                data.add(wo);
            }
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException e) {
            System.out.println("issue during loading into table");
        }
    }

    public int insertOrderIntoDatabase(String status, String type, String model, String serialNumber, String problemDesc, int customerId, String vendorId, String warrantyNumber, double deposit) {
        String sql = "INSERT INTO work_order (status, type, model, serialNumber, problemDesc, customer_id, vendorId, warrantyNumber, deposit_amount, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try {
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, status);
            stmt.setString(2, type);
            stmt.setString(3, model);
            stmt.setString(4, serialNumber);
            stmt.setString(5, problemDesc);
            stmt.setInt(6, customerId);
            stmt.setString(7, vendorId);
            stmt.setString(8, warrantyNumber);
            stmt.setDouble(9, deposit);

            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                rs.close();
                stmt.close();
                conn.close();
                return id;
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println("issues during inserting into table");
        }
        return -1;

    }

    public void loadOrdersTable(){
        MFXTableColumn<WorkOrder> workOrder = new MFXTableColumn<>("Workorder",  false);
        MFXTableColumn<WorkOrder> status = new MFXTableColumn<>("Status",false);
        MFXTableColumn<WorkOrder> type = new MFXTableColumn<>("Type", false);
        MFXTableColumn<WorkOrder> date = new MFXTableColumn<>("Date",false);

        workOrder.setColumnResizable(true);
        status.setColumnResizable(true);
        type.setColumnResizable(true);
        date.setColumnResizable(true);

        workOrder.setMinWidth(110);
        status.setMinWidth(160);
        type.setMinWidth(260);
        date.setMinWidth(180);


        workOrder.setRowCellFactory(order -> new MFXTableRowCell<>(WorkOrder::getWorkorderNumber));
        status.setRowCellFactory(order -> new MFXTableRowCell<>(WorkOrder::getStatus));
        type.setRowCellFactory(order -> new MFXTableRowCell<>(WorkOrder::getType));
        date.setRowCellFactory(order -> new MFXTableRowCell<>(WorkOrder::getCreatedAt){{setAlignment(Pos.CENTER_RIGHT);}});




        date.setAlignment(Pos.CENTER_RIGHT);
        table.getTableColumns().addAll(workOrder,status,type,date);

        table.getFilters().addAll(new IntegerFilter<>("Workorder", WorkOrder::getWorkorderNumber));
        table.getFilters().addAll(new StringFilter<>("Status", WorkOrder::getStatus));
        table.getFilters().addAll(new StringFilter<>("Date", WorkOrder::getCreatedAt));
        table.getFilters().addAll(new StringFilter<>("Warranty Number", WorkOrder::getWarrantyNumber));


    }

    public void openSettingsMenu() throws IOException {
        contentPane.setEffect(new GaussianBlur(4));
        contentPane.setDisable(true);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/settings.fxml"));
        MFXGenericDialog dialog = loader.load();

        SettingsController dialogController = loader.getController();
        dialogController.setMainController(this);
        dialogController.setDialogInstance(dialog);

        dialog.setOpacity(0);
        dialog.setScaleX(0.8);
        dialog.setScaleY(0.8);

        rootStack.getChildren().add(dialog);
        playShowAnimation(dialog);

    }

    public void createNewOrder() throws IOException {
        contentPane.setEffect(new GaussianBlur(4));
        contentPane.setDisable(true);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/newOrder.fxml"));
        MFXGenericDialog dialog = loader.load();

        NewOrderController dialogController = loader.getController();
        dialogController.setMainController(this);
        dialogController.setDialogInstance(dialog);

        dialog.setOpacity(0);
        dialog.setScaleX(0.8);
        dialog.setScaleY(0.8);

        rootStack.getChildren().add(dialog);
        playShowAnimation(dialog);

//        FXMLLoader loader = new FXMLLoader(Vendors.class.getResource("/main/newOrder.fxml"));
//        MFXGenericDialog dialog = loader.load();
//        Stage dialogStage = new Stage();
//        /*
//        we are telling javafx that new stage should be modal
//        It will prevent user from interacting with other windows.
//
//        Modality.APPLICATION blocks mouse and keyboard input to all other windows in this app.
//         */
//        dialogStage.initModality(Modality.APPLICATION_MODAL);
//        dialogStage.setTitle("New Order");
//
//        Scene scene = new Scene(dialog);
//        dialogStage.setScene(scene);
//        dialogStage.showAndWait();
    }

    public void openWorkOrder(WorkOrder order, Customer co) throws IOException{
        contentPane.setEffect(new GaussianBlur(4));
        contentPane.setDisable(true);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/viewOrder.fxml"));
        MFXGenericDialog dialog = loader.load();

        ViewOrderController dialogController = loader.getController();
        dialogController.setMainController(this);
        dialogController.setDialogInstance(dialog);
        dialogController.initData(order, co);

        dialog.setOpacity(0);
        dialog.setScaleX(0.8);
        dialog.setScaleY(0.8);

        rootStack.getChildren().add(dialog);
        playShowAnimation(dialog);

    }


    public void avatar(Circle techAvatar){
        Image im = new Image("/avatar.png"); //make
        techAvatar.setCenterX(50);
        techAvatar.setCenterY(50);
        techAvatar.setFill(new ImagePattern(im));
    }



    private void playShowAnimation(MFXGenericDialog dialog) {
        // Fade from transparent → opaque
        FadeTransition fade = new FadeTransition(Duration.millis(250), dialog);
        fade.setFromValue(0);
        fade.setToValue(1);

        // Scale from 80% → 100%
        ScaleTransition scale = new ScaleTransition(Duration.millis(250), dialog);
        scale.setFromX(0.8);
        scale.setToX(1);
        scale.setFromY(0.8);
        scale.setToY(1);

        // Play both at once
        ParallelTransition pt = new ParallelTransition(fade, scale);
        pt.play();
    }



    public void signOut(MouseEvent e) throws IOException { //sign out button
        LoginController.tech = null;
        Parent root = FXMLLoader.load(Main.class.getResource("/main/login.fxml"));
        Stage stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}