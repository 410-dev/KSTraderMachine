package me.hysong.kynesystems.apps.ksmanualtrader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import me.hysong.apis.kstrader.v1.driver.TraderDriverManifestV1;
import me.hysong.atlas.application.ApplicationStorage;
import me.hysong.atlas.async.SimplePromise;
import me.hysong.atlas.interfaces.KSApplication;
import me.hysong.atlas.sdk.graphite.v1.GPSplashWindow;
import me.hysong.atlas.sdk.graphite.v1.GraphiteProgramLauncher;
import me.hysong.atlas.sdk.graphite.v1.KSGraphicalApplication;
import me.hysong.atlas.sharedobj.KSEnvironment;
import me.hysong.atlas.utils.MFS1;
import me.hysong.kynesystems.common.foundation.SystemLogs;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

@Getter
public class KSManualTrader extends KSGraphicalApplication implements KSApplication {

    private final String appDisplayName = "Kyne Systems Manual Trader";
    private final int windowWidth = 800;
    private final int windowHeight = 600;

    private String storagePath = "Storage";

    @Override
    public int appMain(KSEnvironment environment, String execLocation, String[] args) {
        return 0;
    }

    @Override
    public GPSplashWindow getSplashWindow() {
        GPSplashWindow splashWindow = new GPSplashWindow(400, 300, JLabel.RIGHT);
        splashWindow.setSplashBackend(new Thread(() -> {
            StorageTool
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

    public void loadDrivers() {

        // Load drivers
        try {
            Drivers.loadJarsIn(new File(MFS1.realPath(storagePath + "/drivers")));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Failed to load drivers", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        // Index drivers
        try {
            HashMap<String, Class<?>> drivers = (HashMap<String, Class<?>>) Drivers.DriverIntrospection.findImplementations(TraderDriverManifestV1.class);
            for (String key : drivers.keySet()) {
                Class<?> driverClass = drivers.get(key);
                SystemLogs.log("INFO", "Driver: " + key + " -> " + driverClass.getName());
                try {
                    TraderDriverManifestV1 manifest = (TraderDriverManifestV1) driverClass.getDeclaredConstructor().newInstance();
                    Drivers.driversInstantiated.put(key, manifest);
                    Drivers.drivers.put(key, driverClass);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Drivers not instantiated!", "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                    throw new RuntimeException("Driver instantiation failed", ex);
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Drivers are not loaded!", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            throw new RuntimeException(e);
        }


    }

    public static void main(String[] args) {
        GraphiteProgramLauncher.sleekUIEnabled = true;
        GraphiteProgramLauncher.launch(KSManualTrader.class, args);
    }
}