package Controllers;

import DB.DbConfig;
import io.github.palexdev.materialfx.controls.MFXPasswordField;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class NewTechController {

    @FXML private ActualWorkshopController mainController;
    @FXML private MFXGenericDialog dialogInstance;

    @FXML private MFXTextField usernameTXF;
    @FXML private MFXTextField fNameTXF;
    @FXML private MFXTextField lNameTXF;
    @FXML private MFXPasswordField passwordTXF;
    @FXML private MFXPasswordField repPassworfTXF;

    public void setMainController(ActualWorkshopController controller) {
        this.mainController = controller;
    }

    public void setDialogInstance(MFXGenericDialog dialogInstance) {
        this.dialogInstance = dialogInstance;
    }

    public void initialize() {

    }

    @FXML
    public void addTech() {
        String username = usernameTXF.getText().trim();
        String firstName = fNameTXF.getText().trim();
        String lastName = lNameTXF.getText().trim();
        String password = passwordTXF.getText();
        String repeatPassword = repPassworfTXF.getText();

        if(username.isBlank() || firstName.isBlank() || lastName.isBlank() || password.isBlank()){
            new Alert(Alert.AlertType.WARNING,
                    "Please fill all fields",
                    ButtonType.OK).showAndWait();
            return;
        }

        if(!password.equals(repeatPassword)){
            new Alert(Alert.AlertType.WARNING,
                    "Passwords do not match",
                    ButtonType.OK).showAndWait();
            return;
        }

        if(usernameExists(username)){
            new Alert(Alert.AlertType.WARNING,
                    "Username already exists",
                    ButtonType.OK).showAndWait();
            return;
        }

        String hashedPassword = sha1(password);

        String sql = "INSERT INTO technician (username, first_name, last_name, password) VALUES (?, ?, ?, ?)";

        try(Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement ps = conn.prepareStatement(sql)){

            ps.setString(1, username);
            ps.setString(2, firstName);
            ps.setString(3, lastName);
            ps.setString(4, hashedPassword);

            ps.executeUpdate();

            new Alert(Alert.AlertType.INFORMATION, "Technician added successfully", ButtonType.OK).showAndWait();
            closeDialog();

        }catch(Exception e){
            e.printStackTrace();

            new Alert(Alert.AlertType.ERROR,
                    "Failed to add technician",
                    ButtonType.OK).showAndWait();

        }
    }

    private boolean usernameExists(String username){

        String sql = "SELECT 1 FROM technician WHERE username = ? LIMIT 1";

        try(Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement ps = conn.prepareStatement(sql)){

            ps.setString(1, username);

            try(ResultSet rs = ps.executeQuery()){
                return rs.next();
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return false;
    }

    private String sha1(String input){

        try{
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for(byte b : bytes){
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
//
    @FXML
    public void Cancel(){
        closeDialog();
    }

    @FXML
    public void closeDialog() {
        Stage stage = (Stage) dialogInstance.getScene().getWindow();
        stage.close();
    }
}