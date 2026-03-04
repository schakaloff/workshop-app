package utils;

import Controllers.PrinterOutputController;
import io.github.palexdev.materialfx.utils.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.print.*;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.WritableImage;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import utils.enums.OutputChoice;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;


public class DocumentOutput {
    public interface InitFn {
        void init(FXMLLoader loader) throws Exception;
    }
    public interface PdfSavedFn {
        void onSaved(File pdfFile) throws Exception;
    }

    public static void printOrPdf(String title, String fxmlPath, InitFn initFn, Window owner) throws Exception {
        OutputChoice choice = showChoiceDialog(owner);
        if (choice == OutputChoice.CANCEL) return;

        FXMLLoader loader = new FXMLLoader(DocumentOutput.class.getResource(fxmlPath));
        Parent node = loader.load();

        initFn.init(loader);

        node.applyCss();
        node.layout();

        if (choice == OutputChoice.PRINTER) {
            printNode(node, owner);
        } else if (choice == OutputChoice.PDF) {
            exportNodeToPdf(node, owner, title);
        }
    }

    public static void printOrPdf(
            String title,
            String fxmlPath,
            InitFn initFn,
            Window owner,
            PdfSavedFn pdfSavedFn // NEW
    ) throws Exception {
        OutputChoice choice = showChoiceDialog(owner);
        if (choice == OutputChoice.CANCEL) return;

        FXMLLoader loader = new FXMLLoader(DocumentOutput.class.getResource(fxmlPath));
        Parent node = loader.load();
        initFn.init(loader);

        node.applyCss();
        node.layout();

        if (choice == OutputChoice.PRINTER) {
            printNode(node, owner);
        } else if (choice == OutputChoice.PDF) {
            File saved = exportNodeToPdf(node, owner, title); // now returns File
            if (saved != null && pdfSavedFn != null) {
                pdfSavedFn.onSaved(saved);
            }
        }
    }

    private static OutputChoice showChoiceDialog(Window owner) throws Exception {
        FXMLLoader loader = new FXMLLoader(DocumentOutput.class.getResource("/main/printOption.fxml"));
        Parent root = loader.load();
        PrinterOutputController controller = loader.getController();
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.setTitle("Output");
        stage.setScene(new Scene(root));
        stage.showAndWait();
        return controller.getResult();
    }

    private static void printNode(Parent node, Window owner) {
        Printer printer = Printer.getDefaultPrinter();
        if (printer == null) {
            new Alert(Alert.AlertType.ERROR, "No default printer found.").showAndWait();
            return;
        }

        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null || !job.showPrintDialog(owner)) return;

        PageLayout layout = printer.createPageLayout(
                Paper.A4,
                PageOrientation.PORTRAIT,
                Printer.MarginType.HARDWARE_MINIMUM
        );

        Group scaled = new Group(node);

        double nodeW = node.getBoundsInParent().getWidth();
        double nodeH = node.getBoundsInParent().getHeight();

        double scale = Math.min(layout.getPrintableWidth() / nodeW, layout.getPrintableHeight() / nodeH);
        scaled.getTransforms().add(new Scale(scale, scale));

        boolean ok = job.printPage(layout, scaled);

        scaled.getTransforms().clear();
        if (ok) job.endJob();
    }

    private static File exportNodeToPdf(Parent node, Window owner, String suggestedName) throws Exception {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        String safeName = (suggestedName == null ? "document" : suggestedName)
                .replaceAll("[^a-zA-Z0-9-_ ]", "")
                .trim();

        if (safeName.isBlank()) safeName = "document";
        fc.setInitialFileName(safeName + ".pdf");

        File file = fc.showSaveDialog(owner);
        if (file == null) return null;

        WritableImage fxImage = node.snapshot(null, null);
        BufferedImage bimg = SwingFXUtils.fromFXImage(fxImage, null);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            var pdImage = LosslessFactory.createFromImage(doc, bimg);

            float pageW = page.getMediaBox().getWidth();
            float pageH = page.getMediaBox().getHeight();

            float imgW = bimg.getWidth();
            float imgH = bimg.getHeight();

            float scale = Math.min(pageW / imgW, pageH / imgH);
            float drawW = imgW * scale;
            float drawH = imgH * scale;

            float x = (pageW - drawW) / 2f;
            float y = (pageH - drawH) / 2f;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.drawImage(pdImage, x, y, drawW, drawH);
            }

            doc.save(file);
        }

        return file;
    }

    private static byte[] nodeToPdfBytes(Parent node) throws Exception {
        WritableImage fxImage = node.snapshot(null, null);
        BufferedImage bimg = SwingFXUtils.fromFXImage(fxImage, null);

        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            var pdImage = LosslessFactory.createFromImage(doc, bimg);

            float pageW = page.getMediaBox().getWidth();
            float pageH = page.getMediaBox().getHeight();

            float imgW = bimg.getWidth();
            float imgH = bimg.getHeight();

            float scale = Math.min(pageW / imgW, pageH / imgH);
            float drawW = imgW * scale;
            float drawH = imgH * scale;

            float x = (pageW - drawW) / 2f;
            float y = (pageH - drawH) / 2f;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.drawImage(pdImage, x, y, drawW, drawH);
            }

            doc.save(out);
            return out.toByteArray();
        }
    }

    public static byte[] generatePdfBytes(String fxmlPath, InitFn initFn) throws Exception {
        FXMLLoader loader = new FXMLLoader(DocumentOutput.class.getResource(fxmlPath));
        Parent node = loader.load();

        initFn.init(loader);

        node.applyCss();
        node.layout();

        return nodeToPdfBytes(node);
    }


}