package Controllers;

import Controllers.DbRepo.WorkshopQueries;
import Skeletons.TechWorkRow;
import Skeletons.Technicians;
import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import utils.TableMethods;
import DB.DbConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.io.IOException;
import java.util.List;

public class SettingsController {
    @FXML private ActualWorkshopController mainController;
    @FXML private MFXGenericDialog dialogInstance;

    @FXML private MFXComboBox techCombo;
    @FXML private MFXDatePicker fromDP;
    @FXML private MFXDatePicker toDP;
    @FXML private MFXTextField totalEarnedTXF;
    @FXML private MFXTextField totalRepairsTXF;


    @FXML private MFXTableView<Technicians> techsTable;
    private final ObservableList<Technicians> techData = FXCollections.observableArrayList();

    public void setMainController(ActualWorkshopController controller) {
        this.mainController = controller;
    }

    public void setDialogInstance(MFXGenericDialog dialogInstance) {
        this.dialogInstance = dialogInstance;
    }

    public void initialize() {
        techsTable.setFooterVisible(false);
        loadTechs();
        setupTechRowOpen();
        loadTechCombo();
    }

    private void loadTechs() {
        techData.setAll(TableMethods.loadTechniciansWithRoles());
        TableMethods.loadTechniciansRolesTable(techsTable, techData);
    }

    private void setupTechRowOpen() {
        techsTable.setTableRowFactory(tech -> {
            MFXTableRow<Technicians> row = new MFXTableRow<>(techsTable, tech);
            row.setPrefHeight(45);

            row.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, event -> {
                if (event.getClickCount() == 2) {
                    Technicians selected = row.getData();
                    if (selected != null) {
                        try {
                            openTechInfo(selected);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            return row;
        });
    }
    private void openTechInfo(Technicians tech) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/viewTech.fxml"));
        MFXGenericDialog dialog = loader.load();

        ViewTechnicianInfoController controller = loader.getController();
        controller.setTechnicianData(tech);
        controller.setDialogInstance(dialog);

        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Technician Info");
        dialogStage.setScene(new Scene(dialog));
        dialogStage.showAndWait();

        loadTechs();
    }

    @FXML
    public void addNewTech() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/newTech.fxml"));
        MFXGenericDialog dialog = loader.load();

        NewTechController dialogController = loader.getController();
        dialogController.setMainController(mainController);
        dialogController.setDialogInstance(dialog);

        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("New Technician");
        dialogStage.setScene(new Scene(dialog));
        dialogStage.showAndWait();

        loadTechs();
    }

    @FXML
    public void saveTechs() {
        TableMethods.saveAllTechnicianRoles(techData);
        new Alert(Alert.AlertType.INFORMATION, "Technician roles updated successfully.", ButtonType.OK).showAndWait();
        loadTechs();
    }

    @FXML
    public void calculate() {
        String techName = techCombo.getValue() != null ? techCombo.getValue().toString() : null;
        LocalDate fromDate = fromDP.getValue();
        LocalDate toDate = toDP.getValue();

        if (techName == null || techName.isBlank() || fromDate == null || toDate == null) {
            return;
        }
        if (fromDate.isAfter(toDate)) {
            return;
        }

        WorkshopQueries workshopQueries = new WorkshopQueries();
        List<TechWorkRow> rows = workshopQueries.loadTechWorkByDateRange(techName, fromDate, toDate);

        double totalEarned = 0.0;
        int totalRepairs = 0;

        for (TechWorkRow row : rows) {
            String status = row.getStatus();
            if (status != null && (status.equalsIgnoreCase("repair complete")
                    || status.equalsIgnoreCase("billing complete"))) {
                totalEarned += row.getLabourAmount();
                totalRepairs++;
            }
        }

        totalEarnedTXF.setText(String.format("$%.2f", totalEarned));
        totalRepairsTXF.setText(String.valueOf(totalRepairs));
    }

    private void loadTechCombo() {
        ObservableList<String> names = FXCollections.observableArrayList();
        for (Technicians t : techData) {
            names.add(t.getUserName());
        }
        techCombo.setItems(names);
    }


    @FXML
    public void closeDialog() {
        mainController.rootStack.getChildren().remove(dialogInstance);
        mainController.contentPane.setEffect(null);
        mainController.contentPane.setDisable(false);
    }
}