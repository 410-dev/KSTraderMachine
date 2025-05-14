package me.hysong.kynesystems.apps.kstradermachine.backend.startup;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.hysong.atlas.async.SimplePromise;
import me.hysong.atlas.sdk.graphite.v1.GPSplashWindow;
import me.hysong.atlas.utils.MFS1;

import javax.swing.*;

public class StorageSetupTool {

    public static void copyDefault(GPSplashWindow splashWindow, String storagePath) {
        // TODO: Implement the copy default function.
    }

    public static String init(GPSplashWindow splashWindow) {
        splashWindow.setCurrentStatus("Locating storage path...");

        String rootPath = "Storage";

        boolean hasDir = MFS1.isDirectory(rootPath);

        if (!hasDir) {
            SimplePromise.runAsync(() -> {
                JOptionPane.showMessageDialog(null, "Storage directory not found. Please check again.", "Error", JOptionPane.ERROR_MESSAGE);
            });
            throw new RuntimeException("Storage directory not found.");
        }
        // Locate storage declaration
        boolean hasStorageDeclaration = MFS1.isFile(rootPath + "/storage.json");
        if (!hasStorageDeclaration) {
            SimplePromise.runAsync(() -> {
                JOptionPane.showMessageDialog(null, "Storage declaration not found. Please check again.", "Error", JOptionPane.ERROR_MESSAGE);
            });
            throw new RuntimeException("Storage declaration not found.");
        }
        // Load storage declaration
        splashWindow.setCurrentStatus("Loading storage declaration...");
        String storageJson = MFS1.readString(rootPath + "/storage.json");
        if (storageJson == null || storageJson.isEmpty()) {
            SimplePromise.runAsync(() -> {
                JOptionPane.showMessageDialog(null, "Storage declaration is empty. Please check again.", "Error", JOptionPane.ERROR_MESSAGE);
            });
            throw new RuntimeException("Storage declaration is empty.");
        }
        JsonObject storageJo = JsonParser.parseString(storageJson).getAsJsonObject();
        if (storageJo == null) {
            SimplePromise.runAsync(() -> {
                JOptionPane.showMessageDialog(null, "Storage declaration is invalid. Please check again.", "Error", JOptionPane.ERROR_MESSAGE);
            });
            throw new RuntimeException("Storage declaration is invalid.");
        } else if (!storageJo.has("version") && storageJo.get("version").getAsInt() != 1) {
            SimplePromise.runAsync(() -> {
                JOptionPane.showMessageDialog(null, "Storage declaration version is invalid. Please check again.", "Error", JOptionPane.ERROR_MESSAGE);
            });
            throw new RuntimeException("Storage declaration version is invalid.");
        }

        return rootPath;
    }
}
