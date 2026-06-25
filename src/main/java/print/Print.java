package print;

import Skeletons.Customer;
import Skeletons.WorkOrder;
import javafx.fxml.FXMLLoader;
import javafx.print.*;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.transform.Scale;
import javafx.stage.Window;

public class Print {
    public static void printWorkOrder(WorkOrder wo, Customer co, Window owner) throws Exception {
        FXMLLoader loader = new FXMLLoader(Print.class.getResource("/main/printOrder.fxml"));
        Parent printNode = loader.load();

        Controllers.PrinterController pc = loader.getController();
        pc.initData(wo, co);

        printNode.applyCss();
        printNode.layout();

        Printer printer = Printer.getDefaultPrinter();
        if (printer == null) {
            System.out.println("No default printer.");
            return;
        }

        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null || !job.showPrintDialog(owner)) {
            return;
        }

        // Use HARDWARE_MINIMUM margins so printable area is maximized
        PageLayout pageLayout = printer.createPageLayout(
                Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.HARDWARE_MINIMUM);

        double printableWidth = pageLayout.getPrintableWidth();
        double printableHeight = pageLayout.getPrintableHeight();

        // Get the ACTUAL rendered size of the node
        double nodeWidth = printNode.getBoundsInLocal().getWidth();
        double nodeHeight = printNode.getBoundsInLocal().getHeight();

        System.out.println("Node: " + nodeWidth + " x " + nodeHeight);
        System.out.println("Printable: " + printableWidth + " x " + printableHeight);

        double scaleX = printableWidth / nodeWidth;
        double scaleY = printableHeight / nodeHeight;
        double scale = Math.min(scaleX, scaleY);

        Group scaledNode = new Group(printNode);
        scaledNode.getTransforms().add(new Scale(scale, scale));

        // Tell the job the scaled size
        job.getJobSettings().setPageLayout(pageLayout);

        boolean success = job.printPage(pageLayout, scaledNode);
        scaledNode.getTransforms().clear();

        if (success)
            job.endJob();
        System.out.println("WO print success = " + success);
    }

    // public static void printFinalInvoice(WorkOrder wo, Customer co, String
    // method, double amount, String tech, String date, Window owner) throws
    // Exception {
    // FXMLLoader loader = new
    // FXMLLoader(Print.class.getResource("/main/invoice.fxml"));
    // Parent printNode = loader.load();
    //
    // Controllers.InvoiceController pc = loader.getController();
    // pc.initData(wo, co, method, amount, tech, date);
    //
    // printNode.applyCss();
    // printNode.layout();
    //
    // Printer printer = Printer.getDefaultPrinter();
    // if (printer == null) {
    // System.out.println("No default printer.");
    // return;
    // }
    //
    // PrinterJob job = PrinterJob.createPrinterJob(printer);
    // if (job == null || !job.showPrintDialog(owner)) return;
    //
    // PageLayout pageLayout = printer.createPageLayout(Paper.A4,
    // PageOrientation.PORTRAIT, Printer.MarginType.HARDWARE_MINIMUM);
    //
    // Group scaledNode = new Group(printNode);
    //
    // double nodeWidth = printNode.getBoundsInParent().getWidth();
    // double nodeHeight = printNode.getBoundsInParent().getHeight();
    //
    // double printableWidth = pageLayout.getPrintableWidth();
    // double printableHeight = pageLayout.getPrintableHeight();
    //
    // double scaleX = printableWidth / nodeWidth;
    // double scaleY = printableHeight / nodeHeight;
    // double scale = Math.min(scaleX, scaleY);
    //
    // scaledNode.getTransforms().add(new Scale(scale, scale));
    //
    // boolean success = job.printPage(pageLayout, scaledNode);
    // scaledNode.getTransforms().clear();
    //
    // if (success) job.endJob();
    //
    // }
}
