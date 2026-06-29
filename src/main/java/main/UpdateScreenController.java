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
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

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

        // Override basePath so update4j downloads to the same dir AppLauncher looks in
        if (config != null) {
            config = config.toBuilder().basePath(libDir).build();
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

    private static Path resolveLogFile() {
        String os = System.getProperty("os.name").toLowerCase();
        Path base;
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            base = appData != null ? Path.of(appData, "workordermanager") : Path.of(System.getProperty("user.home"));
        } else {
            base = Path.of(System.getProperty("user.home"), ".workordermanager");
        }
        try { Files.createDirectories(base); } catch (Exception ignored) {}
        return base.resolve("launcher.log");
    }

    private static void writeLog(Path logFile, String message, Throwable t) {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(logFile,
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND))) {
            pw.println("[" + LocalDateTime.now() + "] " + message);
            if (t != null) t.printStackTrace(pw);
            pw.flush();
        } catch (Exception ignored) {}
    }

    private void launchApp(Stage stage) {
        Platform.runLater(() -> {
            // Hide the window but DON'T exit — keep the AppImage mount alive
            stage.hide();

            Path logFile = resolveLogFile();
            writeLog(logFile, "launchApp() called", null);

            try {
                Path appJar = AppLauncher.resolveAppJar();
                writeLog(logFile, "appJar resolved: " + appJar + " exists=" + appJar.toFile().exists(), null);

                String javaExe = System.getProperty("java.home") + "/bin/java";
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    javaExe += ".exe";
                }
                writeLog(logFile, "java exe: " + javaExe, null);

                ProcessBuilder pb = new ProcessBuilder(
                        javaExe,
                        "--add-modules", "javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.swing",
                        "-cp", appJar.toString(),
                        "main.Main"
                );
                pb.redirectErrorStream(true);
                pb.redirectOutput(logFile.toFile());
                writeLog(logFile, "Starting process: " + pb.command(), null);
                Process process = pb.start();
                writeLog(logFile, "Process started PID=" + process.pid(), null);

                // Wait for app to finish in background thread, then exit
                new Thread(() -> {
                    try {
                        int exit = process.waitFor();
                        writeLog(logFile, "Process exited with code " + exit, null);
                    } catch (InterruptedException ignored) {}
                    Platform.exit();
                }).start();

            } catch (Exception e) {
                writeLog(logFile, "FAILED to launch app", e);
                Platform.exit();
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