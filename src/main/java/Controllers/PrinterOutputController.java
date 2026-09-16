package Controllers;

import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import utils.enums.OutputChoice;

public class PrinterOutputController {

    @FXML private MFXGenericDialog dialogRoot;
    @FXML private Label docLabel;

    private OutputChoice result = OutputChoice.CANCEL;

    public OutputChoice getResult() {
        return result;
    }

    // Tells the user exactly what's about to be printed/saved (e.g. "Print
    // Work Order #124055") — the dialog used to just ask "how would you like
    // to output this document?" with no indication of which document.
    public void setDocumentTitle(String title) {
        if (title != null && !title.isBlank()) {
            docLabel.setText(title);
        }
    }

    @FXML
    private void Printer() {
        result = OutputChoice.PRINTER;
        close();
    }

    @FXML
    private void PDF() {
        result = OutputChoice.PDF;
        close();
    }

    @FXML
    private void Cancel() {
        result = OutputChoice.CANCEL;
        close();
    }

    private void close() {
        ((Stage) dialogRoot.getScene().getWindow()).close();
    }
}