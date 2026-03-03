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

    public void setMainController(ActualWorkshopController controller){this.mainController = controller;}

    @FXML private MFXTextField techIDTXF;
    @FXML private MFXTextField dateTXF;

    @FXML private MFXCheckbox visaCB;
    @FXML private MFXCheckbox debitCB;
    @FXML private MFXCheckbox masterCardCB;
    @FXML private MFXCheckbox cashCB;


    @FXML private MFXTextField visaTXF;
    @FXML private MFXTextField debitTXF;
    @FXML private MFXTextField masterCardTXF;
    @FXML private MFXTextField cashTXF;

    private InvoiceType invoiceType;
    private Window ownerWindow;

    @FXML
    public void initialize() {
        techIDTXF.setText(LoginController.tech);
        dateTXF.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        visaTXF.setDisable(true);
        debitTXF.setDisable(true);
        masterCardTXF.setDisable(true);
        cashTXF.setDisable(true);
    }

    public void setContext(WorkOrder wo, Customer co, InvoiceType type, Window owner) {
        this.currentWorkOrder = wo;
        this.currentCustomer = co;
        this.invoiceType = type;
        this.ownerWindow = owner;
        if (type == InvoiceType.DEPOSIT) {
            this.suggestedAmount = wo.getDepositAmount();
        }
    }

    @FXML
    public void paymentTypeSelection(ActionEvent e) {
        MFXCheckbox cb = (MFXCheckbox) e.getSource();
        boolean selected = cb.isSelected();

        if (cb.isSelected()) {
            selectOnly(cb);
        }

        switch (cb.getId()) {
            case "visaCB":
                visaTXF.setDisable(!selected);
                if (selected) {
                    autofill(visaTXF);
                } else {
                    visaTXF.clear();
                }
                break;

            case "debitCB":
                debitTXF.setDisable(!selected);
                if (selected){
                    autofill(debitTXF);
                }
                else debitTXF.clear();
                break;

            case "masterCardCB":
                masterCardTXF.setDisable(!selected);
                if (selected) {
                    autofill(masterCardTXF);
                }
                else masterCardTXF.clear();
                break;

            case "cashCB":
                cashTXF.setDisable(!selected);
                if (selected) {
                    autofill(cashTXF);
                }
                else cashTXF.clear();
                break;
        }
    }

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
                fxml = "/main/firstInvoice.fxml";
                title = "Deposit Invoice WO " + currentWorkOrder.getWorkorderNumber();
            } else if (invoiceType == InvoiceType.FINAL) {
                fxml = "/main/invoiceSheet.fxml";
                title = "Final Invoice WO " + currentWorkOrder.getWorkorderNumber();
            } else {
                throw new IllegalStateException("Invoice type not set.");
            }

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

    private void selectOnly(MFXCheckbox selected) {
        for (MFXCheckbox cb : new MFXCheckbox[]{visaCB, debitCB, masterCardCB, cashCB}) {
            if (cb != selected){
                cb.setSelected(false);
            }
        }
        visaTXF.setDisable(true); debitTXF.setDisable(true); masterCardTXF.setDisable(true); cashTXF.setDisable(true);
        visaTXF.clear(); debitTXF.clear(); masterCardTXF.clear(); cashTXF.clear();
    }

    private String getSelectedMethod() {
        if (visaCB.isSelected()) return "VISA";
        if (debitCB.isSelected()) return "DEBIT";
        if (masterCardCB.isSelected()) return "MASTERCARD";
        if (cashCB.isSelected()) return "CASH";
        throw new IllegalArgumentException("Select a payment method.");
    }

    public void setSuggestedAmount(Double amount) {
        this.suggestedAmount = amount;
    }

    private double getEnteredAmount(String method) {
        String raw;
        switch (method) {
            case "VISA": raw = visaTXF.getText(); break;
            case "DEBIT": raw = debitTXF.getText(); break;
            case "MASTERCARD": raw = masterCardTXF.getText(); break;
            case "CASH": raw = cashTXF.getText(); break;
            default: throw new IllegalArgumentException("Unknown method.");
        }

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
        String base = nf.format(amount);
        return base + " CAD";
    }

    private void autofill(MFXTextField field) {
        if (suggestedAmount == null) {
            return;
        }

        String text = field.getText();

        if (text == null) {
            text = "";
        }

        text = text.trim();

        if (text.equals("") || text.equals("$") || text.equalsIgnoreCase("CAD")) {
            field.setText(formatCad(suggestedAmount));
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) techIDTXF.getScene().getWindow();
        stage.close();
    }
}
