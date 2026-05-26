package Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class PrintOptionsController {

    @FXML private Label woLabel;

    private ViewOrderController viewOrderController;
    private Stage stage;

    // ─── SETUP ──────────────────────────────────────────────────────────────────

    public void setViewOrderController(ViewOrderController controller) {
        this.viewOrderController = controller;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setWoNumber(int woNumber) {
        woLabel.setText("WO #" + woNumber);
    }

    // ─── BUTTON ACTIONS ─────────────────────────────────────────────────────────

    @FXML
    public void onWorkOrder() {
        close();
        try { viewOrderController.printOrder(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void onEstimate() {
        close();
        try { viewOrderController.printEstimate(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void onRepairComplete() {
        close();
        try { viewOrderController.printRepaired(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // ─── HELPER ─────────────────────────────────────────────────────────────────

    private void close() {
        if (stage != null) stage.close();
    }
}