package main;

import com.jfoenix.controls.*;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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

import java.io.IOException;
import java.sql.*;

public class ActualWorkshopController{
    @FXML private Label welcomeTech;
    @FXML private Circle techAvatar;

    @FXML public StackPane rootStack;
    @FXML public BorderPane contentPane;

    @FXML private TableView<WorkOrder> ordersTable; //whole TableView

    @FXML private TableColumn<WorkOrder,String> colWorkorderNumber; //first column
    @FXML private TableColumn<WorkOrder, String> colStatus; //second
    @FXML private TableColumn<WorkOrder, String> colDescription; //third
    @FXML private TableColumn<WorkOrder, String> colCreatedAt; //date

    private final ObservableList<WorkOrder> data = FXCollections.observableArrayList(); //extension of List that updates UI automatically


    public void initialize(){
        welcomeTech.setText(LoginController.tech); //welcome tech's name
        avatar(techAvatar); //set avatar's pic

        colWorkorderNumber.setCellValueFactory(c -> c.getValue().workorderNumberProperty());
        colStatus.setCellValueFactory(c -> c.getValue().statusProperty()); // “Show the WorkOrder’s status property in the Status column.”
        colDescription.setCellValueFactory(c -> c.getValue().descriptionProperty()); // “Show the WorkOrder’s description property in the Description column.”
        colCreatedAt.setCellValueFactory(c -> c.getValue().createdAtProperty());

        loadOrders();
        ordersTable.setItems(data);
    }

    public void createNewOrder() throws IOException {

        contentPane.setEffect(new GaussianBlur(4));

        FXMLLoader loader = new FXMLLoader(getClass().getResource("newOrder.fxml"));
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



    public void loadOrders(){
        String sql = "SELECT workorder, status, item_desc, DATE_FORMAT(createdAt, '%Y-%m-%d %H:%i') AS createdAt FROM work_order";
        try{
            Connection connection = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement stmt = connection.prepareStatement(sql);
            data.clear();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()){
                data.add(new WorkOrder(
                        rs.getString("workorder"),
                        rs.getString("status"),
                        rs.getString("item_desc"),
                        rs.getString("createdAt")
                ));
            }
        }catch (SQLException e){
            System.out.println("sql error");
        }
    }

    public void signOut(MouseEvent e) throws IOException { //sign out button
        LoginController.tech = null;
        Parent root = FXMLLoader.load(Main.class.getResource("login.fxml"));
        Stage stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

    }



    public void insertOrderIntoDatabase(String status, String desc) {
        String sql = "INSERT INTO work_order (status, item_desc, createdAt) VALUES (?, ?, NOW())";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setString(2, desc);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
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