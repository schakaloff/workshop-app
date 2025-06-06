package Controllers;

import DB.DbConfig;
import Skeletons.WorkOrder;
import io.github.palexdev.materialfx.controls.MFXTableColumn;
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

    @FXML private TableView<WorkOrder> ordersTable; //whole TableView

    @FXML private MFXTableView<WorkOrder> table;

    @FXML private TableColumn<WorkOrder, String> colWorkorderNumber; //first column
    @FXML private TableColumn<WorkOrder, String> colStatus; //second
    @FXML private TableColumn<WorkOrder, String> colType; //third
    @FXML private TableColumn<WorkOrder, String> colCreatedAt; //date

    private final ObservableList<WorkOrder> data = FXCollections.observableArrayList(); //extension of List that updates UI automatically


    public void initialize(){
        welcomeTech.setText(LoginController.tech); //welcome tech's name
        avatar(techAvatar); //set avatar's pic

//        colWorkorderNumber.setCellValueFactory(c -> c.getValue().workorderNumberProperty());
//        colStatus.setCellValueFactory(c -> c.getValue().statusProperty()); // “Show the WorkOrder’s status property in the Status column.”
//        colType.setCellValueFactory(c -> c.getValue().typeProperty()); // “Show the WorkOrder’s description property in the Description column.”
//        colCreatedAt.setCellValueFactory(c -> c.getValue().createdAtProperty());
        table.autosizeColumnsOnInitialization();

        loadTable();
        loadOrders();
        table.setItems(data);

//        ordersTable.setItems(data);
    }

    public void loadTable(){
        MFXTableColumn<WorkOrder> workOrder = new MFXTableColumn<>("WorkOrder", true);
        MFXTableColumn<WorkOrder> status = new MFXTableColumn<>("Status", true);
        MFXTableColumn<WorkOrder> type = new MFXTableColumn<>("Type", true);
        MFXTableColumn<WorkOrder> date = new MFXTableColumn<>("Date", true);

        workOrder.setRowCellFactory(order -> new MFXTableRowCell<>(WorkOrder::getWorkorderNumber));
        status.setRowCellFactory(order -> new MFXTableRowCell<>(WorkOrder::getStatus));
        type.setRowCellFactory(order -> new MFXTableRowCell<>(WorkOrder::getType));
        date.setRowCellFactory(order -> new MFXTableRowCell<>(WorkOrder::getCreatedAt){{
            setAlignment(Pos.CENTER_RIGHT);
        }});

        date.setAlignment(Pos.CENTER_RIGHT);

        table.getTableColumns().addAll(workOrder,status,type,date);

    }

    public void createNewOrder() throws IOException {

        contentPane.setEffect(new GaussianBlur(4));

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/newOrder.fxml"));
        MFXGenericDialog dialog = loader.load();

        newOrderController dialogController = loader.getController();
        dialogController.setMainController(this);
        dialogController.setDialogInstance(dialog);

        dialog.setOpacity(0);
        dialog.setScaleX(0.8);
        dialog.setScaleY(0.8);

        rootStack.getChildren().add(dialog);
        playShowAnimation(dialog);
    }


    public void loadOrders() {
        String sql = "SELECT workorder, status, type, DATE_FORMAT(createdAt, '%Y-%m-%d %H:%i') AS createdAt, model, serialNumber, problemDesc FROM work_order";
        data.clear();
        try {
            Connection connection = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String woNumber = rs.getString("workorder");
                String status = rs.getString("status");
                String type = rs.getString("type");
                String createdAt = rs.getString("createdAt");
                WorkOrder wo = new WorkOrder(woNumber, status, type, createdAt, "", "", "");
                data.add(wo);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Something went wrong during loading the orders");
        }
    }

    public void insertOrderIntoDatabase(String status, String type, String model, String serialNumber, String problemDesc) {
        String sql = "INSERT INTO work_order (status, type, model, serialNumber, problemDesc, createdAt) VALUES (?, ?, ?, ?, ?, NOW())";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setString(2, type);
            stmt.setString(3, model);
            stmt.setString(4, serialNumber);
            stmt.setString(5, problemDesc);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }

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