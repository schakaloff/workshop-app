package utils;

import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.Languages;
import org.languagetool.rules.RuleMatch;

import java.util.List;

public class SpellCheckUtil {

    private static final Language EN_US = Languages.getLanguageForShortCode("en-US");

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
        // Store matches on the field via user data
        field.textProperty().addListener((obs, oldText, newText) -> checkAndStore(field, newText));

        // Intercept right-click to build a merged context menu
        field.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() != MouseButton.SECONDARY) return;
            e.consume(); // prevent MFX from showing its own menu

            @SuppressWarnings("unchecked")
            List<RuleMatch> matches = (List<RuleMatch>) field.getUserData();

            ContextMenu menu = buildMergedMenu(field, matches);
            menu.show(field, e.getScreenX(), e.getScreenY());
        });
    }

    // ─── Attach to TextArea ───────────────────────────────────────────────────────

// ─── Attach to TextArea ───────────────────────────────────────────────────────

    public static void attach(TextArea field) {
        suppressDefaultMenu(field);  // ← ADD THIS
        field.textProperty().addListener((obs, oldText, newText) -> checkAndStoreTextArea(field, newText));

        field.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() != MouseButton.SECONDARY) return;
            e.consume();

            @SuppressWarnings("unchecked")
            List<RuleMatch> matches = (List<RuleMatch>) field.getUserData();

            ContextMenu menu = buildMergedMenuTextArea(field, matches);
            menu.show(field, e.getScreenX(), e.getScreenY());
        });
    }

// ─── Suppress JavaFX's built-in context menu on any TextArea ─────────────────

    private static void suppressDefaultMenu(TextArea field) {
        // JavaFX installs the default Cut/Copy/Paste menu lazily on first right-click.
        // Setting an empty context menu beforehand blocks that entirely.
        field.setContextMenu(new ContextMenu());
    }

    // ─── Check and store matches on MFXTextField ─────────────────────────────────

    private static void checkAndStore(MFXTextField field, String text) {
        if (text == null || text.isBlank()) {
            Platform.runLater(() -> {
                field.setUserData(null);
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
                    field.setUserData(matches.isEmpty() ? null : matches);
                    if (matches.isEmpty()) clearError(field);
                    else field.setStyle(field.getStyle() + "-fx-border-color: red; -fx-border-width: 1.5px;");
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ─── Check and store matches on TextArea ─────────────────────────────────────

    private static void checkAndStoreTextArea(TextArea field, String text) {
        if (text == null || text.isBlank()) {
            Platform.runLater(() -> {
                field.setUserData(null);
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
                    field.setUserData(matches.isEmpty() ? null : matches);
                    if (matches.isEmpty()) clearError(field);
                    else field.setStyle(field.getStyle() + "-fx-border-color: red; -fx-border-width: 1.5px;");
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ─── Build merged menu for MFXTextField ──────────────────────────────────────

    private static ContextMenu buildMergedMenu(MFXTextField field, List<RuleMatch> matches) {
        ContextMenu menu = new ContextMenu();

        // ── Standard edit actions ──
        MenuItem cut = new MenuItem("Cut");
        cut.setOnAction(e -> field.cut());

        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(e -> field.copy());

        MenuItem paste = new MenuItem("Paste");
        paste.setOnAction(e -> field.paste());

        MenuItem selectAll = new MenuItem("Select All");
        selectAll.setOnAction(e -> field.selectAll());

        menu.getItems().addAll(cut, copy, paste, selectAll);

        // ── Spell-check suggestions ──
        if (matches != null && !matches.isEmpty()) {
            menu.getItems().add(new SeparatorMenuItem());
            appendSpellItems(menu, field, matches);
        }

        return menu;
    }

    // ─── Build merged menu for TextArea ──────────────────────────────────────────

    private static ContextMenu buildMergedMenuTextArea(TextArea field, List<RuleMatch> matches) {
        ContextMenu menu = new ContextMenu();

        MenuItem cut = new MenuItem("Cut");
        cut.setOnAction(e -> field.cut());

        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(e -> field.copy());

        MenuItem paste = new MenuItem("Paste");
        paste.setOnAction(e -> field.paste());

        MenuItem selectAll = new MenuItem("Select All");
        selectAll.setOnAction(e -> field.selectAll());

        menu.getItems().addAll(cut, copy, paste, selectAll);

        if (matches != null && !matches.isEmpty()) {
            menu.getItems().add(new SeparatorMenuItem());
            appendSpellItems(menu, field, matches);
        }

        return menu;
    }

    // ─── Shared spell-check item builder ─────────────────────────────────────────

    private static void appendSpellItems(ContextMenu menu, TextInputControl field, List<RuleMatch> matches) {
        for (RuleMatch match : matches) {
            MenuItem errorLabel = new MenuItem("⚠ " + match.getMessage());
            errorLabel.setDisable(true);
            menu.getItems().add(errorLabel);

            List<String> suggestions = match.getSuggestedReplacements();
            if (suggestions.isEmpty()) {
                MenuItem none = new MenuItem("  No suggestions");
                none.setDisable(true);
                menu.getItems().add(none);
            } else {
                for (String suggestion : suggestions.subList(0, Math.min(5, suggestions.size()))) {
                    MenuItem item = new MenuItem("  ✔ " + suggestion);
                    final int from = match.getFromPos();
                    final int to   = match.getToPos();
                    item.setOnAction(e -> {
                        String current = field.getText();
                        if (to <= current.length()) {
                            field.setText(current.substring(0, from) + suggestion + current.substring(to));
                            field.positionCaret(from + suggestion.length());
                        }
                    });
                    menu.getItems().add(item);
                }
            }
            menu.getItems().add(new SeparatorMenuItem());
        }
    }




    // ─── Clear error state ────────────────────────────────────────────────────────

    private static void clearError(TextInputControl field) {
        field.setStyle(field.getStyle()
                .replace("-fx-border-color: red;", "")
                .replace("-fx-border-width: 1.5px;", ""));
        field.setUserData(null);
    }
}