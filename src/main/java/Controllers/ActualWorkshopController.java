package Controllers;
import Controllers.DbRepo.WorkshopQueries;
import DB.DbConfig;
import Skeletons.Customer;
import Skeletons.TechWorkRow;
import Skeletons.WorkOrder;
import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.filter.IntegerFilter;
import io.github.palexdev.materialfx.filter.StringFilter;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import main.Main;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ActualWorkshopController {
    @FXML private Label welcomeTech;
    @FXML private Circle techAvatar;
    @FXML public StackPane rootStack;
    @FXML public BorderPane contentPane;
    @FXML public MFXButton signOutBtn;
    @FXML private Button newOrderBTN;
    @FXML private Button btnAllWO;
    @FXML private Button btnOldNew;
    @FXML private Button btnRepairedNotPaid;
    @FXML private Button btnShowMyWO;
    @FXML private MFXTextField searchTxtField;
    @FXML private MFXPaginatedTableView<WorkOrder> table;
    @FXML private AnchorPane personalwork;
    @FXML private MFXTextField techTXF;
    @FXML private MFXDatePicker fromDPicker;
    @FXML private MFXDatePicker toDPicker;
    @FXML private MFXTextField tEarnedTXF;
    @FXML private MFXTextField repairsTXF;
    @FXML private MFXTableView<TechWorkRow> techTable;
    @FXML private MFXButton calcBTN;
    @FXML private MFXComboBox<String> searchCondition;

    // ─── LOADING OVERLAY ────────────────────────────────────────────────────────
    @FXML private StackPane loadingOverlay;
    @FXML private MFXProgressSpinner loadingSpinner;

    private boolean oldNewFilterEnabled = false;
    private boolean repairedNotBilledFilterEnabled = false;
    private boolean myWoFilterEnabled = false;
    private boolean allOpenFilterEnabled = false;
    private boolean isDashboardLoaded = false;

    private final ObservableList<TechWorkRow> techWorkData = FXCollections.observableArrayList();
    private final WorkshopQueries workshopQueries = new WorkshopQueries();
    private static final DateTimeFormatter DB_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final ObservableList<WorkOrder> data    = FXCollections.observableArrayList();
    private final ObservableList<WorkOrder> allData = FXCollections.observableArrayList();

    private MFXGenericDialog viewOrderDialog;
    private ViewOrderController viewOrderController;

    private static final int ROWS_PER_PAGE   = 15;
    private static final int DASHBOARD_LIMIT = 75;

    // ─── LOADING OVERLAY HELPERS ────────────────────────────────────────────────

    private void showLoadingOverlay() {
        loadingSpinner.setProgress(-1); // indeterminate spin
        loadingOverlay.setOpacity(1.0);
        loadingOverlay.setVisible(true);
        loadingOverlay.setManaged(true);
        loadingOverlay.toFront();
    }

    private void hideLoadingOverlay() {
        FadeTransition fade = new FadeTransition(Duration.millis(300), loadingOverlay);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> {
            loadingOverlay.setVisible(false);
            loadingOverlay.setManaged(false);
            loadingOverlay.setOpacity(1.0); // reset for next time
        });
        fade.play();
    }

    // ─── CORE HELPERS ───────────────────────────────────────────────────────────

    private void setTableItems(ObservableList<WorkOrder> items) {
        Platform.runLater(() -> {
            try {
                table.setItems(items);
                table.setCurrentPage(1);
            } catch (Exception ignored) {}
            Platform.runLater(() -> {
                try {
                    table.goToPage(1);
                    table.setCurrentPage(1);
                } catch (Exception ignored) {}
            });
        });
    }

    private void showDashboardItems() {
        ObservableList<WorkOrder> recent = FXCollections.observableArrayList(
                allData.stream().limit(DASHBOARD_LIMIT).toList()
        );
        setTableItems(recent);
    }

    // ─── INITIALIZE ─────────────────────────────────────────────────────────────

    public void initialize() {
        // lightweight stuff first — renders immediately
        welcomeTech.setText(LoginController.tech);
        avatar(techAvatar);
        personalwork.setVisible(false);
        personalwork.setManaged(false);
        techTXF.setText(LoginController.tech);
        techTable.setFooterVisible(false);
        table.setRowsPerPage(ROWS_PER_PAGE);

        searchCondition.setItems(FXCollections.observableArrayList(
                "WO Number", "Phone Number", "First Name", "Last Name", "Full Name"
        ));
        searchCondition.selectItem("WO Number");
        searchTxtField.setOnAction(ev -> onSearchEnter());
        searchTxtField.textProperty().addListener((obs, o, n) -> {
            if (n == null || n.isBlank()) {
                showLoadingOverlay();
                showDashboardItems();
                Platform.runLater(() -> Platform.runLater(this::hideLoadingOverlay));
            }
        });

        // defer heavy work to AFTER the scene is rendered
        Platform.runLater(() -> {
            showLoadingOverlay();

            // load viewOrder.fxml on background thread — saves 793ms of FX thread blocking
            Task<Void> preloadTask = new Task<>() {
                private MFXGenericDialog dialog;
                private ViewOrderController controller;

                @Override
                protected Void call() throws Exception {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/viewOrder.fxml"));
                    dialog = loader.load();
                    controller = loader.getController();
                    return null;
                }

                @Override
                protected void succeeded() {
                    // back on FX thread — safe to touch nodes now
                    viewOrderDialog = dialog;
                    viewOrderController = controller;
                    viewOrderController.setMainController(ActualWorkshopController.this);
                    viewOrderController.setDialogInstance(viewOrderDialog);

                    // now do the rest
                    loadTechStatsTable();
                    fromDPicker.setValue(LocalDate.now().withDayOfMonth(1));
                    toDPicker.setValue(LocalDate.now());
                    LoadOrders();
                }

                @Override
                protected void failed() {
                    throw new RuntimeException("Failed to preload viewOrder.fxml", getException());
                }
            };

            new Thread(preloadTask).start();
        });
    }

    // ─── LOAD ORDERS ────────────────────────────────────────────────────────────

    public void LoadOrders() {
        showDashboardControls();

        table.getTableColumns().clear();
        table.getItems().clear();
        loadOrdersTable();
        viewOrder(table);
        loadOrdersAsync();
    }

    /** Called externally (e.g. after new WO created) to force a full reload. */
    public void reloadOrders() {
        isDashboardLoaded = false;
        LoadOrders();
    }

    @FXML
    public void showDashboard() {
        showDashboardControls();
        showLoadingOverlay();

        if (isDashboardLoaded) {
            showDashboardItems();
            refreshOrdersInBackground();
        } else {
            LoadOrders();
        }
    }

    private void refreshOrdersInBackground() {
        Task<List<WorkOrder>> task = new Task<>() {
            @Override
            protected List<WorkOrder> call() {
                return workshopQueries.loadOrdersIntoTable();
            }
        };

        task.setOnSucceeded(ev -> {
            List<WorkOrder> result = task.getValue();
            data.setAll(result);
            allData.setAll(result);
            showDashboardItems();
            updateAllOpenWOButtonCount();
            updateOldNewButtonCount();
            updateRepairedNotBilledButtonCount();
            updateMyWoButtonCount();
            Platform.runLater(() -> Platform.runLater(this::hideLoadingOverlay));
        });

        task.setOnFailed(ev -> {
            task.getException().printStackTrace();
            hideLoadingOverlay();
        });

        new Thread(task).start();
    }

    private void loadOrdersAsync() {
        Task<List<WorkOrder>> task = new Task<>() {
            @Override
            protected List<WorkOrder> call() {
                return workshopQueries.loadOrdersIntoTable();
            }
        };

        task.setOnSucceeded(ev -> {
            List<WorkOrder> result = task.getValue();
            data.setAll(result);
            allData.setAll(result);
            showDashboardItems();
            isDashboardLoaded = true;
            updateAllOpenWOButtonCount();
            updateOldNewButtonCount();
            updateRepairedNotBilledButtonCount();
            updateMyWoButtonCount();
            // hide spinner after table items are set — deferred so render completes first
            Platform.runLater(() -> Platform.runLater(this::hideLoadingOverlay));
        });

        task.setOnFailed(ev -> {
            task.getException().printStackTrace();
            hideLoadingOverlay();
        });

        new Thread(task).start();
    }

    // kept for compatibility — some callers may still use this directly
    public void loadOrdersIntoTable() {
        data.setAll(workshopQueries.loadOrdersIntoTable());
    }

    // ─── SEARCH ─────────────────────────────────────────────────────────────────

    public void onSearchEnter() {
        String text = searchTxtField.getText();
        if (text == null || text.isBlank()) return;

        String trimmed   = text.trim();
        String condition = searchCondition.getValue();

        switch (condition) {
            case "WO Number"    -> searchByWoNumber(trimmed);
            case "Phone Number" -> searchByPhone(trimmed);
            case "First Name"   -> searchByCustomerField(trimmed, "first_name");
            case "Last Name"    -> searchByCustomerField(trimmed, "last_name");
            case "Full Name"    -> searchByFullName(trimmed);
        }
    }

    private void searchByWoNumber(String text) {
        int woNumber;
        try {
            woNumber = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return;
        }

        WorkOrder found = allData.stream()
                .filter(w -> w.getWorkorderNumber() == woNumber)
                .findFirst().orElse(null);

        if (found != null) {
            setTableItems(FXCollections.observableArrayList(found));
        } else {
            WorkOrder wo = workshopQueries.getWorkOrderById(woNumber);
            if (wo != null) UILoader(wo);
        }
    }

    private void searchByPhone(String phone) {
        applyCustomerFilter(workshopQueries.getCustomerIdsByPhone(phone));
    }

    private void searchByCustomerField(String value, String column) {
        applyCustomerFilter(workshopQueries.getCustomerIdsByField(column, value));
    }

    private void searchByFullName(String fullName) {
        String[] parts = fullName.split("\\s+", 2);
        if (parts.length < 2) {
            List<Integer> ids = workshopQueries.getCustomerIdsByField("first_name", parts[0]);
            ids.addAll(workshopQueries.getCustomerIdsByField("last_name", parts[0]));
            applyCustomerFilter(ids);
        } else {
            applyCustomerFilter(workshopQueries.getCustomerIdsByFullName(parts[0], parts[1]));
        }
    }

    private void applyCustomerFilter(List<Integer> customerIds) {
        if (customerIds.isEmpty()) {
            setTableItems(FXCollections.observableArrayList());
            return;
        }
        ObservableList<WorkOrder> filtered = FXCollections.observableArrayList(
                allData.stream().filter(wo -> customerIds.contains(wo.getCustomerId())).toList()
        );
        setTableItems(filtered);
    }

    // ─── FILTER BUTTONS ─────────────────────────────────────────────────────────

    @FXML
    public void showAllOpenWO() {
        allOpenFilterEnabled = !allOpenFilterEnabled;
        if (allOpenFilterEnabled) {
            oldNewFilterEnabled = false;
            repairedNotBilledFilterEnabled = false;
            myWoFilterEnabled = false;
            updateOldNewButtonCount();
            updateRepairedNotBilledButtonCount();
            updateMyWoButtonCount();
            btnOldNew.setStyle("");
            btnRepairedNotPaid.setStyle("");
            btnShowMyWO.setStyle("");
        }
        if (allOpenFilterEnabled) {
            ObservableList<WorkOrder> filtered = FXCollections.observableArrayList(
                    allData.stream().filter(this::isOpenWO).toList());
            setTableItems(filtered);
            btnAllWO.setText("SHOWING ALL OPEN WO: " + filtered.size());
        } else {
            showDashboardItems();
            updateAllOpenWOButtonCount();
        }
    }

    @FXML
    public void showOldNewOver10() {
        oldNewFilterEnabled = !oldNewFilterEnabled;
        if (oldNewFilterEnabled) {
            allOpenFilterEnabled = false;
            repairedNotBilledFilterEnabled = false;
            myWoFilterEnabled = false;
            updateAllOpenWOButtonCount();
            updateRepairedNotBilledButtonCount();
            updateMyWoButtonCount();
            btnRepairedNotPaid.setStyle("");
            btnShowMyWO.setStyle("");
        }
        if (oldNewFilterEnabled) {
            ObservableList<WorkOrder> filtered = FXCollections.observableArrayList(
                    allData.stream()
                            .filter(wo -> isStatusNew(wo.getStatus()))
                            .filter(wo -> ageDays(wo) > 10)
                            .toList());
            setTableItems(filtered);
            btnOldNew.setText("SHOWING OLD NEW WO (>10d): " + filtered.size());
            btnOldNew.setStyle("-fx-background-color: rgba(255,0,0,0.35);");
        } else {
            showDashboardItems();
            updateOldNewButtonCount();
        }
    }

    @FXML
    public void showRepairedNotPaid() {
        repairedNotBilledFilterEnabled = !repairedNotBilledFilterEnabled;
        if (repairedNotBilledFilterEnabled) {
            allOpenFilterEnabled = false;
            oldNewFilterEnabled = false;
            myWoFilterEnabled = false;
            updateAllOpenWOButtonCount();
            updateOldNewButtonCount();
            updateMyWoButtonCount();
            btnOldNew.setStyle("");
            btnShowMyWO.setStyle("");
        }
        if (repairedNotBilledFilterEnabled) {
            ObservableList<WorkOrder> filtered = FXCollections.observableArrayList(
                    allData.stream().filter(wo -> isStatusComplete(wo.getStatus())).toList());
            setTableItems(filtered);
            btnRepairedNotPaid.setText("SHOWING REPAIRED NOT BILLED: " + filtered.size());
            btnRepairedNotPaid.setStyle("-fx-background-color: rgba(0, 120, 255, 0.35);");
        } else {
            showDashboardItems();
            updateRepairedNotBilledButtonCount();
        }
    }

    @FXML
    public void showMyWO() {
        myWoFilterEnabled = !myWoFilterEnabled;
        if (myWoFilterEnabled) {
            allOpenFilterEnabled = false;
            oldNewFilterEnabled = false;
            repairedNotBilledFilterEnabled = false;
            updateAllOpenWOButtonCount();
            updateOldNewButtonCount();
            updateRepairedNotBilledButtonCount();
            btnOldNew.setStyle("");
            btnRepairedNotPaid.setStyle("");
        }
        if (myWoFilterEnabled) {
            ObservableList<WorkOrder> filtered = FXCollections.observableArrayList(
                    allData.stream().filter(this::isMyWO).toList());
            setTableItems(filtered);
            btnShowMyWO.setText("SHOWING MY WO: " + filtered.size());
            btnShowMyWO.setStyle("-fx-background-color: rgba(160, 70, 255, 0.35);");
        } else {
            showDashboardItems();
            updateMyWoButtonCount();
        }
    }

    // ─── BUTTON COUNTS ──────────────────────────────────────────────────────────

    private void updateAllOpenWOButtonCount() {
        if (btnAllWO == null) return;
        btnAllWO.setText("ALL OPEN WO: " + countAllOpenWO());
    }

    private void updateOldNewButtonCount() {
        if (btnOldNew == null) return;
        int count = countOldNewOver10();
        btnOldNew.setText("OLD OPENED WO: " + count);
        btnOldNew.setStyle(count > 0 ? "-fx-background-color: rgba(255,0,0,0.20);" : "");
    }

    private void updateRepairedNotBilledButtonCount() {
        if (btnRepairedNotPaid == null) return;
        int count = countRepairedNotBilled();
        btnRepairedNotPaid.setText("REPAIRED NOT BILLED: " + count);
        btnRepairedNotPaid.setStyle(count > 0 ? "-fx-background-color: rgba(0, 120, 255, 0.20);" : "");
    }

    private void updateMyWoButtonCount() {
        if (btnShowMyWO == null) return;
        int count = countMyWO();
        btnShowMyWO.setText("MY WO: " + count);
        btnShowMyWO.setStyle(count > 0 ? "-fx-background-color: rgba(160, 70, 255, 0.20);" : "");
    }

    // ─── STATUS HELPERS ─────────────────────────────────────────────────────────

    private boolean isStatusNew(String status) {
        if (status == null) return false;
        return status.trim().toLowerCase().equals("new");
    }

    private boolean isStatusComplete(String status) {
        if (status == null) return false;
        return status.trim().toLowerCase().equals("repair complete");
    }

    private boolean isStatusBillingComplete(String status) {
        if (status == null) return false;
        return status.trim().toLowerCase().equals("billing complete");
    }

    private boolean isOpenWO(WorkOrder wo) {
        if (wo == null) return false;
        String status = wo.getStatus();
        if (status == null) return false;
        String s = status.trim().toLowerCase();
        return !s.equals("repair complete") && !s.equals("billing complete");
    }

    private boolean isMyWO(WorkOrder wo) {
        if (wo == null) return false;
        if (isStatusBillingComplete(wo.getStatus())) return false;
        int myId = getLoggedTechId();
        if (myId == 0) return false;
        return wo.getTechId() == myId;
    }

    private boolean isAgingStatus(String status) {
        if (status == null) return false;
        String s = status.trim().toLowerCase();
        return s.equals("new") || s.equals("in progress") || s.equals("waiting parts");
    }

    private int countAllOpenWO()         { return (int) allData.stream().filter(this::isOpenWO).count(); }
    private int countOldNewOver10()      { return (int) allData.stream().filter(wo -> isStatusNew(wo.getStatus())).filter(wo -> ageDays(wo) > 10).count(); }
    private int countRepairedNotBilled() { return (int) allData.stream().filter(wo -> isStatusComplete(wo.getStatus())).count(); }
    private int countMyWO()              { return (int) allData.stream().filter(this::isMyWO).count(); }

    // ─── DB HELPERS ─────────────────────────────────────────────────────────────

    private int getLoggedTechId() {
        String username = LoginController.tech;
        if (username == null || username.isBlank()) return 0;
        String sql = "SELECT id FROM technician WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    private String getLoggedTechRole() {
        String username = LoginController.tech;
        if (username == null || username.isBlank()) return "";
        String sql = "SELECT role FROM technician WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("role").trim().toUpperCase();
        } catch (SQLException e) { e.printStackTrace(); }
        return "";
    }

    private long ageDays(WorkOrder wo) {
        try {
            LocalDateTime created = LocalDateTime.parse(wo.getCreatedAt(), DB_DT);
            return ChronoUnit.DAYS.between(created, LocalDateTime.now(ZoneId.systemDefault()));
        } catch (Exception ignored) { return 0; }
    }

    // ─── STATS ──────────────────────────────────────────────────────────────────

    @FXML
    public void myStats() {
        hideDashboardControls();
        personalwork.setVisible(true);
        personalwork.setManaged(true);
        techTXF.setText(LoginController.tech);
        techTable.setItems(null);
        techTable.setItems(techWorkData);
        techTable.autosizeColumns();
    }

    @FXML
    public void calculateWork() {
        LocalDate fromDate = fromDPicker.getValue();
        LocalDate toDate   = toDPicker.getValue();
        if (fromDate == null || toDate == null) return;
        if (fromDate.isAfter(toDate)) return;
        loadTechWorkByDateRange(fromDate, toDate);
    }

    private void loadTechWorkByDateRange(LocalDate fromDate, LocalDate toDate) {
        techWorkData.clear();
        String techUsername = techTXF.getText();
        if (techUsername == null || techUsername.isBlank()) {
            tEarnedTXF.setText("0.00");
            repairsTXF.setText("0");
            return;
        }
        List<TechWorkRow> rows = workshopQueries.loadTechWorkByDateRange(techUsername, fromDate, toDate);
        double totalEarned = 0.0;
        int repairsCount   = 0;
        for (TechWorkRow row : rows) {
            techWorkData.add(row);
            totalEarned += row.getLabourAmount();
            repairsCount++;
        }
        tEarnedTXF.setText(String.format("%.2f", totalEarned));
        repairsTXF.setText(String.valueOf(repairsCount));
        techTable.setItems(null);
        techTable.setItems(techWorkData);
        techTable.autosizeColumns();
    }

    // ─── TABLE SETUP ────────────────────────────────────────────────────────────

    public int insertOrderIntoDatabase(String status, String type, String model, String serialNumber,
                                       String problemDesc, int customerId, String vendorId,
                                       String warrantyNumber, double deposit) {
        return workshopQueries.insertOrderIntoDatabase(status, type, model, serialNumber,
                problemDesc, customerId, vendorId, warrantyNumber, deposit);
    }

    public void loadOrdersTable() {
        table.getItems().clear();
        table.getFilters().clear();

        MFXTableColumn<WorkOrder> workOrder   = new MFXTableColumn<>("Workorder", false);
        MFXTableColumn<WorkOrder> status      = new MFXTableColumn<>("Status", false);
        MFXTableColumn<WorkOrder> type        = new MFXTableColumn<>("Type", false);
        MFXTableColumn<WorkOrder> customerCol = new MFXTableColumn<>("Customer", false);
        MFXTableColumn<WorkOrder> date        = new MFXTableColumn<>("Date", false);

        workOrder.setColumnResizable(true);
        status.setColumnResizable(true);
        type.setColumnResizable(true);
        customerCol.setColumnResizable(true);
        date.setColumnResizable(true);

        workOrder.setMinWidth(110);
        status.setMinWidth(160);
        type.setMinWidth(200);
        customerCol.setMinWidth(160);
        date.setMinWidth(180);

        workOrder.setRowCellFactory(order   -> new MFXTableRowCell<>(WorkOrder::getWorkorderNumber));
        status.setRowCellFactory(order      -> new MFXTableRowCell<>(WorkOrder::getStatus));
        type.setRowCellFactory(order        -> new MFXTableRowCell<>(WorkOrder::getType));
        customerCol.setRowCellFactory(order -> new MFXTableRowCell<>(WorkOrder::getCustomerName));
        date.setRowCellFactory(order        -> new MFXTableRowCell<>(WorkOrder::getCreatedAt) {{ setAlignment(Pos.CENTER_RIGHT); }});

        date.setAlignment(Pos.CENTER_RIGHT);
        table.getTableColumns().addAll(workOrder, status, type, customerCol, date);

        table.getFilters().addAll(new IntegerFilter<>("Workorder", WorkOrder::getWorkorderNumber));
        table.getFilters().addAll(new StringFilter<>("Status", WorkOrder::getStatus));
        table.getFilters().addAll(new StringFilter<>("Customer", WorkOrder::getCustomerName));
        table.getFilters().addAll(new StringFilter<>("Date", WorkOrder::getCreatedAt));
        table.getFilters().addAll(new StringFilter<>("Warranty Number", WorkOrder::getWarrantyNumber));
    }

    private void loadTechStatsTable() {
        techTable.getTableColumns().clear();
        MFXTableColumn<TechWorkRow> workOrderCol    = new MFXTableColumn<>("Work Order", false);
        MFXTableColumn<TechWorkRow> typeCol         = new MFXTableColumn<>("Type", false);
        MFXTableColumn<TechWorkRow> statusCol       = new MFXTableColumn<>("Status", false);
        MFXTableColumn<TechWorkRow> labourCol       = new MFXTableColumn<>("Labour", false);
        MFXTableColumn<TechWorkRow> finishedDateCol = new MFXTableColumn<>("Finished Date", false);

        workOrderCol.setRowCellFactory(row    -> new MFXTableRowCell<>(TechWorkRow::getWorkOrderNumber));
        typeCol.setRowCellFactory(row         -> new MFXTableRowCell<>(TechWorkRow::getType));
        statusCol.setRowCellFactory(row       -> new MFXTableRowCell<>(TechWorkRow::getStatus));
        labourCol.setRowCellFactory(row       -> new MFXTableRowCell<>(r -> String.format("$%.2f", r.getLabourAmount())));
        finishedDateCol.setRowCellFactory(row -> new MFXTableRowCell<>(TechWorkRow::getFinishedDate));

        workOrderCol.setMinWidth(110);
        typeCol.setMinWidth(170);
        statusCol.setMinWidth(160);
        labourCol.setMinWidth(110);
        finishedDateCol.setMinWidth(180);

        finishedDateCol.setAlignment(Pos.CENTER);
        techTable.getTableColumns().addAll(workOrderCol, typeCol, statusCol, labourCol, finishedDateCol);
        techTable.setItems(techWorkData);
    }

    // ─── VIEW ORDER ─────────────────────────────────────────────────────────────

    private void preloadViewOrderDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/viewOrder.fxml"));
            viewOrderDialog     = loader.load();
            viewOrderController = loader.getController();
            viewOrderController.setMainController(this);
            viewOrderController.setDialogInstance(viewOrderDialog);
        } catch (IOException e) {
            throw new RuntimeException("Failed to preload viewOrder.fxml", e);
        }
    }

    public void viewOrder(MFXTableView<WorkOrder> table) {
        table.setTableRowFactory(wo -> {
            MFXTableRow<WorkOrder> row = new MFXTableRow<>(table, wo);
            applyRowStyle(row, wo);
            try { row.dataProperty().addListener((obs, oldVal, newVal) -> applyRowStyle(row, newVal)); }
            catch (Exception ignored) {}
            row.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
                if (e.getClickCount() == 2) {
                    e.consume();
                    Customer customer = workshopQueries.getCustomerById(wo.getCustomerId());
                    openWorkOrderFast(wo, customer);
                }
            });
            return row;
        });
    }

    private void applyRowStyle(MFXTableRow<WorkOrder> row, WorkOrder wo) {
        row.setStyle("");
        if (wo == null) return;
        if (isStatusBillingComplete(wo.getStatus())) {
            row.setStyle("-fx-background-color: rgba(0, 200, 0, 0.22);");
            return;
        }
        if (isStatusComplete(wo.getStatus())) {
            row.setStyle("-fx-background-color: rgba(0, 120, 255, 0.22);");
            return;
        }
        if (isAgingStatus(wo.getStatus())) {
            long days = ageDays(wo);
            if (days > 10)     { row.setStyle("-fx-background-color: rgba(255, 0, 0, 0.25);"); return; }
            else if (days > 5) { row.setStyle("-fx-background-color: rgba(255, 215, 0, 0.25);"); return; }
        }
        if (isMyWO(wo)) row.setStyle("-fx-background-color: rgba(160, 70, 255, 0.18);");
    }

    public void openWorkOrderFast(WorkOrder order, Customer co) {
        contentPane.setEffect(new GaussianBlur(4));
        contentPane.setDisable(true);
        viewOrderController.initData(order, co);
        if (!rootStack.getChildren().contains(viewOrderDialog)) {
            viewOrderDialog.setOpacity(0);
            viewOrderDialog.setScaleX(0.8);
            viewOrderDialog.setScaleY(0.8);
            rootStack.getChildren().add(viewOrderDialog);
            playShowAnimation(viewOrderDialog);
        } else {
            viewOrderDialog.toFront();
        }
    }

    private void UILoader(WorkOrder wo) {
        Task<Customer> task = new Task<>() {
            @Override
            protected Customer call() {
                return workshopQueries.getCustomerById(wo.getCustomerId());
            }
        };
        task.setOnSucceeded(ev -> {
            Customer customer = task.getValue();
            if (customer != null) {
                try {
                    openWorkOrderFast(wo, customer);
                    searchTxtField.clear();
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
        task.setOnFailed(ev -> task.getException().printStackTrace());
        new Thread(task).start();
    }

    // ─── NAVIGATION ─────────────────────────────────────────────────────────────

    @FXML
    public void signOut() throws IOException {
        LoginController.tech = null;
        Parent root = FXMLLoader.load(Main.class.getResource("/main/login.fxml"));
        Stage stage = (Stage) rootStack.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    public void createNewOrder() throws IOException {
        contentPane.setEffect(new GaussianBlur(4));
        contentPane.setDisable(true);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/newOrder.fxml"));
        MFXGenericDialog dialog = loader.load();
        NewOrderController dialogController = loader.getController();
        dialogController.setMainController(this);
        dialogController.setDialogInstance(dialog);
        dialog.setOpacity(0); dialog.setScaleX(0.8); dialog.setScaleY(0.8);
        rootStack.getChildren().add(dialog);
        playShowAnimation(dialog);
    }

    public void openWorkOrder(WorkOrder order, Customer co) throws IOException {
        contentPane.setEffect(new GaussianBlur(4));
        contentPane.setDisable(true);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/viewOrder.fxml"));
        MFXGenericDialog dialog = loader.load();
        ViewOrderController dialogController = loader.getController();
        dialogController.setMainController(this);
        dialogController.setDialogInstance(dialog);
        dialogController.initData(order, co);
        dialog.setOpacity(0); dialog.setScaleX(0.8); dialog.setScaleY(0.8);
        rootStack.getChildren().add(dialog);
        playShowAnimation(dialog);
    }

    public void openSettingsMenu() throws IOException {
        String role = getLoggedTechRole();
        if (!role.equals("ADMIN") && !role.equals("ACCOUNTANT")) return;
        contentPane.setEffect(new GaussianBlur(4));
        contentPane.setDisable(true);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/settings.fxml"));
        MFXGenericDialog dialog = loader.load();
        SettingsController dialogController = loader.getController();
        dialogController.setMainController(this);
        dialogController.setDialogInstance(dialog);
        dialog.setOpacity(0); dialog.setScaleX(0.8); dialog.setScaleY(0.8);
        rootStack.getChildren().add(dialog);
        playShowAnimation(dialog);
    }

    // ─── UI HELPERS ─────────────────────────────────────────────────────────────

    private void showDashboardControls() {
        table.setVisible(true);              table.setManaged(true);
        btnAllWO.setVisible(true);           btnAllWO.setManaged(true);
        btnOldNew.setVisible(true);          btnOldNew.setManaged(true);
        btnRepairedNotPaid.setVisible(true); btnRepairedNotPaid.setManaged(true);
        btnShowMyWO.setVisible(true);        btnShowMyWO.setManaged(true);
        searchTxtField.setVisible(true);     searchTxtField.setManaged(true);
        newOrderBTN.setVisible(true);        newOrderBTN.setManaged(true);
        personalwork.setVisible(false);      personalwork.setManaged(false);
    }

    private void hideDashboardControls() {
        table.setVisible(false);              table.setManaged(false);
        btnAllWO.setVisible(false);           btnAllWO.setManaged(false);
        btnOldNew.setVisible(false);          btnOldNew.setManaged(false);
        btnRepairedNotPaid.setVisible(false); btnRepairedNotPaid.setManaged(false);
        btnShowMyWO.setVisible(false);        btnShowMyWO.setManaged(false);
        searchTxtField.setVisible(false);     searchTxtField.setManaged(false);
        newOrderBTN.setVisible(false);        newOrderBTN.setManaged(false);
    }

    public void avatar(Circle techAvatar) {
        Image im = new Image("/avatar.png");
        techAvatar.setCenterX(50);
        techAvatar.setCenterY(50);
        techAvatar.setFill(new ImagePattern(im));
    }

    private void playShowAnimation(MFXGenericDialog dialog) {
        FadeTransition fade = new FadeTransition(Duration.millis(250), dialog);
        fade.setFromValue(0); fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(250), dialog);
        scale.setFromX(0.8); scale.setToX(1); scale.setFromY(0.8); scale.setToY(1);
        new ParallelTransition(fade, scale).play();
    }
}