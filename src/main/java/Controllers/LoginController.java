package Controllers;
import DB.DbConfig;
import DB.ShopSettings;
import io.github.palexdev.materialfx.controls.MFXPasswordField;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import main.Main;

import java.io.IOException;
import java.sql.*;

public class LoginController {
    public static String tech;
    @FXML
    private MFXTextField usernameField;

    @FXML private MFXPasswordField passwordField;
    @FXML
    private Label wrongLogin;
    @FXML
    private Label versionLabel;

    private Stage stage;
    private Scene scene;
    private Parent root;

    @FXML
    public void initialize() {
        versionLabel.setText("v" + ShopSettings.VERSION);
    }

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
    // Dashboard's fx:id="rootStack" is fixed at this size (main.fxml min/max/pref
    // all locked to it). Rather than rewrite every screen to reflow, we scale the
    // whole thing as one uniform image — resizing/maximizing auto-fits it to the
    // window, and Ctrl+scroll lets the user zoom in further on top of that. Much
    // bigger text for low-vision users without touching per-widget layouts.
    private static final double BASE_W = 1000.0;
    private static final double BASE_H = 675.0;
    private double manualZoom = 1.0;

    public void launchWorkshop(ActionEvent e) throws IOException {
        stage = (Stage) ((Node) e.getSource()).getScene().getWindow();

        // Switch scene immediately — login disappears fast
        root = FXMLLoader.load(Main.class.getResource("main.fxml"));

        Group scaleGroup = new Group(root);
        Scale scale = new Scale(1, 1, BASE_W / 2, BASE_H / 2);
        scaleGroup.getTransforms().add(scale);

        StackPane container = new StackPane(scaleGroup);
        container.setStyle("-fx-background-color: #e8eef0;");
        // On Linux, the scaled content's edge can leave a 1px rounding gap where
        // the Scene's own default white fill shows through as a thin white line
        // along the letterbox border. Make the Scene's fill match the letterbox
        // so any such gap is invisible instead of a white seam.
        scene = new Scene(container, BASE_W, BASE_H, Color.web("#e8eef0"));
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        Runnable updateScale = () -> {
            double factor = Math.min(scene.getWidth() / BASE_W, scene.getHeight() / BASE_H) * manualZoom;
            scale.setX(factor);
            scale.setY(factor);
        };
        scene.widthProperty().addListener((obs, o, n) -> updateScale.run());
        scene.heightProperty().addListener((obs, o, n) -> updateScale.run());

        scene.addEventFilter(ScrollEvent.SCROLL, se -> {
            if (!se.isControlDown()) return;
            se.consume();
            manualZoom += se.getDeltaY() > 0 ? 0.1 : -0.1;
            manualZoom = Math.max(0.5, Math.min(manualZoom, 2.5));
            updateScale.run();
        });

        stage.setResizable(true);
        stage.setMinWidth(BASE_W / 2);
        stage.setMinHeight(BASE_H / 2);
        stage.setWidth(BASE_W);
        stage.setHeight(BASE_H);
        stage.setScene(scene);
        updateScale.run();
        stage.show();
    }
}
