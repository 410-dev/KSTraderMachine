package me.hysong.kynesystems.apps.kstradermachine;

import lombok.Getter;
import me.hysong.atlas.interfaces.KSApplication;
import me.hysong.atlas.sdk.graphite.v1.GPSplashWindow;
import me.hysong.atlas.sdk.graphite.v1.GraphiteProgramLauncher;
import me.hysong.atlas.sdk.graphite.v1.KSGraphicalApplication;
import me.hysong.atlas.sharedobj.KSEnvironment;
import me.hysong.kynesystems.apps.kstradermachine.backend.Config;

import javax.swing.*;
import java.awt.*;

@Getter
public class Application extends KSGraphicalApplication implements KSApplication {

    private final String appDisplayName = "Kyne Systems Trader Machine";
    private final int width = 800;
    private final int height = 600;

    @Override
    public int appMain(KSEnvironment environment, String execLocation, String[] args) {
        return 0;
    }

    @Override
    public GPSplashWindow getSplashWindow() {
        GPSplashWindow splashWindow = new GPSplashWindow(3840, 200, JLabel.RIGHT);
        splashWindow.setSplashBackend(new Thread(() -> {
            // Simulate loading process
            try {
                splashWindow.setCurrentStatus("Loading configurations...");
                Config.load("path/to/config.json"); // Set the path to your config file // TODO: Implement loading path

                Thread.sleep(100); // Simulate loading time
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));
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
