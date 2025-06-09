package print;

import Skeletons.WorkOrder;
import javafx.fxml.FXMLLoader;
import javafx.print.PrinterJob;
import javafx.scene.Parent;
import javafx.stage.Window;

public class Print {
    public static void printWorkOrder(WorkOrder wo, Window owner) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                Print.class.getResource("/main/printOrder.fxml")
        );
        Parent printNode = loader.load();

        // initialize the labels in your PrintController
        Controllers.PrinterController pc = loader.getController();
        pc.initData(wo);

        // force CSS/layout so it actually has size
        printNode.applyCss();
        printNode.layout();

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) return;

        if (!job.showPrintDialog(owner)) {
            job.endJob();
            return;
        }

        if (job.printPage(printNode)) {
            job.endJob();
        }
    }
}
