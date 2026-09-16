package utils;

import java.awt.Desktop;
import java.net.URI;

public class LinkOpener {

    // Desktop.browse() is a blocking native call — if the OS has no properly
    // configured browser handler (common on minimal/headless-ish Linux setups)
    // it can hang indefinitely instead of failing fast. Calling it straight
    // from a click handler on the JavaFX Application Thread freezes the whole
    // UI for as long as it hangs, since nothing else can run on that thread
    // until it returns. Always run it on its own daemon thread instead, so a
    // hang there can never take the app down with it.
    public static void open(String url) {
        Thread t = new Thread(() -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create(url));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }
}
