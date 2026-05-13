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
import utils.enums.InvoiceType;
import DB.DbConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PaymentController {

    private ActualWorkshopController mainController;

    public WorkOrder currentWorkOrder;
    public Customer currentCustomer;

    private Double suggestedAmount;
    private Double totalAmount;
    private InvoiceType invoiceType;
    private Window ownerWindow;

    public void setMainController(ActualWorkshopController controller) {
        this.mainController = controller;
    }

    // ─── FXML fields ────────────────────────────────────────────────────────────

    @FXML private MFXTextField techIDTXF;
    @FXML private MFXTextField dateTXF;

    // Customer info
    @FXML private MFXTextField customerNameTXF;
    @FXML private MFXTextField customerAddressTXF;
    @FXML private MFXTextField customerCityTXF;
    @FXML private MFXTextField customerPhoneTXF;

    // PO / Warranty / Contract
    @FXML private MFXTextField poNumberTXF;
    @FXML private MFXTextField warrantyAddressTXF;
    @FXML private MFXTextField contractTXF;

    // Payment checkboxes
    @FXML private MFXCheckbox visaCB;
    @FXML private MFXCheckbox debitCB;
    @FXML private MFXCheckbox masterCardCB;
    @FXML private MFXCheckbox cashCB;
    @FXML private MFXCheckbox amexCB;

    // Payment amount fields
    @FXML private MFXTextField visaTXF;
    @FXML private MFXTextField debitTXF;
    @FXML private MFXTextField masterCardTXF;
    @FXML private MFXTextField cashTXF;
    @FXML private MFXTextField amexTXF;

    // Deposit
    @FXML private MFXCheckbox depositCB;
    @FXML private MFXTextField depositTXF;


    private double grossTotal; // labour + parts + taxes, before deposit

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

    public void setContext(WorkOrder wo, Customer co, InvoiceType type, Window owner) {
        this.currentWorkOrder = wo;
        this.currentCustomer  = co;
        this.invoiceType      = type;
        this.ownerWindow      = owner;

        // populate customer fields
        customerNameTXF.setText(co.getFirstName() + " " + co.getLastName());
        customerAddressTXF.setText(co.getAddress() != null ? co.getAddress() : "");
        customerCityTXF.setText(co.getTown() != null ? co.getTown() : "");
        customerPhoneTXF.setText(co.getPhone() != null ? co.getPhone() : "");

        if (type == InvoiceType.DEPOSIT) {
            this.totalAmount     = wo.getDepositAmount();
            this.suggestedAmount = wo.getDepositAmount();
            depositCB.setSelected(true);
            depositTXF.setDisable(false);
            depositTXF.setText(formatCad(wo.getDepositAmount()));
        } else if (type == InvoiceType.FINAL) {
            boolean hasDeposit = wo.getDepositAmount() > 0;
            // totalAmount from setSuggestedAmount = labour+parts+taxes - deposit
            // gross = totalAmount + deposit
            double gross = totalAmount + wo.getDepositAmount();
            depositCB.setSelected(hasDeposit);
            depositTXF.setDisable(!hasDeposit);
            depositTXF.setText(formatCad(wo.getDepositAmount()));
            // store gross so depositSelected can recalculate correctly
            this.grossTotal = gross;
        }
    }


    public void setSuggestedAmount(Double amount) {
        this.suggestedAmount = amount;           // net (after deposit)
        this.totalAmount     = amount;           // net (after deposit)
        this.grossTotal      = amount + 0;       // will be corrected in setContext
    }

    // ─── DEPOSIT ────────────────────────────────────────────────────────────────

    @FXML
    public void depositSelected() {
        boolean checked = depositCB.isSelected();
        depositTXF.setDisable(!checked);

        if (!checked) {
            depositTXF.clear();
            suggestedAmount = grossTotal;
        } else {
            depositTXF.setText(formatCad(currentWorkOrder.getDepositAmount()));
            suggestedAmount = grossTotal - currentWorkOrder.getDepositAmount();
        }

        refreshActivePaymentField();
    }

    private void refreshActivePaymentField() {
        if (visaCB.isSelected())       { visaTXF.clear();       autofill(visaTXF); }
        if (debitCB.isSelected())      { debitTXF.clear();      autofill(debitTXF); }
        if (masterCardCB.isSelected()) { masterCardTXF.clear(); autofill(masterCardTXF); }
        if (cashCB.isSelected())       { cashTXF.clear();       autofill(cashTXF); }
        if (amexCB.isSelected())       { amexTXF.clear();       autofill(amexTXF); }
    }

    // ─── PAYMENT TYPE SELECTION ─────────────────────────────────────────────────

    @FXML
    public void paymentTypeSelection(ActionEvent e) {
        MFXCheckbox cb = (MFXCheckbox) e.getSource();
        boolean selected = cb.isSelected();

        if (selected) {
            selectOnly(cb);
        }

        switch (cb.getId()) {
            case "visaCB" -> {
                visaTXF.setDisable(!selected);
                if (selected) autofill(visaTXF); else visaTXF.clear();
            }
            case "debitCB" -> {
                debitTXF.setDisable(!selected);
                if (selected) autofill(debitTXF); else debitTXF.clear();
            }
            case "masterCardCB" -> {
                masterCardTXF.setDisable(!selected);
                if (selected) autofill(masterCardTXF); else masterCardTXF.clear();
            }
            case "cashCB" -> {
                cashTXF.setDisable(!selected);
                if (selected) autofill(cashTXF); else cashTXF.clear();
            }
            case "amexCB" -> {
                amexTXF.setDisable(!selected);
                if (selected) autofill(amexTXF); else amexTXF.clear();
            }
        }
    }

    private void selectOnly(MFXCheckbox selected) {
        for (MFXCheckbox cb : new MFXCheckbox[]{visaCB, debitCB, masterCardCB, cashCB, amexCB}) {
            if (cb != selected) {
                cb.setSelected(false);
            }
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

            String tech = techIDTXF.getText();
            String date = dateTXF.getText();

            String fxml;
            String title;

            if (invoiceType == InvoiceType.DEPOSIT) {
                fxml  = "/main/firstInvoice.fxml";
                title = "DEPOSIT_INVOICE_" + currentWorkOrder.getWorkorderNumber();
            } else if (invoiceType == InvoiceType.FINAL) {
                fxml  = "/main/invoiceSheet.fxml";
                title = "FINAL_INVOICE_" + currentWorkOrder.getWorkorderNumber();
            } else {
                throw new IllegalStateException("Invoice type not set.");
            }

            // generate PDF and attach to work order files
            byte[] pdfBytes = DocumentOutput.generatePdfBytes(
                    fxml,
                    loader -> {
                        if (invoiceType == InvoiceType.DEPOSIT) {
                            FirstInvoiceController ic = loader.getController();
                            ic.initData(currentWorkOrder, currentCustomer, method, amount, tech, date);
                        } else {
                            InvoiceController ic = loader.getController();
                            ic.initData(currentWorkOrder, currentCustomer, method, amount, tech, date);
                        }
                    }
            );

            attachPdfToWorkOrder(pdfBytes, title + ".pdf");

            // print / save PDF for customer
            DocumentOutput.printOrPdf(
                    title,
                    fxml,
                    loader -> {
                        if (invoiceType == InvoiceType.DEPOSIT) {
                            FirstInvoiceController ic = loader.getController();
                            ic.initData(currentWorkOrder, currentCustomer, method, amount, tech, date);
                        } else {
                            InvoiceController ic = loader.getController();
                            ic.initData(currentWorkOrder, currentCustomer, method, amount, tech, date);
                        }
                    },
                    ownerWindow
            );

            if (invoiceType == InvoiceType.FINAL) {
                updateWoStatus("Billing Complete");
            }

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

    private String formatCad(double amount) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.CANADA);
        return nf.format(amount) + " CAD";
    }

    private void autofill(MFXTextField field) {
        if (suggestedAmount == null) return;
        String text = field.getText();
        if (text == null) text = "";
        text = text.trim();
        if (text.isEmpty() || text.equals("$") || text.equalsIgnoreCase("CAD")) {
            field.setText(formatCad(suggestedAmount));
        }
    }

    private void updateWoStatus(String newStatus) {
        String sql = "UPDATE work_order SET status = ? WHERE workorder = ?";
        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, currentWorkOrder.getWorkorderNumber());
            ps.executeUpdate();
            currentWorkOrder.setStatus(newStatus);
            if (mainController != null) {
                mainController.LoadOrders();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void attachPdfToWorkOrder(byte[] pdfBytes, String displayName) throws Exception {
        String sql = "INSERT INTO work_order_files (workorder_id, file_name, file_data) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentWorkOrder.getWorkorderNumber());
            ps.setString(2, displayName);
            ps.setBytes(3, pdfBytes);
            ps.executeUpdate();
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) techIDTXF.getScene().getWindow();
        stage.close();
    }
}