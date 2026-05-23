package main;

import Controllers.UpdateScreenController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class UpdateScreen extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/main/updateScreen.fxml"));
        StackPane root = loader.load();
        UpdateScreenController controller = loader.getController();

        Scene scene = new Scene(root, 500, 320);
        scene.setFill(Color.TRANSPARENT);

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("FixelCRM");
        stage.getIcons().add(
                new Image(getClass().getResourceAsStream("/icon.png")));
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();

        controller.startUpdate(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}