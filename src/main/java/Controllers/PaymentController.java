package Controllers;
import Skeletons.Customer;
import Skeletons.WorkOrder;
import io.github.palexdev.materialfx.controls.MFXCheckbox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.stage.Window;
import print.Print;
import utils.InvoiceType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class PaymentController {
    private ActualWorkshopController mainController;

    public WorkOrder currentWorkOrder;
    public Customer currentCustomer;

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
    }

    @FXML
    public void paymentTypeSelection(ActionEvent e) {
        MFXCheckbox cb = (MFXCheckbox) e.getSource();
        boolean selected = cb.isSelected();

        switch (cb.getId()) {
            case "visaCB":
                visaTXF.setDisable(!selected);
                if (selected) visaTXF.setText("CDN$");
                else visaTXF.clear();
                break;

            case "debitCB":
                debitTXF.setDisable(!selected);
                if (selected) debitTXF.setText("CDN$");
                else debitTXF.clear();
                break;

            case "masterCardCB":
                masterCardTXF.setDisable(!selected);
                if (selected) masterCardTXF.setText("CDN$");
                else masterCardTXF.clear();
                break;

            case "cashCB":
                cashTXF.setDisable(!selected);
                if (selected) cashTXF.setText("CDN$");
                else cashTXF.clear();
                break;
        }
    }

    @FXML
    public void pay() {
        try {
            String method = getSelectedMethod();     // implement below
            double amount = getEnteredAmount(method); // implement below

            String tech = techIDTXF.getText();
            String date = dateTXF.getText();

            if (invoiceType == InvoiceType.DEPOSIT) {
                //Print.printFirstPayment(currentWorkOrder, currentCustomer, method, amount, tech, date, ownerWindow);
                String fxml = (invoiceType == InvoiceType.DEPOSIT)
                        ? "/main/firstInvoice.fxml"
                        : "/main/invoice.fxml";

                String title = (invoiceType == InvoiceType.DEPOSIT)
                        ? "Deposit Invoice WO " + currentWorkOrder.getWorkorderNumber()
                        : "Final Invoice WO " + currentWorkOrder.getWorkorderNumber();

                utils.DocumentOutput.printOrPdf(
                        title,
                        fxml,
                        loader -> {
                            if (invoiceType == InvoiceType.DEPOSIT) {
                                FirstInvoiceController ic = loader.getController();
                                ic.initData(currentWorkOrder, currentCustomer, method, amount, tech, date);
                            } else {
                                InvoiceController ic = loader.getController();
                                //ic.initData(currentWorkOrder, currentCustomer, method, amount, tech, date);
                            }
                        },
                        ownerWindow
                );
            } else {
                //Print.printFinalInvoice(currentWorkOrder, currentCustomer, method, amount, tech, date, ownerWindow);
            }

            // close payment window
            ((Stage) techIDTXF.getScene().getWindow()).close();

        } catch (Exception ex) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    ex.getMessage()).showAndWait();
        }


    }

    private String getSelectedMethod() {
        if (visaCB.isSelected()) return "VISA";
        if (debitCB.isSelected()) return "DEBIT";
        if (masterCardCB.isSelected()) return "MASTERCARD";
        if (cashCB.isSelected()) return "CASH";
        throw new IllegalArgumentException("Select a payment method.");
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

        // your text fields include "CDN$" so strip it
        raw = raw.replace("CDN$", "").replace("$", "").trim();

        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount must be a number, e.g. 50 or 50.00");
        }
    }



}
