package Controllers;

import Skeletons.WorkOrder;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class PrinterController {
    @FXML private Label lblWorkorderNumber;
    @FXML private Label lblCreatedAt;
    @FXML private Label lblModel;
    @FXML private Label lblSerialNumber;
    @FXML private TextArea ProblemDesc;

    public void initData(WorkOrder wo){
        lblWorkorderNumber.setText("WO#: "     + wo.getWorkorderNumber());
        lblCreatedAt.setText       ("Created: "+ wo.getCreatedAt());
        lblModel.setText           ("Model: "  + wo.getModel());
        lblSerialNumber.setText    ("SN: "     + wo.getSerialNumber());
        ProblemDesc.setText     ("Problem: "+ wo.getProblemDesc());
    }
}
