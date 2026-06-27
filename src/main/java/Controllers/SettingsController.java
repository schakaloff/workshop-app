    package Controllers;

    import Controllers.DbRepo.VendorsQueries;
    import Controllers.DbRepo.WorkshopQueries;
    import Controllers.PrintTechSummaryController;
    import utils.DocumentOutput;
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
    import java.io.IOException;
    import java.time.LocalDate;
    import java.util.List;

    public class SettingsController {

        @FXML private ActualWorkshopController mainController;
        @FXML private MFXGenericDialog dialogInstance;

        // ─── Tech stats ──────────────────────────────────────────────────────────────
        @FXML private MFXComboBox<String> techCombo;
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

        private boolean techTableInitialized = false;

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
            setupTechRowOpen(); // only called once here
            loadTechCombo();
            loadVendorsTable(); // setupVendorRowOpen is called inside here
            setupVendorRowOpen();
        }

        private void loadTechs() {
            techsTable.setItems(FXCollections.observableArrayList());
            if (!techTableInitialized) {
                techData.setAll(TableMethods.loadTechniciansWithRoles());
                TableMethods.loadTechniciansRolesTable(techsTable, techData);
                techTableInitialized = true;
            } else {
                techData.setAll(TableMethods.loadTechniciansWithRoles());
            }
            techsTable.setItems(techData);
        }

        // ─── TECHNICIANS ─────────────────────────────────────────────────────────────


        private void setupTechRowOpen() {
            techsTable.setTableRowFactory(tech -> {
                MFXTableRow<Technicians> row = new MFXTableRow<>(techsTable, tech);
                row.setPrefHeight(45);
                row.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
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
            openModal(dialog, "Technician Info");
            loadTechs();
        }

        @FXML
        public void addNewTech() throws IOException {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/newTech.fxml"));
            MFXGenericDialog dialog = loader.load();
            NewTechController dialogController = loader.getController();
            dialogController.setMainController(mainController);
            dialogController.setDialogInstance(dialog);
            openModal(dialog, "New Technician");
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
            for (Technicians t : techData) {
                names.add(t.getUserName());
            }
            techCombo.setItems(names);
        }

        // ─── VENDORS ─────────────────────────────────────────────────────────────────

        private void loadVendorsTable() {
            vendorsTable.setItems(FXCollections.observableArrayList());
            vendorData.setAll(VendorsQueries.getAllVendors());
            TableMethods.loadVendorsTable(vendorsTable, vendorData);
            vendorsTable.setItems(vendorData);
            // NO setupVendorRowOpen() here
        }

        private void setupVendorRowOpen() {
            vendorsTable.setTableRowFactory(vendor -> {
                MFXTableRow<Vendor> row = new MFXTableRow<>(vendorsTable, vendor);
                row.setPrefHeight(45);
                row.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                    if (event.getClickCount() == 2) {
                        Vendor selected = row.getData();
                        if (selected != null) {
                            openViewVendor(selected);
                        }
                    }
                });
                return row;
            });
        }

        @FXML
        public void addNewVendorFull() {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/newVendor.fxml"));
                MFXGenericDialog dialog = loader.load();
                NewVendorFullController vc = loader.getController();
                vc.setOnSaved(() -> loadVendorsTable());
                openModal(dialog, "Add Vendor");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void openViewVendor(Vendor vendor) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/viewVendor.fxml"));
                MFXGenericDialog dialog = loader.load();
                ViewVendorController vc = loader.getController();
                vc.setVendorData(vendor);
                vc.setOnSaved(() -> loadVendorsTable());
                openModal(dialog, "Edit Vendor");
            } catch (Exception e) {
                e.printStackTrace();
            }
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

        // ─── TECH STATS ──────────────────────────────────────────────────────────────

        @FXML
        public void calculate() {
            String techName = techCombo.getValue() != null ? techCombo.getValue() : null;
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

        @FXML
        public void printTechSummary() {
            String techName    = techCombo.getValue();
            LocalDate fromDate = fromDP.getValue();
            LocalDate toDate   = toDP.getValue();

            if (techName == null || techName.isBlank() || fromDate == null || toDate == null) {
                new Alert(Alert.AlertType.WARNING,
                        "Select a technician and date range first.", ButtonType.OK).showAndWait();
                return;
            }
            if (fromDate.isAfter(toDate)) {
                new Alert(Alert.AlertType.WARNING,
                        "\"From\" date must be before \"To\" date.", ButtonType.OK).showAndWait();
                return;
            }

            try {
                WorkshopQueries workshopQueries = new WorkshopQueries();
                List<TechWorkRow> rows = workshopQueries.loadTechWorkByDateRange(techName, fromDate, toDate);

                final String    fn = techName;
                final LocalDate fd = fromDate;
                final LocalDate td = toDate;

                DocumentOutput.printOrPdf(
                        "Tech Summary - " + techName,
                        "/main/techSummary.fxml",
                        loader -> {
                            PrintTechSummaryController ctrl = loader.getController();
                            ctrl.initData(fn, fd, td, rows);
                        },
                        dialogInstance.getScene().getWindow()
                );
            } catch (Exception e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR,
                        "Failed to generate report: " + e.getMessage(), ButtonType.OK).showAndWait();
            }
        }

        // ─── SHOP STATS ──────────────────────────────────────────────────────────────

        @FXML
        public void calculateShopStats() {
            LocalDate fromDate = statsFromSettingDP.getValue();
            LocalDate toDate   = statsToSettingDP.getValue();

            if (fromDate == null || toDate == null) return;
            if (fromDate.isAfter(toDate)) return;

            // SQL moved to WorkshopQueries.loadShopStats()
            double[] stats = WorkshopQueries.loadShopStats(fromDate, toDate);
            shopTotalEarnedTXF.setText(String.format("$%.2f", stats[0]));
            shopTotalRepairsTXF.setText(String.valueOf((int) stats[1]));
            shopTotalPSTTXF.setText(String.format("$%.2f", stats[2]));
            shopTotalGSTTXF.setText(String.format("$%.2f", stats[3]));
        }

        // ─── CLOSE ───────────────────────────────────────────────────────────────────

        @FXML
        public void closeDialog() {
            mainController.rootStack.getChildren().remove(dialogInstance);
            mainController.contentPane.setEffect(null);
            mainController.contentPane.setDisable(false);
        }

        // ─── HELPERS ─────────────────────────────────────────────────────────────────

        /** Opens an MFXGenericDialog as a modal window. */
        private void openModal(MFXGenericDialog dialog, String title) {
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(title);
            stage.setScene(new Scene(dialog));
            stage.showAndWait();
        }
    }