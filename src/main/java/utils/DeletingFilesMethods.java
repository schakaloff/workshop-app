package utils;

import DB.DbConfig;
import Skeletons.FilesHandler;
import io.github.palexdev.materialfx.controls.MFXContextMenu;
import utils.SftpClient;
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
import java.sql.ResultSet;
import java.sql.SQLException;


public class DeletingFilesMethods {

    private final MFXListView<FilesHandler> filesList;

    private final MFXContextMenu filesMenu;
    private final MFXContextMenuItem deleteFileItem;

    private int workorderNumber;

    public DeletingFilesMethods(MFXListView<FilesHandler> filesList) {
        this.filesList = filesList;
        this.filesMenu = new MFXContextMenu(filesList);
        this.deleteFileItem = new MFXContextMenuItem("Delete");
        filesMenu.getItems().add(deleteFileItem);
        filesList.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleFilesContextClick);
    }

    public void setWorkorderNumber(int workorderNumber) { this.workorderNumber = workorderNumber; }


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
        String select = "SELECT file_path FROM work_order_files WHERE id = ? AND workorder_id = ?";
        String delete = "DELETE FROM work_order_files WHERE id = ? AND workorder_id = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password)) {
            String filePath = null;
            try (PreparedStatement sel = conn.prepareStatement(select)) {
                sel.setInt(1, fileId);
                sel.setInt(2, workorderNumber);
                ResultSet rs = sel.executeQuery();
                if (rs.next()) filePath = rs.getString("file_path");
            }

            if (filePath != null) {
                SftpClient.delete(filePath);
            }

            try (PreparedStatement del = conn.prepareStatement(delete)) {
                del.setInt(1, fileId);
                del.setInt(2, workorderNumber);
                del.executeUpdate();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

