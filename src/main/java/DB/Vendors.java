package DB;

import Controllers.DbRepo.VendorsQueries;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class Vendors {
    public static ObservableList<String> loadIntoBox() {
        List<String> data = VendorsQueries.getAllVendorNames();
        return FXCollections.observableArrayList(data);
    }
}