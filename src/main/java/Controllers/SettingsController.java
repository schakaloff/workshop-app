    package Controllers;

    import Controllers.DbRepo.VendorsQueries;
    import Controllers.DbRepo.WorkshopQueries;
    import Skeletons.TechWorkRow;
    import Skeletons.Technicians;
    import Skeletons.Vendor;
    import io.github.palexdev.materialfx.controls.*;
    import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
    import javafx.collections.FXCollections;
    import javafx.collections.ObservableList;
    import javafx.fxml.FXML;
    import javafx.fxml.FXMLLoader;
    import javafx.scene.Scene;
    import javafx.scene.control.Alert;
    import javafx.scene.control.ButtonType;
    import javafx.scene.input.MouseEvent;
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

        // ─── Tech stats ──────────────────────────────────────────────────────────────
        @FXML private MFXComboBox techCombo;
        @FXML private MFXDatePicker fromDP;
        @FXML private MFXDatePicker toDP;
        @FXML private MFXTextField totalEarnedTXF;
        @FXML private MFXTextField totalRepairsTXF;

        // ─── Shop stats ──────────────────────────────────────────────────────────────
        @FXML private MFXDatePicker statsFromSettingDP;
        @FXML private MFXDatePicker statsToSettingDP;
        @FXML private MFXTextField shopTotalEarnedTXF;
        @FXML private MFXTextField shopTotalRepairsTXF;
        @FXML private MFXTextField shopTotalGSTTXF;
        @FXML private MFXTextField shopTotalPSTTXF;

        // ─── Technicians table ───────────────────────────────────────────────────────
        @FXML private MFXTableView<Technicians> techsTable;
        private final ObservableList<Technicians> techData = FXCollections.observableArrayList();

        // ─── Vendors table ───────────────────────────────────────────────────────────
        @FXML private MFXTableView<Vendor> vendorsTable;
        private final ObservableList<Vendor> vendorData = FXCollections.observableArrayList();

        // ─── SETTERS ─────────────────────────────────────────────────────────────────

        public void setMainController(ActualWorkshopController controller) {
            this.mainController = controller;
        }

        public void setDialogInstance(MFXGenericDialog dialogInstance) {
            this.dialogInstance = dialogInstance;
        }

        // ─── INITIALIZE ──────────────────────────────────────────────────────────────

        public void initialize() {
            techsTable.setFooterVisible(false);
            loadTechs();
            setupTechRowOpen();
            loadTechCombo();
            loadVendorsTable();
        }

        // ─── TECHNICIANS ─────────────────────────────────────────────────────────────

        private void loadTechs() {
            techData.setAll(TableMethods.loadTechniciansWithRoles());
            TableMethods.loadTechniciansRolesTable(techsTable, techData);
        }

        private void setupTechRowOpen() {
            techsTable.setTableRowFactory(tech -> {
                MFXTableRow<Technicians> row = new MFXTableRow<>(techsTable, tech);
                row.setPrefHeight(45);
                row.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                    if (event.getClickCount() == 2) {
                        Technicians selected = row.getData();
                        if (selected != null) {
                            try { openTechInfo(selected); }
                            catch (IOException e) { e.printStackTrace(); }
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

        private void loadTechCombo() {
            ObservableList<String> names = FXCollections.observableArrayList();
            for (Technicians t : techData) names.add(t.getUserName());
            techCombo.setItems(names);
        }

        // ─── VENDORS ─────────────────────────────────────────────────────────────────

        private void loadVendorsTable() {
            vendorData.setAll(VendorsQueries.getAllVendors());
            TableMethods.loadVendorsTable(vendorsTable, vendorData);
            setupVendorRowOpen();
        }

        @FXML
        public void addNewVendorFull() {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/newVendor.fxml"));
                MFXGenericDialog dialog = loader.load();
                NewVendorFullController vc = loader.getController();
                vc.setOnSaved(() -> loadVendorsTable());
                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("Add Vendor");
                stage.setScene(new Scene(dialog));
                stage.showAndWait();
            } catch (Exception e) { e.printStackTrace(); }
        }
        // ─── TECH STATS ──────────────────────────────────────────────────────────────

        @FXML
        public void calculate() {
            String techName = techCombo.getValue() != null ? techCombo.getValue().toString() : null;
            LocalDate fromDate = fromDP.getValue();
            LocalDate toDate = toDP.getValue();

            if (techName == null || techName.isBlank() || fromDate == null || toDate == null) return;
            if (fromDate.isAfter(toDate)) return;

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

        // ─── SHOP STATS ──────────────────────────────────────────────────────────────

        @FXML
        public void calculateShopStats() {
            LocalDate fromDate = statsFromSettingDP.getValue();
            LocalDate toDate   = statsToSettingDP.getValue();

            if (fromDate == null || toDate == null) return;
            if (fromDate.isAfter(toDate)) return;

            String sql = """
                SELECT
                    SUM(r.price)                AS total_labour,
                    COUNT(DISTINCT w.workorder) AS total_repairs,
                    SUM(w.pst)                  AS total_pst,
                    SUM(w.gst)                  AS total_gst
                FROM work_order_repairs r
                JOIN work_order w ON w.workorder = r.workorder_id
                WHERE w.status IN ('Repair Complete', 'Billing Complete')
                AND r.repair_date BETWEEN ? AND ?
            """;

            try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setDate(1, java.sql.Date.valueOf(fromDate));
                ps.setDate(2, java.sql.Date.valueOf(toDate));
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    shopTotalEarnedTXF.setText(String.format("$%.2f", rs.getDouble("total_labour")));
                    shopTotalRepairsTXF.setText(String.valueOf(rs.getInt("total_repairs")));
                    shopTotalPSTTXF.setText(String.format("$%.2f", rs.getDouble("total_pst")));
                    shopTotalGSTTXF.setText(String.format("$%.2f", rs.getDouble("total_gst")));
                }

            } catch (SQLException e) { e.printStackTrace(); }
        }

        @FXML
        public void saveVendors() {
            for (Vendor v : vendorData) {
                VendorsQueries.updateVendor(
                        v.getId(),
                        v.isPaysLabour(),
                        v.isPaysParts(),
                        v.isPaysPst(),
                        v.isPaysGst()
                );
            }
            new Alert(Alert.AlertType.INFORMATION, "Vendor settings saved.", ButtonType.OK).showAndWait();
        }

        private void setupVendorRowOpen() {
            vendorsTable.setTableRowFactory(vendor -> {
                MFXTableRow<Vendor> row = new MFXTableRow<>(vendorsTable, vendor);
                row.setPrefHeight(45);
                row.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                    if (event.getClickCount() == 2) {
                        Vendor selected = row.getData();
                        if (selected != null) openViewVendor(selected);
                    }
                });
                return row;
            });
        }

        private void openViewVendor(Vendor vendor) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/viewVendor.fxml"));
                MFXGenericDialog dialog = loader.load();
                ViewVendorController vc = loader.getController();
                vc.setVendorData(vendor);
                vc.setOnSaved(() -> loadVendorsTable());
                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("Edit Vendor");
                stage.setScene(new Scene(dialog));
                stage.showAndWait();
            } catch (Exception e) { e.printStackTrace(); }
        }

        // ─── CLOSE ───────────────────────────────────────────────────────────────────

        @FXML
        public void closeDialog() {
            mainController.rootStack.getChildren().remove(dialogInstance);
            mainController.contentPane.setEffect(null);
            mainController.contentPane.setDisable(false);
        }
    }