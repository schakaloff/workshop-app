package main;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDrawer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import javafx.scene.input.MouseEvent;
import java.io.IOException;
import java.sql.*;

public class ActualWorkshopController{
    @FXML
    private Label welcomeTech;
    @FXML
    private Circle techAvatar;
    @FXML
    private JFXButton signOutBtn;

    @FXML private TableView<WorkOrder> ordersTable; //whole TableView
    @FXML private TableColumn<WorkOrder,String> colWorkorderNumber; //first column
    @FXML private TableColumn<WorkOrder, String> colStatus; //second
    @FXML private TableColumn<WorkOrder, String> colDescription; //third

    private final ObservableList<WorkOrder> data = FXCollections.observableArrayList();






    public void initialize(){
        welcomeTech.setText(LoginController.tech); //welcome tech's name
        avatar(techAvatar); //set avatar's pic

        colWorkorderNumber.setCellValueFactory(c -> c.getValue().workorderNumberProperty());
        colStatus.setCellValueFactory(c -> c.getValue().statusProperty());
        colDescription.setCellValueFactory(c -> c.getValue().descriptionProperty());

        loadOrders();
        ordersTable.setItems(data);
    }

    public void loadOrders(){
        String sql = "SELECT workorder, status, item_desc FROM work_order";
        try{
            Connection connection = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement stmt = connection.prepareStatement(sql);
            data.clear();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()){
                data.add(new WorkOrder(
                        rs.getString("workorder"),
                        rs.getString("status"),
                        rs.getString("item_desc")
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

    public void avatar(Circle techAvatar){
        Image im = new Image("/avatar.png"); //make
        techAvatar.setCenterX(50);
        techAvatar.setCenterY(50);
        techAvatar.setFill(new ImagePattern(im));
    }


}