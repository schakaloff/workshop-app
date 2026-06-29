package Controllers;

import Skeletons.Customer;
import Skeletons.WorkOrder;
import io.github.palexdev.materialfx.controls.MFXCheckbox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.stage.Window;
import utils.DocumentOutput;
import utils.SftpClient;
import DB.DbConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AdditionalDepositController {

    private ActualWorkshopController mainController;
    private WorkOrder currentWorkOrder;
    private Customer  currentCustomer;
    private Window    ownerWindow;

    public void setMainController(ActualWorkshopController controller) {
        this.mainController = controller;
    }

    // ─── FXML fields ────────────────────────────────────────────────────────────

    @FXML private MFXTextField techIDTXF;
    @FXML private MFXTextField dateTXF;

    @FXML private MFXTextField customerNameTXF;
    @FXML private MFXTextField customerAddressTXF;
    @FXML private MFXTextField customerCityTXF;
    @FXML private MFXTextField customerPhoneTXF;

    @FXML private MFXTextField poNumberTXF;
    @FXML private MFXTextField warrantyAddressTXF;
    @FXML private MFXTextField contractTXF;

    @FXML private MFXCheckbox visaCB;
    @FXML private MFXCheckbox debitCB;
    @FXML private MFXCheckbox masterCardCB;
    @FXML private MFXCheckbox cashCB;
    @FXML private MFXCheckbox amexCB;

    @FXML private MFXTextField visaTXF;
    @FXML private MFXTextField debitTXF;
    @FXML private MFXTextField masterCardTXF;
    @FXML private MFXTextField cashTXF;
    @FXML private MFXTextField amexTXF;

    // ─── INITIALIZE ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        techIDTXF.setText(LoginController.tech);
        dateTXF.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        visaTXF.setDisable(true);
        debitTXF.setDisable(true);
        masterCardTXF.setDisable(true);
        cashTXF.setDisable(true);
        amexTXF.setDisable(true);
    }

    // ─── CONTEXT ────────────────────────────────────────────────────────────────

    public void setContext(WorkOrder wo, Customer co, Window owner) {
        this.currentWorkOrder = wo;
        this.currentCustomer  = co;
        this.ownerWindow      = owner;

        customerNameTXF.setText(co.getFirstName() + " " + co.getLastName());
        customerAddressTXF.setText(co.getAddress() != null ? co.getAddress() : "");
        customerCityTXF.setText(co.getTown() != null ? co.getTown() : "");
        customerPhoneTXF.setText(co.getPhone() != null ? co.getPhone() : "");
    }

    // ─── PAYMENT TYPE SELECTION ─────────────────────────────────────────────────

    @FXML
    public void paymentTypeSelection(ActionEvent e) {
        MFXCheckbox cb = (MFXCheckbox) e.getSource();
        boolean selected = cb.isSelected();

        if (selected) selectOnly(cb);

        switch (cb.getId()) {
            case "visaCB"       -> { visaTXF.setDisable(!selected);       if (!selected) visaTXF.clear(); }
            case "debitCB"      -> { debitTXF.setDisable(!selected);      if (!selected) debitTXF.clear(); }
            case "masterCardCB" -> { masterCardTXF.setDisable(!selected); if (!selected) masterCardTXF.clear(); }
            case "cashCB"       -> { cashTXF.setDisable(!selected);       if (!selected) cashTXF.clear(); }
            case "amexCB"       -> { amexTXF.setDisable(!selected);       if (!selected) amexTXF.clear(); }
        }
    }

    private void selectOnly(MFXCheckbox selected) {
        for (MFXCheckbox cb : new MFXCheckbox[]{visaCB, debitCB, masterCardCB, cashCB, amexCB}) {
            if (cb != selected) cb.setSelected(false);
        }
        visaTXF.setDisable(true);
        debitTXF.setDisable(true);
        masterCardTXF.setDisable(true);
        cashTXF.setDisable(true);
        amexTXF.setDisable(true);
        visaTXF.clear();
        debitTXF.clear();
        masterCardTXF.clear();
        cashTXF.clear();
        amexTXF.clear();
    }

    // ─── PAY ────────────────────────────────────────────────────────────────────

    @FXML
    public void pay() {
        try {
            String method = getSelectedMethod();
            double amount = getEnteredAmount(method);
            String tech   = techIDTXF.getText();
            String date   = dateTXF.getText();

            String title = "ADDITIONAL_DEPOSIT_" + currentWorkOrder.getWorkorderNumber();

            byte[] pdfBytes = DocumentOutput.generatePdfBytes(
                    "/main/firstInvoice.fxml",
                    loader -> {
                        FirstInvoiceController ic = loader.getController();
                        ic.initData(currentWorkOrder, currentCustomer, method, amount, tech, date);
                    }
            );

            attachPdfToWorkOrder(pdfBytes, title + ".pdf");
            addToDepositAmount(amount);

            DocumentOutput.printOrPdf(
                    title,
                    "/main/firstInvoice.fxml",
                    loader -> {
                        FirstInvoiceController ic = loader.getController();
                        ic.initData(currentWorkOrder, currentCustomer, method, amount, tech, date);
                    },
                    ownerWindow
            );

            closeWindow();

        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    // ─── HELPERS ────────────────────────────────────────────────────────────────

    private String getSelectedMethod() {
        if (visaCB.isSelected())       return "VISA";
        if (debitCB.isSelected())      return "DEBIT";
        if (masterCardCB.isSelected()) return "MASTERCARD";
        if (cashCB.isSelected())       return "CASH";
        if (amexCB.isSelected())       return "AMEX";
        throw new IllegalArgumentException("Select a payment method.");
    }

    private double getEnteredAmount(String method) {
        String raw = switch (method) {
            case "VISA"       -> visaTXF.getText();
            case "DEBIT"      -> debitTXF.getText();
            case "MASTERCARD" -> masterCardTXF.getText();
            case "CASH"       -> cashTXF.getText();
            case "AMEX"       -> amexTXF.getText();
            default -> throw new IllegalArgumentException("Unknown method.");
        };
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("Enter an amount.");
        raw = raw.replace("$", "").replace("CAD", "").trim();
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount must be a number, e.g. 50 or 50.00");
        }
    }

    private void addToDepositAmount(double amount) throws Exception {
        String sql = "UPDATE work_order SET deposit_amount = deposit_amount + ? WHERE workorder = ?";
        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, currentWorkOrder.getWorkorderNumber());
            ps.executeUpdate();
        }
    }

    private void attachPdfToWorkOrder(byte[] pdfBytes, String displayName) throws Exception {
        java.io.File tmp = java.io.File.createTempFile("pdf_", ".pdf");
        tmp.deleteOnExit();
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) {
            fos.write(pdfBytes);
        }

        String remotePath;
        try (java.io.FileInputStream fis = new java.io.FileInputStream(tmp)) {
            remotePath = SftpClient.upload(currentWorkOrder.getWorkorderNumber(), displayName, fis);
        }

        String sql = "INSERT INTO work_order_files (workorder_id, file_name, file_path) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentWorkOrder.getWorkorderNumber());
            ps.setString(2, displayName);
            ps.setString(3, remotePath);
            ps.executeUpdate();
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) techIDTXF.getScene().getWindow();
        stage.close();
    }
}
