package me.hysong.kynesystems.common.foundation.startup;

import me.hysong.atlas.async.SimplePromise;
import me.hysong.atlas.utils.MFS1;
import me.hysong.atlas.utils.vfs.v3.VFS3;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the initial setup of the application's storage directory.
 * This tool checks for a storage configuration file, initializes the directory
 * structure if needed, and ensures the storage is ready for use.
 * It uses a simple 'storage.conf' file instead of JSON.
 */
public class StorageSetupTool {

    // Use a constant for the configuration filename to avoid "magic strings".
    private static final String STORAGE_CONFIG_FILENAME = "storage.conf";

    /**
     * Initializes the storage. It checks the state of the storage via pathCheck,
     * and if the storage is uninitialized or a reset is forced, it copies the
     * base file structure to the disk.
     *
     * @param args Command-line arguments. Looks for "--reset-storage" to force re-initialization.
     * @return The verified path to the storage root.
     */
    public static String init(String[] args) {
        String pathCheckResult = pathCheck(args);

        // The first character from pathCheck indicates if storage is initialized ('t' or 'f').
        boolean isInitialized = pathCheckResult.toLowerCase().startsWith("t");
        String storagePath = pathCheckResult.substring(1);

        boolean forceReset = Arrays.stream(args)
                .parallel()
                .anyMatch(element -> element.equals("--reset-storage"));

        // If storage isn't initialized or a reset is forced, set it up.
        if (!isInitialized || forceReset) {
            System.out.println("Initializing or resetting storage at: " + storagePath);

            // Copy the default directory structure from the VFS image to the real disk.
            VFS3 vfs = new VFS3("struct.img.vfs3");
            vfs.imageToRealDisk(new File(storagePath));

            // Create the content for the config file, marking it as initialized.
            // Format: key=type:value
            // '1' is used for true, '0' for false.
            String initializedContent = "version=int32:1\n" +
                    "initialized=int1:1";
            MFS1.write(storagePath + File.separator + STORAGE_CONFIG_FILENAME, initializedContent);
        }

        return storagePath;
    }

    /**
     * Checks for and validates the storage directory and its configuration file.
     *
     * @param args Command-line arguments. Looks for "--root=" to specify a storage path.
     * @return A string where the first character is a flag ('t' for initialized, 'f' for not)
     * and the rest of the string is the path to the storage root.
     */
    private static String pathCheck(String[] args) {
        // Determine the root path for storage from arguments, or use "Storage" as a default.
        String rootPath = Arrays.stream(args)
                .filter(e -> e.startsWith("--root="))
                .map(s -> s.substring("--root=".length()))
                .findFirst()
                .orElse("Storage");

        // Create the storage directory if it doesn't already exist.
        if (!MFS1.isDirectory(rootPath) && !MFS1.mkdir(rootPath)) {
            // If creation fails, throw a descriptive error.
            throw new RuntimeException("Unable to create storage directory at: " + rootPath);
        }

        // Define the full path to the configuration file.
        String configFilePath = rootPath + File.separator + STORAGE_CONFIG_FILENAME;
        String storageConfigContent = MFS1.readString(configFilePath);

        // If the config file doesn't exist or is empty, create a default version.
        if (storageConfigContent == null || storageConfigContent.trim().isEmpty()) {
            System.out.println("Config file not found or empty. Creating default at: " + configFilePath);
            // Default to uninitialized state.
            String defaultConfig = "version=int32:1\n" +
                    "initialized=int1:0"; // 0 for false
            MFS1.write(configFilePath, defaultConfig);
            storageConfigContent = defaultConfig;
        }

        // Parse the key-value pairs from the config file content.
        Map<String, String> properties = new HashMap<>();
        // Split by any standard newline sequence for cross-platform compatibility.
        String[] lines = storageConfigContent.split("\\R");
        for (String line : lines) {
            // Skip empty or commented lines.
            if (line == null || line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String valuePart = parts[1].trim();

                // The value is expected to be after a ':', e.g., '1' from 'int32:1'
                String[] valueParts = valuePart.split(":", 2);
                if (valueParts.length == 2) {
                    properties.put(key, valueParts[1].trim());
                } else {
                    System.err.println("Warning: Malformed line in " + STORAGE_CONFIG_FILENAME + " (missing type specifier): " + line);
                }
            }
        }

        // Validate that the required properties exist and are valid.
        if (!"1".equals(properties.get("version")) || !properties.containsKey("initialized")) {
            SimplePromise.runAsync(() ->
                    JOptionPane.showMessageDialog(null, "Storage configuration is invalid or corrupt.\nFile: " + configFilePath)
            );
            throw new RuntimeException("Unable to load storage declaration. Missing or invalid 'version' or 'initialized' key in " + STORAGE_CONFIG_FILENAME);
        }

        // Determine the initialization flag.
        String initializedValue = properties.get("initialized");
        char initializedFlag = "1".equals(initializedValue) ? 't' : 'f';

        // Return the flag character prepended to the root path.
        return initializedFlag + rootPath;
    }
}
