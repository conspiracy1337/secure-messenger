package cns.client.data;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StoredFiles {
    public Path getConfigFolder() {
        String configPath;
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            configPath = System.getenv("APPDATA");
        } else if (os.contains("mac")) {
            String home = System.getProperty("user.home");
            configPath = home + "/Library/Application Support";
        } else {
            String home = System.getProperty("user.home");
            configPath = home + "/.config";
        }

        Path appDir = Paths.get(configPath, "cns", "secure-messenger");

        try {
            Files.createDirectories(appDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory: " + appDir, e);
        }

        return appDir;
    }

    public File getMasterKeyFile() {
        Path appDir = getConfigFolder();
        return appDir.resolve("master.key").toFile();
    }

    public File getDbFile() {
        Path appDir = getConfigFolder();
        return appDir.resolve("storage.db").toFile();
    }
}
