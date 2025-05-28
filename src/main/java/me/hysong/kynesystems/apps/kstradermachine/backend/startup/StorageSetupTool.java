package me.hysong.kynesystems.apps.kstradermachine.backend.startup;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.hysong.atlas.async.SimplePromise;
import me.hysong.atlas.sdk.graphite.v1.GPSplashWindow;
import me.hysong.atlas.utils.MFS1;
import me.hysong.atlas.utils.VFS2;
import me.hysong.atlas.utils.vfs.v3.VFS3;
import me.hysong.kynesystems.apps.kstradermachine.Application;

import javax.swing.*;
import java.io.File;

public class StorageSetupTool {

    public static void copyDefault(GPSplashWindow splashWindow, String storagePath) {
        // Recursively list files in default location
        VFS3 vfs = new VFS3("struct.img.vfs3");
        vfs.imageToRealDisk(new File(storagePath));

        // Set storage initialized to true
        JsonObject storageDeclaration = new JsonObject();
        storageDeclaration.addProperty("version", 1);
        storageDeclaration.addProperty("initialized", true);
        MFS1.write(storagePath + "/storage.json", storageDeclaration.toString());
    }

    public static String init(GPSplashWindow splashWindow) {
        splashWindow.setCurrentStatus("Locating storage path...");

        String rootPath = "Storage";

        boolean hasDir = MFS1.isDirectory(rootPath);

        if (!hasDir) {
            if (!MFS1.mkdir(rootPath)) {
                throw new RuntimeException("Unable to create storage directory");
            }
        }

        // Locate storage declaration
        boolean hasStorageDeclaration = MFS1.isFile(rootPath + "/storage.json");
        if (!hasStorageDeclaration) {
            MFS1.write(rootPath + "/storage.json", "{}");
        }
        // Load storage declaration
        splashWindow.setCurrentStatus("Loading storage declaration...");
        String storageJson = MFS1.readString(rootPath + "/storage.json");
        if (storageJson == null || storageJson.isEmpty()) {
            JsonObject storageDeclaration = new JsonObject();
            storageDeclaration.addProperty("version", 1);
            storageDeclaration.addProperty("initialized", false);
            MFS1.write(rootPath + "/storage.json", storageDeclaration.toString());
            storageJson = storageDeclaration.toString();
        }
        JsonObject storageJo = JsonParser.parseString(storageJson).getAsJsonObject();
        if (storageJo == null) {
            SimplePromise.runAsync(() -> {
                JOptionPane.showMessageDialog(null, "Unable to load storage declaration");
            });
            throw new RuntimeException("Unable to parse storage json");
        } else if ((!storageJo.has("version") && storageJo.get("version").getAsInt() != 1) || !storageJo.has("initialized")) {
            throw new RuntimeException("Unable to load storage declaration. Missing version or initialized");
        }

        return storageJo.get("initialized").getAsString().charAt(0) + rootPath;
    }
}
