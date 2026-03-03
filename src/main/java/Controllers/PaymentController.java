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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


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

        // default suggestion
        if (type == InvoiceType.DEPOSIT) {
            this.suggestedAmount = wo.getDepositAmount();
        }
        // if FINAL, suggestedAmount will be set by ViewOrderController via setSuggestedAmount()
    }

    private void autofill(MFXTextField field) {
        if (suggestedAmount == null) return;

        String t = field.getText();
        String normalized = (t == null) ? "" : t.replace(" ", "").trim();

        boolean emptyish = normalized.isBlank() || normalized.equalsIgnoreCase("CDN$");

        if (emptyish) {
            field.setText(String.format("CDN$%.2f", suggestedAmount));
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
                    visaTXF.setText("CDN$");
                    autofill(visaTXF);
                } else visaTXF.clear();
                break;

            case "debitCB":
                debitTXF.setDisable(!selected);
                if (selected){
                    debitTXF.setText("CDN$");
                    autofill(debitTXF);
                }
                else debitTXF.clear();
                break;

            case "masterCardCB":
                masterCardTXF.setDisable(!selected);
                if (selected) {
                    masterCardTXF.setText("CDN$");
                    autofill(masterCardTXF);

                }
                else masterCardTXF.clear();
                break;

            case "cashCB":
                cashTXF.setDisable(!selected);
                if (selected) {
                    cashTXF.setText("CDN$");
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

            ((Stage) techIDTXF.getScene().getWindow()).close();

        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private void selectOnly(MFXCheckbox selected) {
        for (MFXCheckbox cb : new MFXCheckbox[]{visaCB, debitCB, masterCardCB, cashCB}) {
            if (cb != selected) cb.setSelected(false);
        }
        // disable all fields first
        visaTXF.setDisable(true); debitTXF.setDisable(true); masterCardTXF.setDisable(true); cashTXF.setDisable(true);
        // clear all fields
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

        raw = raw.replace("CDN$", "").replace("$", "").trim();

        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount must be a number, e.g. 50 or 50.00");
        }
    }



}
