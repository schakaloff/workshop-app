package Controllers;

import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import utils.enums.OutputChoice;

public class PrinterOutputController {

    @FXML private MFXGenericDialog dialogRoot;

    private OutputChoice result = OutputChoice.CANCEL;

    public OutputChoice getResult() {
        return result;
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