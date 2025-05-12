package main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;

public class LoginController {
    public static String tech;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField passwordField;
    @FXML
    private Label wrongLogin;

    private Stage stage;
    private Scene scene;
    private Parent root;




    public static void main(String[] args){

    }

    public void userLogin(ActionEvent e) throws  IOException{
        String username = usernameField.getText();
        String userPassword = passwordField.getText();

        try{
            Connection connection = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password); //actual connection to db
            String logInQuery = "SELECT * FROM technician WHERE username = ? AND password = SHA1(?)"; //prepared statement to prevent sql injection
            PreparedStatement stmt = connection.prepareStatement(logInQuery); //declaring prepared statement

            stmt.setString(1, username);    //setting prepared string for username;
            stmt.setString(2, userPassword); //setting prepared string for password

            ResultSet rs = stmt.executeQuery(); //result set is used whenever we need to read database from the database, we use it when query returns a table of data

            if(rs.next()){ //checks if that tech exists
                System.out.println("technician exists");
                tech = username;
                launchWorkshop(e);
            }else{
                System.out.println("wrong data");
                wrongLogin.setText("Invalid Data");

            }
//            int rowsInserted = stmt.executeUpdate(); //checks if row is at 1, if so the new user was created
//            if(rowsInserted > 0){
//                System.out.println("new technician is created");
//            }
        }catch (SQLException sql){
            sql.printStackTrace();
        }
    }
    public void launchWorkshop(ActionEvent e) throws IOException{
        root = FXMLLoader.load(Main.class.getResource("/main/main.fxml"));
        stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

    }
}
