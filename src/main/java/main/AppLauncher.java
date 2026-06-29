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

            // Use the same JRE that's currently running
            String bundledJava = System.getProperty("java.home") + "/bin/java";
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                bundledJava += ".exe";
            }
            System.out.println("Using java: " + bundledJava);

            Path logFile = appJar.getParent().resolve("app.log");
            ProcessBuilder pb = new ProcessBuilder(
                    bundledJava,
                    "-cp", appJar.toString(),
                    "main.Main"
            );
            pb.redirectErrorStream(true);
            pb.redirectOutput(logFile.toFile());
            Process process = pb.start();
            process.waitFor();
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

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
        return Path.of(System.getProperty("user.home"), "workordermanager", "lib");
    }
}