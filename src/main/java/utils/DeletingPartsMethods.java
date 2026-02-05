package utils;
import DB.DbConfig;
import Skeletons.PartTable;
import io.github.palexdev.materialfx.controls.MFXContextMenu;
import io.github.palexdev.materialfx.controls.MFXContextMenuItem;
import io.github.palexdev.materialfx.controls.MFXTableView;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public class DeletingPartsMethods {

    private final MFXTableView<PartTable> partsTable;

    private final MFXContextMenu partsMenu;
    private final MFXContextMenuItem deleteItem;

    public DeletingPartsMethods(MFXTableView<PartTable> partsTable) {
        this.partsTable = partsTable;

        this.partsMenu = new MFXContextMenu(partsTable);
        this.deleteItem = new MFXContextMenuItem("Delete");

        partsMenu.getItems().add(deleteItem);

        partsTable.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleContextClick);
    }

    public void setOnDelete(EventHandler<ActionEvent> handler) {
        deleteItem.setOnAction(handler);
    }

    public boolean removeSelectedFromTable() {
        PartTable selected = partsTable.getSelectionModel().getSelectedValue();
        if (selected == null) return false;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Part");
        alert.setHeaderText("Delete this part?");
        alert.setContentText(selected.getName());

        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return false;

        // Remove from UI list
        partsTable.getItems().remove(selected);
        return true;
    }

    private void handleContextClick(MouseEvent e) {
        if (e.getButton() == MouseButton.SECONDARY) {
            PartTable selected = partsTable.getSelectionModel().getSelectedValue();
            deleteItem.setDisable(selected == null);

            partsMenu.show(partsTable, e.getScreenX(), e.getScreenY());
            e.consume();
        } else {
            partsMenu.hide();
        }
    }
}
