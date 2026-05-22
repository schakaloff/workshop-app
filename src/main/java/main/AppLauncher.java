package main;

import org.update4j.LaunchContext;
import org.update4j.service.Launcher;

public class AppLauncher implements Launcher {

    @Override
    public void run(LaunchContext context) {
        // Set the correct classloader so JavaFX and all deps resolve properly
        Thread.currentThread().setContextClassLoader(context.getClassLoader());

        // Hand off to your JavaFX app
        Main.main(new String[]{});
    }
}