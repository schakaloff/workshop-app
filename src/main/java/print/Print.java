package print;

import Skeletons.Customer;
import Skeletons.WorkOrder;
import javafx.fxml.FXMLLoader;
import javafx.print.*;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.transform.Scale;
import javafx.stage.Window;

public class Print {
    public static void printWorkOrder(WorkOrder wo, Customer co, Window owner) throws Exception {
        // Load the FXML
        FXMLLoader loader = new FXMLLoader(Print.class.getResource("/main/printOrder.fxml"));
        Parent printNode = loader.load();

        // Initialize with your data
        Controllers.PrinterController pc = loader.getController();
        pc.initData(wo, co);

        // Force layout and CSS
        printNode.applyCss();
        printNode.layout();

        // Create printer job and layout
        Printer printer = Printer.getDefaultPrinter();
        if (printer == null) {
            System.out.println("No default printer.");
            return;
        }

        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null || !job.showPrintDialog(owner)) {
            return;
        }

        PageLayout pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.HARDWARE_MINIMUM);

        // Wrap the FXML node in a Group
        Group scaledNode = new Group(printNode);

        // Get original bounds (after layout)
        double nodeWidth = printNode.getBoundsInParent().getWidth();
        double nodeHeight = printNode.getBoundsInParent().getHeight();

        // Printable area size
        double printableWidth = pageLayout.getPrintableWidth();
        double printableHeight = pageLayout.getPrintableHeight();

        // Calculate scale to fit page
        double scaleX = printableWidth / nodeWidth;
        double scaleY = printableHeight / nodeHeight;
        double scale = Math.min(scaleX, scaleY); // maintain aspect ratio

        // Apply scaling transform
        Scale scaling = new Scale(scale, scale);
        scaledNode.getTransforms().add(scaling);

        // Print the scaled Group
        boolean success = job.printPage(pageLayout, scaledNode);

        // Clean up
        scaledNode.getTransforms().clear();
        if (success) {
            job.endJob();
        }
    }
}
