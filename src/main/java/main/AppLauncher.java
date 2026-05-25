package main;

import org.update4j.LaunchContext;
import org.update4j.service.Launcher;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class AppLauncher implements Launcher {

    @Override
    public void run(LaunchContext context) {
        try {
            Path appJar = resolveAppJar();

            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{appJar.toUri().toURL()},
                    Thread.currentThread().getContextClassLoader()
            );

            Thread.currentThread().setContextClassLoader(classLoader);

            Class<?> mainClass = classLoader.loadClass("main.Main");
            Method mainMethod = mainClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) new String[]{});

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Path resolveAppJar() {
        // Try next to the bootstrap JAR first (portable install)
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

        // Fallback to OS user data dir
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