package main;

import org.update4j.LaunchContext;
import org.update4j.service.Launcher;

public class AppLauncher implements Launcher {

    @Override
    public void run(LaunchContext context) {
        Thread.currentThread().setContextClassLoader(context.getClassLoader());

        try {
            // Load Main from the DOWNLOADED JAR classloader — not bootstrap
            Class<?> mainClass = context.getClassLoader().loadClass("main.Main");
            mainClass.getMethod("main", String[].class)
                    .invoke(null, (Object) new String[]{});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}