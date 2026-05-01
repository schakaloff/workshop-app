package Controllers;
import DB.DbConfig;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import main.Main;

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

    public void userLogin(ActionEvent e) throws IOException {
        String username = usernameField.getText();
        String userPassword = passwordField.getText();

        // disable button to prevent double click
        ((Node) e.getSource()).setDisable(true);
        wrongLogin.setText("Logging in...");

        Task<Boolean> loginTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                Connection connection = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
                String logInQuery = "SELECT * FROM technician WHERE username = ? AND password = SHA1(?)";
                PreparedStatement stmt = connection.prepareStatement(logInQuery);
                stmt.setString(1, username);
                stmt.setString(2, userPassword);
                ResultSet rs = stmt.executeQuery();
                boolean exists = rs.next();
                rs.close(); stmt.close(); connection.close();
                return exists;
            }
        };

        loginTask.setOnSucceeded(ev -> {
            boolean exists = loginTask.getValue();
            if (exists) {
                tech = username;
                try { launchWorkshop(e); }
                catch (IOException ex) { ex.printStackTrace(); }
            } else {
                wrongLogin.setText("Invalid Data");
                ((Node) e.getSource()).setDisable(false);
            }
        });

        loginTask.setOnFailed(ev -> {
            loginTask.getException().printStackTrace();
            wrongLogin.setText("Connection error.");
            ((Node) e.getSource()).setDisable(false);
        });

        new Thread(loginTask).start();
    }
    public void launchWorkshop(ActionEvent e) throws IOException {
        stage = (Stage) ((Node) e.getSource()).getScene().getWindow();

        // Switch scene immediately — login disappears fast
        root = FXMLLoader.load(Main.class.getResource("main.fxml"));
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setResizable(false);
        stage.setWidth(1000);
        stage.setHeight(675);
        stage.setScene(scene);
        stage.show();
    }
}
