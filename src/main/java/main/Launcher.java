package main;

import org.update4j.Configuration;

import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class Launcher {

    private static final String CONFIG_URL =
            "https://github.com/schakaloff/workshop-app/releases/latest/download/config.xml";

    public static void main(String[] args) throws Exception {

        Path libDir = resolveLibDir();
        Files.createDirectories(libDir);

        System.out.println("Using lib dir: " + libDir);

        Configuration config = null;
        Path localConfig = libDir.resolve("config.xml");

        try {
            URL url = URI.create(CONFIG_URL).toURL();
            try (var reader = new InputStreamReader(url.openStream())) {
                config = Configuration.read(reader);
            }
            System.out.println("Remote config loaded.");
        } catch (Exception e) {
            System.out.println("Offline or unreachable: " + e.getMessage());
        }

        if (config == null && Files.exists(localConfig)) {
            try (var reader = Files.newBufferedReader(localConfig)) {
                config = Configuration.read(reader);
                System.out.println("Using cached config.");
            }
        }

        if (config == null) {
            System.err.println("No configuration found. Check your internet connection.");
            System.exit(1);
        }

        if (config.requiresUpdate()) {
            System.out.println("Downloading updates to: " + libDir);
            boolean success = config.update();
            if (!success) {
                System.err.println("Update failed. Launching with cached version.");
            } else {
                System.out.println("Update complete.");
                try (var writer = Files.newBufferedWriter(localConfig)) {
                    config.write(writer);
                }
            }
        } else {
            System.out.println("Already up to date.");
        }

        config.launch();
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

            if (isWritable(libDir)) {
                return libDir;
            }
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