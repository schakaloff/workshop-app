package Controllers;

import DB.DbConfig;
import Skeletons.Customer;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.fxml.FXML;
import javafx.stage.Stage;

import java.sql.*;

public class NewCustomerController {

    @FXML private MFXTextField firstName;
    @FXML private MFXTextField lastName;
    @FXML private MFXTextField phone;
    @FXML private MFXTextField address;
    @FXML private MFXTextField postalCode;
    @FXML private MFXTextField town;

    private Customer Customer;

    public Customer getCustomer() {
        return Customer;
    }

    @FXML
    public void addNewCustomerIntoDB(){
        String first = firstName.getText();
        String last =  lastName.getText();
        String phoneNum = phone.getText();
        String addr = address.getText();
        String postal = postalCode.getText();
        String twn = town.getText();

        String sql = "INSERT INTO customer (first_name, last_name, additional_names, phone, additional_phone, address, postal_code, town) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try{
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            stmt.setString(1, first);
            stmt.setString(2, last);
            stmt.setString(3, "");       // additional_names
            stmt.setString(4, phoneNum);
            stmt.setString(5, "");       // additional_phone
            stmt.setString(6, addr);
            stmt.setString(7, postal);
            stmt.setString(8, twn);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    String newId = String.valueOf(rs.getInt(1));
                    // 6) Build your Customer so caller can retrieve it later
                    Customer = new Customer(newId, first, last, "", phoneNum, "", addr, postal, twn);
                }
            }
            stmt.close();
            conn.close();
            Stage stage = (Stage) firstName.getScene().getWindow();
            stage.close();

        }catch (SQLException e){
            e.printStackTrace();
        }
    }

}
