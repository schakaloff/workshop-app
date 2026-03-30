package utils;

import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.Languages;
import org.languagetool.rules.RuleMatch;

import java.util.List;

public class SpellCheckUtil {

    // ─── Create language once, reuse across threads ───────────────────────────────
    private static final Language EN_US = Languages.getLanguageForShortCode("en-US");

    // ─── One JLanguageTool per thread (not thread safe per docs) ─────────────────
    private static final ThreadLocal<JLanguageTool> LANG_TOOL = ThreadLocal.withInitial(() -> {
        try {
            return new JLanguageTool(EN_US);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    });

    // ─── Attach to MFXTextField ──────────────────────────────────────────────────

    public static void attach(MFXTextField field) {
        field.textProperty().addListener((obs, oldText, newText) -> check(field, newText));
    }

    // ─── Attach to TextArea ───────────────────────────────────────────────────────

    public static void attach(TextArea field) {
        field.textProperty().addListener((obs, oldText, newText) -> check(field, newText));
    }

    // ─── Core check logic ────────────────────────────────────────────────────────

    private static void check(TextInputControl field, String text) {
        if (text == null || text.isBlank()) {
            Platform.runLater(() -> {
                clearError(field);
            });
            return;
        }

        Thread t = new Thread(() -> {
            try {
                JLanguageTool lt = LANG_TOOL.get();
                if (lt == null) return;

                List<RuleMatch> matches = lt.check(text);

                Platform.runLater(() -> {
                    if (matches.isEmpty()) {
                        clearError(field);
                    } else {
                        // Red border on the field
                        field.setStyle(field.getStyle()
                                + "-fx-border-color: red; -fx-border-width: 1.5px;");

                        // Build context menu with suggestions per match
                        ContextMenu menu = new ContextMenu();

                        for (RuleMatch match : matches) {
                            // Error description — disabled so it acts as a label
                            MenuItem errorLabel = new MenuItem("⚠ " + match.getMessage());
                            errorLabel.setDisable(true);
                            menu.getItems().add(errorLabel);

                            List<String> suggestions = match.getSuggestedReplacements();
                            if (suggestions.isEmpty()) {
                                MenuItem none = new MenuItem("  No suggestions");
                                none.setDisable(true);
                                menu.getItems().add(none);
                            } else {
                                for (String suggestion : suggestions.subList(
                                        0, Math.min(5, suggestions.size()))) {
                                    MenuItem item = new MenuItem("  ✔ " + suggestion);
                                    final int from = match.getFromPos();
                                    final int to   = match.getToPos();
                                    item.setOnAction(e -> {
                                        String current = field.getText();
                                        if (to <= current.length()) {
                                            field.setText(
                                                    current.substring(0, from)
                                                            + suggestion
                                                            + current.substring(to));
                                            field.positionCaret(from + suggestion.length());
                                        }
                                    });
                                    menu.getItems().add(item);
                                }
                            }
                            menu.getItems().add(new SeparatorMenuItem());
                        }

                        field.setContextMenu(menu);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        t.setDaemon(true);
        t.start();
    }

    // ─── Clear error state ───────────────────────────────────────────────────────

    private static void clearError(TextInputControl field) {
        field.setStyle(field.getStyle()
                .replace("-fx-border-color: red;", "")
                .replace("-fx-border-width: 1.5px;", ""));
        field.setContextMenu(null);
    }
}