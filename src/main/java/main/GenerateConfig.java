package main;

import org.update4j.Configuration;
import org.update4j.FileMetadata;

import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

/**
 * Run this in CI after mvn package to generate config.xml.
 *
 * Args:
 *   [0] baseUrl        — remote URL where JARs are uploaded
 *   [1] jarDir         — local directory containing the app JAR
 *   [2] outputPath     — where to write config.xml
 *
 * Example (CI):
 *   java -cp target/workordermanager-app.jar main.GenerateConfig \
 *     "https://github.com/USER/REPO/releases/download/v1.0.0/" \
 *     "target/" \
 *     "target/config.xml"
 */
public class GenerateConfig {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: GenerateConfig <baseUrl> <jarDir> <outputPath>");
            System.exit(1);
        }

        String baseUrl   = args[0];
        Path   jarDir    = Paths.get(args[1]);
        Path   outputPath = Paths.get(args[2]);

        System.out.println("Base URL  : " + baseUrl);
        System.out.println("JAR dir   : " + jarDir.toAbsolutePath());
        System.out.println("Output    : " + outputPath.toAbsolutePath());

        Configuration config = Configuration.builder()
                .baseUri(baseUrl)
                // basePath is resolved dynamically at runtime by Launcher.resolveLibDir()
                // This is just a safe neutral default
                .basePath("${user.home}/.workordermanager/lib")
                .launcher(AppLauncher.class)   // tells update4j which Launcher to use
                .files(
                        FileMetadata.streamDirectory(jarDir)
                                .filter(f -> f.getSource()
                                        .getFileName()
                                        .toString()
                                        .equals("workordermanager-app.jar")) // only the app JAR
                                .map(r -> r.classpath(true))
                                .collect(Collectors.toList())
                )
                .build();

        try (var writer = new FileWriter(outputPath.toFile())) {
            config.write(writer);
        }

        System.out.println("config.xml written successfully.");
    }
}