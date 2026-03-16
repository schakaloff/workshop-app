package Controllers;

import DB.DbConfig;
import Skeletons.Technicians;
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

public class ViewTechnicianInfoController {

    @FXML private MFXGenericDialog rootStack;

    @FXML private MFXTextField usernameTXF;
    @FXML private MFXTextField fNameTXF;
    @FXML private MFXTextField lNameTXF;
    @FXML private MFXPasswordField passwordTXF;
    @FXML private MFXPasswordField repPassworfTXF;

    private MFXGenericDialog dialogInstance;
    private Technicians currentTech;
    private String originalUsername;

    public void setDialogInstance(MFXGenericDialog dialogInstance) {
        this.dialogInstance = dialogInstance;
    }

    public void setTechnicianData(Technicians tech) {
        this.currentTech = tech;
        this.originalUsername = tech.getUserName();

        usernameTXF.setText(tech.getUserName());
        fNameTXF.setText(tech.getFName());
        lNameTXF.setText(tech.getLName());
    }

    @FXML
    public void updateTechInfo() {
        if (currentTech == null) {
            new Alert(Alert.AlertType.ERROR, "No technician selected.", ButtonType.OK).showAndWait();
            return;
        }

        String username = usernameTXF.getText() == null ? "" : usernameTXF.getText().trim();
        String firstName = fNameTXF.getText() == null ? "" : fNameTXF.getText().trim();
        String lastName = lNameTXF.getText() == null ? "" : lNameTXF.getText().trim();
        String password = passwordTXF.getText() == null ? "" : passwordTXF.getText();
        String repeatPassword = repPassworfTXF.getText() == null ? "" : repPassworfTXF.getText();

        if (username.isBlank() || firstName.isBlank() || lastName.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Username, first name, and last name are required.", ButtonType.OK).showAndWait();
            return;
        }

        if (!password.isBlank() || !repeatPassword.isBlank()) {
            if (!password.equals(repeatPassword)) {
                new Alert(Alert.AlertType.WARNING, "Passwords do not match.", ButtonType.OK).showAndWait();
                return;
            }

            updateTechWithPassword(username, firstName, lastName, sha1(password));
        } else {
            updateTechWithoutPassword(username, firstName, lastName);
        }
    }

    private void updateTechWithoutPassword(String username, String firstName, String lastName) {
        String sql = "UPDATE technician SET username = ?, first_name = ?, last_name = ? WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, firstName);
            ps.setString(3, lastName);
            ps.setString(4, originalUsername);

            ps.executeUpdate();

            new Alert(Alert.AlertType.INFORMATION, "Technician updated successfully.", ButtonType.OK).showAndWait();
            closeDialog();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to update technician.", ButtonType.OK).showAndWait();
        }
    }

    private void updateTechWithPassword(String username, String firstName, String lastName, String hashedPassword) {
        String sql = "UPDATE technician SET username = ?, first_name = ?, last_name = ?, password = ? WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, firstName);
            ps.setString(3, lastName);
            ps.setString(4, hashedPassword);
            ps.setString(5, originalUsername);

            ps.executeUpdate();

            new Alert(Alert.AlertType.INFORMATION, "Technician updated successfully.", ButtonType.OK).showAndWait();
            closeDialog();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to update technician.", ButtonType.OK).showAndWait();
        }
    }

    private String sha1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    @FXML
    public void closeDialog() {
        Stage stage = (Stage) usernameTXF.getScene().getWindow();
        stage.close();
    }
}