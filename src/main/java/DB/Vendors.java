package DB;

import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;

import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Vendors {
    public static ObservableList<String> loadIntoBox(String path) throws IOException {
        List<String> data = new ArrayList<>();
        BufferedReader r = new BufferedReader(new FileReader(path));

        String line;
        while((line = r.readLine()) != null){
            data.add(line);
        }
        r.close();
        return FXCollections.observableArrayList(data);
    }

    public static void addNewVendor(MFXComboBox<String> comboBox) throws IOException {
        ObservableList<String> items = loadIntoBox("src/main/resources/VendorsList.txt");
        items.add("+");
        comboBox.setItems(items);

        comboBox.setOnAction(e ->{
            String selected = comboBox.getValue();
            if ("+".equals(selected)) {
                try {
                    newVendorWindow();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                comboBox.getSelectionModel().clearSelection();
            }
        });


    }
    public static void newVendorWindow() throws IOException {
        FXMLLoader loader = new FXMLLoader(Vendors.class.getResource("/main/newVendor.fxml"));
        MFXGenericDialog dialog = loader.load();
        Stage dialogStage = new Stage();

        /*
        we are telling javafx that new stage should be modal
        It will prevent user from interacting with other windows.

        Modality.APPLICATION blocks mouse and keyboard input to all other windows in this app.
         */

        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("New Vendor");

        Scene scene = new Scene(dialog);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }




}
