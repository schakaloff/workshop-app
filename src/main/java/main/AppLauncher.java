package main;

import org.update4j.LaunchContext;
import org.update4j.service.Launcher;

import java.nio.file.Path;

public class AppLauncher implements Launcher {

    @Override
    public void run(LaunchContext context) {
        try {
            Path appJar = resolveAppJar();
            System.out.println("Launching app JAR: " + appJar);

            if (!appJar.toFile().exists()) {
                System.err.println("App JAR not found: " + appJar);
                return;
            }

            ProcessBuilder pb = new ProcessBuilder(
                    ProcessHandle.current().info().command().orElse("java"),
                    "-jar", appJar.toString()
            );
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    // public so UpdateScreenController can use it
    public static Path resolveAppJar() {
        try {
            Path jarLocation = Path.of(
                    AppLauncher.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).getParent();

            Path libDir = jarLocation.resolve("lib");
            Path appJar = libDir.resolve("workordermanager-app.jar");
            if (appJar.toFile().exists()) return appJar;
        } catch (Exception ignored) {}

        return getFallbackDir().resolve("workordermanager-app.jar");
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