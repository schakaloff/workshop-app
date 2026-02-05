package utils;

import Skeletons.WorkTable;
import io.github.palexdev.materialfx.controls.MFXContextMenu;
import io.github.palexdev.materialfx.controls.MFXContextMenuItem;
import io.github.palexdev.materialfx.controls.MFXTableView;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class DeletingLabourMethods {

    private final MFXTableView<WorkTable> repairTable;

    private final MFXContextMenu menu;
    private final MFXContextMenuItem deleteItem;

    public DeletingLabourMethods(MFXTableView<WorkTable> repairTable) {
        this.repairTable = repairTable;

        // owner REQUIRED (same as your other ones)
        this.menu = new MFXContextMenu(repairTable);
        this.deleteItem = new MFXContextMenuItem("Delete");

        menu.getItems().add(deleteItem);

        // right click hook
        repairTable.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleRightClick);
    }

    public void setOnDelete(EventHandler<ActionEvent> handler) {
        deleteItem.setOnAction(handler);
    }

    public boolean removeSelectedFromTable() {
        WorkTable selected = repairTable.getSelectionModel().getSelectedValue();
        if (selected == null) return false;

        if (!confirmDelete(selected)) return false;

        repairTable.getItems().remove(selected);
        return true;
    }

    private void handleRightClick(MouseEvent e) {
        if (e.getButton() == MouseButton.SECONDARY) {
            WorkTable selected = repairTable.getSelectionModel().getSelectedValue();
            deleteItem.setDisable(selected == null);

            menu.show(repairTable, e.getScreenX(), e.getScreenY());
            e.consume();
        } else {
            menu.hide();
        }
    }

    private boolean confirmDelete(WorkTable row) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Labour");
        alert.setHeaderText("Delete this labour row?");
        alert.setContentText(
                "Date: " + row.getDate() + "\n" +
                        "Tech: " + row.getTech() + "\n" +
                        "Desc: " + row.getDescription() + "\n" +
                        "Price: " + row.getPrice()
        );
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}
