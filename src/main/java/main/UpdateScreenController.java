package main;

import DB.ShopSettings;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import org.update4j.Configuration;
import org.update4j.FileMetadata;
import org.update4j.service.UpdateHandler;

import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class UpdateScreenController {

    @FXML private Label versionLabel;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;

    private static final String CONFIG_URL =
            "https://github.com/schakaloff/workshop-app/releases/latest/download/config.xml";

    public void startUpdate(Stage stage) {
        Thread thread = new Thread(() -> runUpdateCheck(stage));
        thread.setDaemon(true);
        thread.start();
    }

    private void runUpdateCheck(Stage stage) {
        Path libDir = Launcher.resolveLibDir();

        try {
            Files.createDirectories(libDir);
        } catch (Exception e) {
            launchApp(stage);
            return;
        }

        Configuration config = null;
        Path localConfig = libDir.resolve("config.xml");

        // ── 1. Try remote config ──────────────────────────────────────────
        try {
            URL url = URI.create(CONFIG_URL).toURL();
            try (var reader = new InputStreamReader(url.openStream())) {
                config = Configuration.read(reader);
            }
        } catch (Exception e) {
            setStatus("Offline — launching cached version");
        }

        // ── 2. Fallback to local config ───────────────────────────────────
        if (config == null && Files.exists(localConfig)) {
            try (var reader = Files.newBufferedReader(localConfig)) {
                config = Configuration.read(reader);
            } catch (Exception ignored) {}
        }

        // ── 3. No config ──────────────────────────────────────────────────
        if (config == null) {
            setVersion("No config found.");
            setStatus("Check your internet connection.");
            return;
        }

        final Configuration finalConfig = config;

        // ── 4. Already up to date ─────────────────────────────────────────
        try {
            if (!config.requiresUpdate()) {
                setVersion("v" + ShopSettings.VERSION + " — Up to date");
                setProgress(1.0);
                setStatus("Launching...");
                Thread.sleep(600);
                launchApp(stage);
                return;
            }
        } catch (Exception e) {
            launchApp(stage);
            return;
        }

        // ── 5. Update available ───────────────────────────────────────────
        setVersion("v" + ShopSettings.VERSION + " → New update available!");
        setStatus("Preparing...");
        setProgress(0);

        try {
            boolean success = config.update(new UpdateHandler() {

                @Override
                public void startDownloads() {
                    setStatus("Starting download...");
                }

                @Override
                public void startDownloadFile(FileMetadata file) {
                    setStatus("Downloading: " + file.getPath().getFileName());
                }

                @Override
                public void updateDownloadFileProgress(FileMetadata file, float progress) {
                    setProgress(progress);
                }

                @Override
                public void updateDownloadProgress(float progress) {
                    setProgress(progress);
                }

                @Override
                public void doneDownloadFile(FileMetadata file, Path path) {
                    setProgress(1.0);
                }

                @Override
                public void doneDownloads() {
                    setStatus("Update complete!");
                    setProgress(1.0);
                }

                @Override
                public void failed(Throwable t) {
                    setStatus("Update failed: " + t.getMessage());
                }
            });

            if (success) {
                try (var writer = Files.newBufferedWriter(localConfig)) {
                    config.write(writer);
                }
            }

            Thread.sleep(800);
            launchApp(stage);

        } catch (Exception e) {
            setStatus("Error: " + e.getMessage());
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
            launchApp(stage);
        }
    }

    private void launchApp(Stage stage) {
        Platform.runLater(() -> {
            stage.close();
            try {
                Path appJar = AppLauncher.resolveAppJar();

                String bundledJava = ProcessHandle.current().info().command()
                        .orElse(System.getProperty("java.home") + "/bin/java");

                // Use -cp instead of -jar
                ProcessBuilder pb = new ProcessBuilder(
                        bundledJava,
                        "-Xshare:off",
                        "-XX:+UseSerialGC",
                        "-cp", appJar.toString(),
                        "main.Main"
                );
                pb.inheritIO();
                pb.start();
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        });
    }

    private void setVersion(String text) {
        Platform.runLater(() -> versionLabel.setText(text));
    }

    private void setStatus(String text) {
        Platform.runLater(() -> statusLabel.setText(text));
    }

    private void setProgress(double value) {
        Platform.runLater(() -> progressBar.setProgress(value));
    }
}