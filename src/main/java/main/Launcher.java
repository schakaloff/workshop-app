package main;

import java.nio.file.Files;
import java.nio.file.Path;

public class Launcher {

    public static void main(String[] args) throws Exception {
        // Just launch the UpdateScreen — it handles everything
        UpdateScreen.main(args);
    }

    public static Path resolveLibDir() {
        try {
            Path jarLocation = Path.of(
                    Launcher.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).getParent();

            Path libDir = jarLocation.resolve("lib");
            if (isWritable(libDir)) return libDir;
        } catch (Exception ignored) {}

        return getFallbackDir();
    }

    private static boolean isWritable(Path path) {
        try {
            Files.createDirectories(path);
            Path testFile = path.resolve(".write_test");
            Files.writeString(testFile, "test");
            Files.delete(testFile);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Path getFallbackDir() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                return Path.of(appData, "workordermanager", "lib");
            }
        } else if (os.contains("mac")) {
            return Path.of(System.getProperty("user.home"),
                    "Library", "Application Support", "workordermanager", "lib");
        }

        String xdg = System.getenv("XDG_DATA_HOME");
        if (xdg != null && !xdg.isBlank()) {
            return Path.of(xdg, "workordermanager", "lib");
        }

        return Path.of(System.getProperty("user.home"),
                ".local", "share", "workordermanager", "lib");
    }
}