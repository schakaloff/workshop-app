package Controllers;

import DB.DbConfig;
import Controllers.CustomersController;
import Skeletons.Customer;
import Skeletons.WorkOrder;
import io.github.palexdev.materialfx.controls.MFXPaginatedTableView;
import io.github.palexdev.materialfx.controls.MFXTableColumn;
import io.github.palexdev.materialfx.controls.MFXTableRow;
import io.github.palexdev.materialfx.controls.MFXTableView;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.filter.IntegerFilter;
import io.github.palexdev.materialfx.filter.StringFilter;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.Comparator;

public class ActualWorkshopController{
    @FXML private Label welcomeTech;
    @FXML private Circle techAvatar;
    @FXML public StackPane rootStack;
    @FXML public BorderPane contentPane;


    @FXML private MFXPaginatedTableView<WorkOrder> table;
    CustomersController co;


    private final ObservableList<WorkOrder> data = FXCollections.observableArrayList(); //extension of List that updates UI automatically


    public void initialize(){
        welcomeTech.setText(LoginController.tech); //welcome tech's name
        avatar(techAvatar); //set avatar's pic
        table.setRowsPerPage(5);
        table.setPagesToShow(5);
        LoadOrders();
    }

    public void LoadOrders(){
        table.getTableColumns().clear();
        table.getItems().clear();
        table.autosizeColumnsOnInitialization(); //autosize table columns
        loadOrdersTable(); //load table
        loadOrdersIntoTable(); //load orders into table
        table.setItems(data);
        viewOrder(table);
    }

    public void LoadCustomers(){
        table.getTableColumns().clear();
        table.getItems().clear();

        table.autosizeColumnsOnInitialization(); //autosize table columns
    }

    public void LoadInvoices(){}

    public void viewOrder(MFXTableView<WorkOrder> table) {
        table.setTableRowFactory(workOrder -> {
            MFXTableRow<WorkOrder> row = new MFXTableRow<>(table, workOrder);
            row.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
                if (e.getClickCount() == 2) {
                    e.consume();
                    try {
                        Customer customer = getCustomerById(workOrder.getCustomerId());
                        openWorkOrder(workOrder, customer);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
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
        String sql = "SELECT workorder, status, type, DATE_FORMAT(createdAt, '%Y-%m-%d %H:%i') AS createdAt, vendorId, warrantyNumber, model, serialNumber, problemDesc, customer_id FROM work_order ORDER BY work_order.createdAt DESC";        data.clear();
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
                        rs.getInt("customer_id")
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

    public int insertOrderIntoDatabase(String status, String type, String model, String serialNumber, String problemDesc, int customerId, String vendorId, String warrantyNumber) {
        String sql = "INSERT INTO work_order (status, type, model, serialNumber, problemDesc, customer_id, vendorId, warrantyNumber, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
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
        MFXTableColumn<WorkOrder> workOrder = new MFXTableColumn<>("WorkOrder", true, Comparator.comparing(WorkOrder::getWorkorderNumber));
        MFXTableColumn<WorkOrder> status = new MFXTableColumn<>("Status", true,Comparator.comparing(WorkOrder::getStatus));
        MFXTableColumn<WorkOrder> type = new MFXTableColumn<>("Type", true,Comparator.comparing(WorkOrder::getType));
        MFXTableColumn<WorkOrder> date = new MFXTableColumn<>("Date", true,Comparator.comparing(WorkOrder::getCreatedAt));

        workOrder.setRowCellFactory(order -> new MFXTableRowCell<>(WorkOrder::getWorkorderNumber));
        status.setRowCellFactory(order -> new MFXTableRowCell<>(WorkOrder::getStatus));
        type.setRowCellFactory(order -> new MFXTableRowCell<>(WorkOrder::getType));
        date.setRowCellFactory(order -> new MFXTableRowCell<>(WorkOrder::getCreatedAt){{setAlignment(Pos.CENTER_RIGHT);}});

        date.setAlignment(Pos.CENTER_RIGHT);
        table.getTableColumns().addAll(workOrder,status,type,date);

        table.getFilters().addAll(new IntegerFilter<>("WorkOrder", WorkOrder::getWorkorderNumber));
        table.getFilters().addAll(new StringFilter<>("Status", WorkOrder::getStatus));
        table.getFilters().addAll(new StringFilter<>("Date", WorkOrder::getCreatedAt));
        table.getFilters().addAll(new StringFilter<>("Warranty Number", WorkOrder::getWarrantyNumber));


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