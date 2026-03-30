package utils;

import Controllers.PrinterOutputController;
import Controllers.PrintRepairController;
import io.github.palexdev.materialfx.utils.SwingFXUtils;
import javafx.application.Platform;
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
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
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

    // ─── Public entry points ─────────────────────────────────────────────────────

    public static void printOrPdf(String title, String fxmlPath, InitFn initFn, Window owner) throws Exception {
        OutputChoice choice = showChoiceDialog(owner);
        if (choice == OutputChoice.CANCEL) return;

        FXMLLoader loader = new FXMLLoader(DocumentOutput.class.getResource(fxmlPath));
        Parent node = loader.load();
        initFn.init(loader);

        Stage hidden = showHiddenStage(node);

        Platform.runLater(() -> {
            try {
                // Check if controller has overflow parts for page 2
                Parent page2 = null;
                Object controller = loader.getController();
                if (controller instanceof PrintRepairController) {
                    page2 = ((PrintRepairController) controller).buildPage2();
                }

                if (choice == OutputChoice.PRINTER) {
                    printNodes(owner, node, page2);
                } else if (choice == OutputChoice.PDF) {
                    exportNodesToPdf(node, page2, owner, title);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                hidden.close();
            }
        });
    }

    public static void printOrPdf(
            String title,
            String fxmlPath,
            InitFn initFn,
            Window owner,
            PdfSavedFn pdfSavedFn
    ) throws Exception {
        OutputChoice choice = showChoiceDialog(owner);
        if (choice == OutputChoice.CANCEL) return;

        FXMLLoader loader = new FXMLLoader(DocumentOutput.class.getResource(fxmlPath));
        Parent node = loader.load();
        initFn.init(loader);

        Stage hidden = showHiddenStage(node);

        Platform.runLater(() -> {
            try {
                if (choice == OutputChoice.PRINTER) {
                    printNodes(owner, node, null);
                } else if (choice == OutputChoice.PDF) {
                    File saved = exportNodesToPdf(node, null, owner, title);
                    if (saved != null && pdfSavedFn != null) pdfSavedFn.onSaved(saved);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                hidden.close();
            }
        });
    }

    public static byte[] generatePdfBytes(String fxmlPath, InitFn initFn) throws Exception {
        FXMLLoader loader = new FXMLLoader(DocumentOutput.class.getResource(fxmlPath));
        Parent node = loader.load();
        initFn.init(loader);
        Stage hidden = showHiddenStage(node);
        try {
            return nodeToPdfBytes(node);
        } finally {
            hidden.close();
        }
    }

    // ─── Hidden stage ────────────────────────────────────────────────────────────

    private static Stage showHiddenStage(Parent node) {
        Stage hidden = new Stage();
        hidden.setOpacity(0);
        hidden.setWidth(595);
        hidden.setHeight(842);
        hidden.setScene(new Scene(node, 595, 842));
        hidden.show();
        node.applyCss();
        node.layout();
        return hidden;
    }

    // ─── Choice dialog ───────────────────────────────────────────────────────────

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

    // ─── Print ───────────────────────────────────────────────────────────────────

    private static void printNodes(Window owner, Parent page1, Parent page2) {
        Printer printer = Printer.getDefaultPrinter();
        if (printer == null) {
            new Alert(Alert.AlertType.ERROR, "No default printer found.").showAndWait();
            return;
        }

        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null || !job.showPrintDialog(owner)) return;

        PageLayout layout = printer.createPageLayout(
                Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.HARDWARE_MINIMUM);

        printScaled(job, layout, page1);

        if (page2 != null) {
            Stage hidden2 = showHiddenStage(page2);
            printScaled(job, layout, page2);
            hidden2.close();
        }

        job.endJob();
    }

    private static void printScaled(PrinterJob job, PageLayout layout, Parent node) {
        Group scaled = new Group(node);
        double scale = Math.min(
                layout.getPrintableWidth()  / node.getBoundsInParent().getWidth(),
                layout.getPrintableHeight() / node.getBoundsInParent().getHeight()
        );
        scaled.getTransforms().add(new Scale(scale, scale));
        job.printPage(layout, scaled);
        scaled.getTransforms().clear();
    }

    // ─── PDF export ──────────────────────────────────────────────────────────────

    private static File exportNodesToPdf(Parent page1, Parent page2,
                                         Window owner, String suggestedName) throws Exception {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        String safeName = (suggestedName == null ? "document" : suggestedName)
                .replaceAll("[^a-zA-Z0-9-_ ]", "").trim();
        if (safeName.isBlank()) safeName = "document";
        fc.setInitialFileName(safeName + ".pdf");

        File file = fc.showSaveDialog(owner);
        if (file == null) return null;

        try (PDDocument doc = new PDDocument()) {
            appendPageToPdf(doc, page1);
            if (page2 != null) {
                Stage hidden2 = showHiddenStage(page2);
                appendPageToPdf(doc, page2);
                hidden2.close();
            }
            doc.save(file);
        }

        return file;
    }

    private static void appendPageToPdf(PDDocument doc, Parent node) throws Exception {
        WritableImage fxImage = node.snapshot(null, null);
        BufferedImage bimg = SwingFXUtils.fromFXImage(fxImage, null);

        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);

        var pdImage = LosslessFactory.createFromImage(doc, bimg);

        float pageW = page.getMediaBox().getWidth();
        float pageH = page.getMediaBox().getHeight();
        float scale = Math.min(pageW / bimg.getWidth(), pageH / bimg.getHeight());
        float drawW = bimg.getWidth() * scale;
        float drawH = bimg.getHeight() * scale;
        float x = (pageW - drawW) / 2f;
        float y = (pageH - drawH) / 2f;

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            cs.drawImage(pdImage, x, y, drawW, drawH);
        }
    }

    // ─── PDF bytes ───────────────────────────────────────────────────────────────

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
            float scale = Math.min(pageW / bimg.getWidth(), pageH / bimg.getHeight());
            float drawW = bimg.getWidth() * scale;
            float drawH = bimg.getHeight() * scale;
            float x = (pageW - drawW) / 2f;
            float y = (pageH - drawH) / 2f;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.drawImage(pdImage, x, y, drawW, drawH);
            }

            doc.save(out);
            return out.toByteArray();
        }
    }
}