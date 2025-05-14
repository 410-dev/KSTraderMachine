package me.hysong.kynesystems.apps.ksmanualtrader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import me.hysong.atlas.async.SimplePromise;
import me.hysong.atlas.interfaces.KSApplication;
import me.hysong.atlas.sdk.graphite.v1.GPSplashWindow;
import me.hysong.atlas.sdk.graphite.v1.GraphiteProgramLauncher;
import me.hysong.atlas.sdk.graphite.v1.KSGraphicalApplication;
import me.hysong.atlas.sharedobj.KSEnvironment;
import me.hysong.atlas.utils.MFS1;

import javax.swing.*;
import java.awt.*;

@Getter
public class Application extends KSGraphicalApplication implements KSApplication {

    private final String appDisplayName = "Kyne Systems Manual Trader";
    private final int width = 800;
    private final int height = 600;

    @Override
    public int appMain(KSEnvironment environment, String execLocation, String[] args) {
        return 0;
    }

    @Override
    public GPSplashWindow getSplashWindow() {
        GPSplashWindow splashWindow = new GPSplashWindow(400, 300, JLabel.RIGHT);
        splashWindow.setSplashBackend(new Thread(() -> {
            // Locate storage path
            splashWindow.setCurrentStatus("Locating storage path...");
            boolean hasDir = MFS1.isDirectory("Storage");
            if (!hasDir) {
                SimplePromise.runAsync(() -> {
                    JOptionPane.showMessageDialog(null, "Storage directory not found. Please check again.", "Error", JOptionPane.ERROR_MESSAGE);
                });
                throw new RuntimeException("Storage directory not found.");
            }
            // Locate storage declaration
            boolean hasStorageDeclaration = MFS1.isFile("Storage/storage.json");
            if (!hasStorageDeclaration) {
                SimplePromise.runAsync(() -> {
                    JOptionPane.showMessageDialog(null, "Storage declaration not found. Please check again.", "Error", JOptionPane.ERROR_MESSAGE);
                });
                throw new RuntimeException("Storage declaration not found.");
            }
            // Load storage declaration
            splashWindow.setCurrentStatus("Loading storage declaration...");
            String storageJson = MFS1.readString("Storage/storage.json");
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

//            try {
//                splashWindow.setCurrentStatus("Loading configurations...");
//                Config.load("path/to/config.json"); // Set the path to your config file // TODO: Implement loading path
//                Thread.sleep(3000); // Simulate loading time
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }));
        JLabel titleLabel = new JLabel("Kyne Systems Trader Machine");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setVerticalAlignment(SwingConstants.CENTER);
        titleLabel.setSize(splashWindow.getWidth(), splashWindow.getHeight());
        titleLabel.setLocation(0,0);
        splashWindow.add(titleLabel);
        splashWindow.setStatusSuffixSpacing("    ");
        splashWindow.setStatusSuffix("KSTraderMachine 1.0");
        splashWindow.setForegroundColor(Color.WHITE);
        splashWindow.setBackgroundColor(Color.BLACK);
        splashWindow.setImageLocation("path/to/splash/image.png"); // Set the path to your splash image
        return splashWindow;
    }

    public static void main(String[] args) {
        GraphiteProgramLauncher.sleekUIEnabled = true;
        GraphiteProgramLauncher.launch(Application.class, args);
    }
}