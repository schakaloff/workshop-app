package utils;

import DB.DbConfig;
import Skeletons.FilesHandler;
import io.github.palexdev.materialfx.controls.MFXContextMenu;
import io.github.palexdev.materialfx.controls.MFXContextMenuItem;
import io.github.palexdev.materialfx.controls.MFXListView;
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


public class DeletingMethods {

    private final MFXListView<FilesHandler> filesList;

    private final MFXContextMenu filesMenu;
    private final MFXContextMenuItem deleteFileItem;

    private int workorderNumber;

    public DeletingMethods(MFXListView<FilesHandler> filesList) {
        this.filesList = filesList;

        this.filesMenu = new MFXContextMenu(filesList);
        this.deleteFileItem = new MFXContextMenuItem("Delete");

        filesMenu.getItems().add(deleteFileItem);

        // right click should work immediately
        filesList.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleFilesContextClick);
    }

    public void setWorkorderNumber(int workorderNumber) {
        this.workorderNumber = workorderNumber;
    }

    // ✅ EventHandler instead of Runnable
    public void setOnDelete(EventHandler<ActionEvent> handler) {
        deleteFileItem.setOnAction(handler);
    }

    public boolean deleteSelectedFile() {
        FilesHandler selected = filesList.getSelectionModel().getSelectedValue();
        if (selected == null) return false;

        if (!confirmDelete(selected)) return false;

        deleteFileFromDb(selected.getId(), workorderNumber);
        return true;
    }

    private void handleFilesContextClick(MouseEvent e) {
        if (e.getButton() == MouseButton.SECONDARY) {
            FilesHandler selected = filesList.getSelectionModel().getSelectedValue();
            deleteFileItem.setDisable(selected == null);

            filesMenu.show(filesList, e.getScreenX(), e.getScreenY());
            e.consume();
        } else {
            filesMenu.hide();
        }
    }

    private boolean confirmDelete(FilesHandler file) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete File");
        alert.setHeaderText("Delete this file?");
        alert.setContentText(file.toString());
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void deleteFileFromDb(int fileId, int workorderNumber) {
        String sql = "DELETE FROM work_order_files WHERE id = ? AND workorder_id = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, fileId);
            ps.setInt(2, workorderNumber);
            ps.executeUpdate();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}

