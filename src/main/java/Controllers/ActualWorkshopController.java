package Controllers;

import DB.DbConfig;
import DB.Vendors;
import Skeletons.WorkOrder;
import io.github.palexdev.materialfx.controls.MFXTableColumn;
import io.github.palexdev.materialfx.controls.MFXTableRow;
import io.github.palexdev.materialfx.controls.MFXTableView;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
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
import javafx.stage.Modality;
import javafx.stage.Stage;


import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import main.Main;

import java.io.IOException;
import java.sql.*;

public class ActualWorkshopController{
    @FXML private Label welcomeTech;
    @FXML private Circle techAvatar;
    @FXML public StackPane rootStack;
    @FXML public BorderPane contentPane;


    @FXML private MFXTableView<WorkOrder> table;


    private final ObservableList<WorkOrder> data = FXCollections.observableArrayList(); //extension of List that updates UI automatically


    public void initialize(){
        welcomeTech.setText(LoginController.tech); //welcome tech's name
        avatar(techAvatar); //set avatar's pic

        table.autosizeColumnsOnInitialization(); //autosize table columns
        loadTable(); //load table
        loadOrdersIntoTable(); //load orders into table
        table.setItems(data);

        viewOrder(table);


    }


    public void LoadOrders(){

    }

    public void LoadCustomers(){
        table.getTableColumns().clear();
        table.getItems().clear();
    }

    public void LoadInvoices(){

    }

    public void viewOrder(MFXTableView<WorkOrder> table){
        table.setTableRowFactory(workOrder -> {
            MFXTableRow<WorkOrder> row = new MFXTableRow<>(table, workOrder);
            row.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
                if (e.getClickCount() == 2) {
                    e.consume();
                   // System.out.println("row " + workOrder.getWorkorderNumber());
                    try {
                        openWorkOrder(workOrder);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
            return row;
        });
    }

    public void loadTable(){
        MFXTableColumn<WorkOrder> workOrder = new MFXTableColumn<>("WorkOrder", true);
        MFXTableColumn<WorkOrder> status = new MFXTableColumn<>("Status", true);
        MFXTableColumn<WorkOrder> type = new MFXTableColumn<>("Type", true);
        MFXTableColumn<WorkOrder> date = new MFXTableColumn<>("Date", true);

        workOrder.setRowCellFactory(order -> new MFXTableRowCell<>(WorkOrder::getWorkorderNumber));
        status.setRowCellFactory(order -> new MFXTableRowCell<>(WorkOrder::getStatus));
        type.setRowCellFactory(order -> new MFXTableRowCell<>(WorkOrder::getType));
        date.setRowCellFactory(order -> new MFXTableRowCell<>(WorkOrder::getCreatedAt){{setAlignment(Pos.CENTER_RIGHT);}});

        date.setAlignment(Pos.CENTER_RIGHT);
        table.getTableColumns().addAll(workOrder,status,type,date);
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

    public void openWorkOrder(WorkOrder order) throws IOException{
        contentPane.setEffect(new GaussianBlur(4));
        contentPane.setDisable(true);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/viewOrder.fxml"));
        MFXGenericDialog dialog = loader.load();

        ViewOrderController dialogController = loader.getController();
        dialogController.setMainController(this);
        dialogController.setDialogInstance(dialog);
        dialogController.initData(order);

        dialog.setOpacity(0);
        dialog.setScaleX(0.8);
        dialog.setScaleY(0.8);

        rootStack.getChildren().add(dialog);
        playShowAnimation(dialog);

    }

    public void loadOrdersIntoTable() {
        String sql = "SELECT workorder, status, type, DATE_FORMAT(createdAt, '%Y-%m-%d %H:%i') AS createdAt, model, serialNumber, problemDesc FROM work_order";
        data.clear();
        try {
            Connection connection = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                WorkOrder wo = new WorkOrder(
                        rs.getString("workorder"),
                        rs.getString("status"),
                        rs.getString("type"),
                        rs.getString("createdAt"),
                        rs.getString("model"),
                        rs.getString("serialNumber"),
                        rs.getString("problemDesc")
                );
                data.add(wo);
            }
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException e) {
            System.out.println("issue during loaders");
        }
    }

    public int insertOrderIntoDatabase(String status, String type, String model, String serialNumber, String problemDesc) {
        String sql = "INSERT INTO work_order (status, type, model, serialNumber, problemDesc, createdAt) VALUES (?, ?, ?, ?, ?, NOW())";
        try {
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, status);
            stmt.setString(2, type);
            stmt.setString(3, model);
            stmt.setString(4, serialNumber);
            stmt.setString(5, problemDesc);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int newId = rs.getInt(1);
                    stmt.close();
                    conn.close();
                    return newId;
                }
            }
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println("issue during inserting");
        }
        return -1;
    }


    public void signOut(MouseEvent e) throws IOException { //sign out button
        LoginController.tech = null;
        Parent root = FXMLLoader.load(Main.class.getResource("/main/login.fxml"));
        Stage stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
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
}