package Controllers;
import Controllers.DbRepo.WorkshopQueries;
import DB.DbConfig;
import Skeletons.Customer;
import Skeletons.TechWorkRow;
import Skeletons.WorkOrder;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.controls.MFXPaginatedTableView;
import io.github.palexdev.materialfx.controls.MFXTableColumn;
import io.github.palexdev.materialfx.controls.MFXTableRow;
import io.github.palexdev.materialfx.controls.MFXTableView;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.filter.IntegerFilter;
import io.github.palexdev.materialfx.filter.StringFilter;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
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

    private boolean oldNewFilterEnabled = false;
    private boolean repairedNotBilledFilterEnabled = false;
    private boolean myWoFilterEnabled = false;
    private boolean allOpenFilterEnabled = false;

    private final ObservableList<TechWorkRow> techWorkData = FXCollections.observableArrayList();
    private final WorkshopQueries workshopQueries = new WorkshopQueries();
    private static final DateTimeFormatter DB_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final ObservableList<WorkOrder> data = FXCollections.observableArrayList();
    private final ObservableList<WorkOrder> allData = FXCollections.observableArrayList();

    private MFXGenericDialog viewOrderDialog;
    private ViewOrderController viewOrderController;

    public void initialize() {
        welcomeTech.setText(LoginController.tech);
        avatar(techAvatar);
        personalwork.setVisible(false);
        personalwork.setManaged(false);
        techTXF.setText(LoginController.tech);
        loadTechStatsTable();
        table.setRowsPerPage(15);
        LoadOrders();
        fromDPicker.setValue(LocalDate.now().withDayOfMonth(1));
        toDPicker.setValue(LocalDate.now());
        preloadViewOrderDialog();
        searchTxtField.setOnAction(e -> onSearchEnter());
    }

    public void LoadOrders() {
        showDashboardControls();
        table.getTableColumns().clear();
        table.getItems().clear();
        loadOrdersTable();
        viewOrder(table);
        loadOrdersIntoTable();
        allData.setAll(data);
        table.setItems(allData);
        updateAllOpenWOButtonCount();
        updateOldNewButtonCount();
        updateRepairedNotBilledButtonCount();
        updateMyWoButtonCount();
    }

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
        LocalDate toDate = toDPicker.getValue();
        if (fromDate == null || toDate == null) {
            return;
        }
        if (fromDate.isAfter(toDate)) {
            return;
        }
        loadTechWorkByDateRange(fromDate, toDate);
    }

    public void onSearchEnter() {
        String text = searchTxtField.getText();
        if (text == null || text.isBlank()) return;
        int woNumber;
        try {
            woNumber = Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            return;
        }
        WorkOrder wo = allData.stream().filter(w -> w.getWorkorderNumber() == woNumber).findFirst().orElse(null);
        UILoader(wo);
    }

    @FXML
    public void signOut() throws IOException {
        LoginController.tech = null;
        Parent root = FXMLLoader.load(Main.class.getResource("/main/login.fxml"));
        Stage stage = (Stage) rootStack.getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        task.setOnFailed(ev -> task.getException().printStackTrace());
        Thread t = new Thread(task);
        t.start();
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
        int repairsCount = 0;
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

    public void loadOrdersIntoTable() {
        data.setAll(workshopQueries.loadOrdersIntoTable());
    }

    public int insertOrderIntoDatabase(String status, String type, String model, String serialNumber, String problemDesc, int customerId, String vendorId, String warrantyNumber, double deposit) {
        return workshopQueries.insertOrderIntoDatabase(
                status,
                type,
                model,
                serialNumber,
                problemDesc,
                customerId,
                vendorId,
                warrantyNumber,
                deposit
        );
    }

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
            ObservableList<WorkOrder> filtered = FXCollections.observableArrayList(allData.stream().filter(this::isOpenWO).toList());
            table.setItems(filtered);
            btnAllWO.setText("SHOWING ALL OPEN WO: " + filtered.size());
        } else {
            table.setItems(allData);
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
            ObservableList<WorkOrder> filtered = FXCollections.observableArrayList(allData.stream().filter(wo -> isStatusNew(wo.getStatus())).filter(wo -> ageDays(wo) > 10).toList());
            table.setItems(filtered);
            btnOldNew.setText("SHOWING OLD NEW WO (>10d): " + filtered.size());
            btnOldNew.setStyle("-fx-background-color: rgba(255,0,0,0.35);");
        } else {
            table.setItems(allData);
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
            ObservableList<WorkOrder> filtered = FXCollections.observableArrayList(allData.stream().filter(wo -> isStatusComplete(wo.getStatus())).toList());
            table.setItems(filtered);
            btnRepairedNotPaid.setText("SHOWING REPAIRED NOT BILLED: " + filtered.size());
            btnRepairedNotPaid.setStyle("-fx-background-color: rgba(0, 120, 255, 0.35);");
        } else {
            table.setItems(allData);
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
                    allData.stream()
                            .filter(this::isMyWO)
                            .toList()
            );
            table.setItems(filtered);
            btnShowMyWO.setText("SHOWING MY WO: " + filtered.size());
            btnShowMyWO.setStyle("-fx-background-color: rgba(160, 70, 255, 0.35);");
        } else {
            table.setItems(allData);
            updateMyWoButtonCount();
        }
    }

    private void updateAllOpenWOButtonCount() {
        if (btnAllWO == null) return;
        int count = countAllOpenWO();
        btnAllWO.setText("ALL OPEN WO: " + count);
    }

    private void updateOldNewButtonCount() {
        if (btnOldNew == null) return;
        int count = countOldNewOver10();
        btnOldNew.setText("OLD OPENED WO: " + count);
        if (count > 0) {
            btnOldNew.setStyle("-fx-background-color: rgba(255,0,0,0.20);");
        } else {
            btnOldNew.setStyle("");
        }
    }

    private void updateRepairedNotBilledButtonCount() {
        if (btnRepairedNotPaid == null) return;
        int count = countRepairedNotBilled();
        btnRepairedNotPaid.setText("REPAIRED NOT BILLED: " + count);
        if (count > 0) {
            btnRepairedNotPaid.setStyle("-fx-background-color: rgba(0, 120, 255, 0.20);");
        } else {
            btnRepairedNotPaid.setStyle("");
        }
    }

    private void updateMyWoButtonCount() {
        if (btnShowMyWO == null) return;
        int count = countMyWO();
        btnShowMyWO.setText("MY WO: " + count);
        if (count > 0) {
            btnShowMyWO.setStyle("-fx-background-color: rgba(160, 70, 255, 0.20);");
        } else {
            btnShowMyWO.setStyle("");
        }
    }

    private boolean isStatusNew(String status) {
        if (status == null) return false;
        String s = status.trim().toLowerCase();
        return s.equals("new");
    }

    private boolean isStatusComplete(String status) {
        if (status == null) return false;
        String s = status.trim().toLowerCase();
        return s.equals("repair complete");
    }

    private boolean isStatusBillingComplete(String status) {
        if (status == null) return false;
        String s = status.trim().toLowerCase();
        return s.equals("billing complete");
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

    private int countAllOpenWO() {
        return (int) allData.stream().filter(this::isOpenWO).count();
    }

    private int countOldNewOver10() {
        return (int) allData.stream().filter(wo -> isStatusNew(wo.getStatus())).filter(wo -> ageDays(wo) > 10).count();
    }

    private int countRepairedNotBilled() {
        return (int) allData.stream().filter(wo -> isStatusComplete(wo.getStatus())).count();
    }

    private int countMyWO() {
        return (int) allData.stream().filter(this::isMyWO).count();
    }

    private int getLoggedTechId() {
        String username = LoginController.tech;
        if (username == null || username.isBlank()) return 0;
        String sql = "SELECT id FROM technician WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private long ageDays(WorkOrder wo) {
        try {
            LocalDateTime created = LocalDateTime.parse(wo.getCreatedAt(), DB_DT);
            LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
            return ChronoUnit.DAYS.between(created, now);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void preloadViewOrderDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/viewOrder.fxml"));
            viewOrderDialog = loader.load();
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
            try {
                row.dataProperty().addListener((obs, oldVal, newVal) -> applyRowStyle(row, newVal));
            } catch (Exception ignored) {
            }
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
            if (days > 10) {
                row.setStyle("-fx-background-color: rgba(255, 0, 0, 0.25);");
                return;
            } else if (days > 5) {
                row.setStyle("-fx-background-color: rgba(255, 215, 0, 0.25);");
                return;
            }
        }
        if (isMyWO(wo)) {
            row.setStyle("-fx-background-color: rgba(160, 70, 255, 0.18);");
        }
    }

    public void loadOrdersTable() {
        MFXTableColumn<WorkOrder> workOrder = new MFXTableColumn<>("Workorder", false);
        MFXTableColumn<WorkOrder> status = new MFXTableColumn<>("Status", false);
        MFXTableColumn<WorkOrder> type = new MFXTableColumn<>("Type", false);
        MFXTableColumn<WorkOrder> date = new MFXTableColumn<>("Date", false);

        workOrder.setColumnResizable(true);
        status.setColumnResizable(true);
        type.setColumnResizable(true);
        date.setColumnResizable(true);

        workOrder.setMinWidth(110);
        status.setMinWidth(160);
        type.setMinWidth(260);
        date.setMinWidth(180);

        workOrder.setRowCellFactory(order -> new MFXTableRowCell<>(WorkOrder::getWorkorderNumber));
        status.setRowCellFactory(order -> new MFXTableRowCell<>(WorkOrder::getStatus));
        type.setRowCellFactory(order -> new MFXTableRowCell<>(WorkOrder::getType));
        date.setRowCellFactory(order -> new MFXTableRowCell<>(WorkOrder::getCreatedAt) {{ setAlignment(Pos.CENTER_RIGHT); }});

        date.setAlignment(Pos.CENTER_RIGHT);
        table.getTableColumns().addAll(workOrder, status, type, date);

        table.getFilters().addAll(new IntegerFilter<>("Workorder", WorkOrder::getWorkorderNumber));
        table.getFilters().addAll(new StringFilter<>("Status", WorkOrder::getStatus));
        table.getFilters().addAll(new StringFilter<>("Date", WorkOrder::getCreatedAt));
        table.getFilters().addAll(new StringFilter<>("Warranty Number", WorkOrder::getWarrantyNumber));
    }

    private void loadTechStatsTable() {
        techTable.getTableColumns().clear();
        MFXTableColumn<TechWorkRow> workOrderCol = new MFXTableColumn<>("Work Order", false);
        MFXTableColumn<TechWorkRow> typeCol = new MFXTableColumn<>("Type", false);
        MFXTableColumn<TechWorkRow> statusCol = new MFXTableColumn<>("Status", false);
        MFXTableColumn<TechWorkRow> labourCol = new MFXTableColumn<>("Labour", false);
        MFXTableColumn<TechWorkRow> finishedDateCol = new MFXTableColumn<>("Finished Date", false);

        workOrderCol.setRowCellFactory(row -> new MFXTableRowCell<>(TechWorkRow::getWorkOrderNumber));
        typeCol.setRowCellFactory(row -> new MFXTableRowCell<>(TechWorkRow::getType));
        statusCol.setRowCellFactory(row -> new MFXTableRowCell<>(TechWorkRow::getStatus));
        labourCol.setRowCellFactory(row -> new MFXTableRowCell<>(r -> String.format("$%.2f", r.getLabourAmount())));
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

    private void showDashboardControls() {
        table.setVisible(true);
        table.setManaged(true);
        btnAllWO.setVisible(true);
        btnAllWO.setManaged(true);
        btnOldNew.setVisible(true);
        btnOldNew.setManaged(true);
        btnRepairedNotPaid.setVisible(true);
        btnRepairedNotPaid.setManaged(true);
        btnShowMyWO.setVisible(true);
        btnShowMyWO.setManaged(true);
        searchTxtField.setVisible(true);
        searchTxtField.setManaged(true);
        newOrderBTN.setVisible(true);
        newOrderBTN.setManaged(true);
        personalwork.setVisible(false);
        personalwork.setManaged(false);
    }

    private void hideDashboardControls() {
        table.setVisible(false);
        table.setManaged(false);
        btnAllWO.setVisible(false);
        btnAllWO.setManaged(false);
        btnOldNew.setVisible(false);
        btnOldNew.setManaged(false);
        btnRepairedNotPaid.setVisible(false);
        btnRepairedNotPaid.setManaged(false);
        btnShowMyWO.setVisible(false);
        btnShowMyWO.setManaged(false);
        searchTxtField.setVisible(false);
        searchTxtField.setManaged(false);
        newOrderBTN.setVisible(false);
        newOrderBTN.setManaged(false);
    }

    public void createNewOrder() throws IOException {
        contentPane.setEffect(new GaussianBlur(4));
        contentPane.setDisable(true);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/newOrder.fxml"));
        MFXGenericDialog dialog = loader.load();
        NewOrderController dialogController = loader.getController();
        dialogController.setMainController(this);
        dialogController.setDialogInstance(dialog);
        dialog.setOpacity(0);
        dialog.setScaleX(0.8);
        dialog.setScaleY(0.8);
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
        dialog.setOpacity(0);
        dialog.setScaleX(0.8);
        dialog.setScaleY(0.8);
        rootStack.getChildren().add(dialog);
        playShowAnimation(dialog);
    }

    public void openSettingsMenu() throws IOException {
        contentPane.setEffect(new GaussianBlur(4));
        contentPane.setDisable(true);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/settings.fxml"));
        MFXGenericDialog dialog = loader.load();
        SettingsController dialogController = loader.getController();
        dialogController.setMainController(this);
        dialogController.setDialogInstance(dialog);
        dialog.setOpacity(0);
        dialog.setScaleX(0.8);
        dialog.setScaleY(0.8);
        rootStack.getChildren().add(dialog);
        playShowAnimation(dialog);
    }

    public void avatar(Circle techAvatar) {
        Image im = new Image("/avatar.png");
        techAvatar.setCenterX(50);
        techAvatar.setCenterY(50);
        techAvatar.setFill(new ImagePattern(im));
    }

    private void playShowAnimation(MFXGenericDialog dialog) {
        FadeTransition fade = new FadeTransition(Duration.millis(250), dialog);
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(Duration.millis(250), dialog);
        scale.setFromX(0.8);
        scale.setToX(1);
        scale.setFromY(0.8);
        scale.setToY(1);

        ParallelTransition pt = new ParallelTransition(fade, scale);
        pt.play();
    }
}