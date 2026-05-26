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
import java.net.URLClassLoader;
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

        try {
            URL url = URI.create(CONFIG_URL).toURL();
            try (var reader = new InputStreamReader(url.openStream())) {
                config = Configuration.read(reader);
            }
        } catch (Exception e) {
            setStatus("Offline — launching cached version");
        }

        if (config == null && Files.exists(localConfig)) {
            try (var reader = Files.newBufferedReader(localConfig)) {
                config = Configuration.read(reader);
            } catch (Exception ignored) {}
        }

        if (config == null) {
            setVersion("No config found.");
            setStatus("Check your internet connection.");
            return;
        }

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
            // Close update screen
            stage.close();

            try {
                // Load the app JAR into a URLClassLoader
                Path appJar = AppLauncher.resolveAppJar();
                URLClassLoader loader = new URLClassLoader(
                        new URL[]{appJar.toUri().toURL()},
                        ClassLoader.getSystemClassLoader()
                );
                Thread.currentThread().setContextClassLoader(loader);

                // Load Main.start() from the downloaded JAR and run it on this stage
                Class<?> mainClass = loader.loadClass("main.Main");
                Object mainInstance = mainClass.getDeclaredConstructor().newInstance();

                // Create a new stage for the app
                Stage appStage = new Stage();

                // Call start(Stage) directly — reuse the existing JavaFX runtime
                mainClass.getMethod("start", Stage.class).invoke(mainInstance, appStage);

            } catch (Exception e) {
                e.printStackTrace();
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