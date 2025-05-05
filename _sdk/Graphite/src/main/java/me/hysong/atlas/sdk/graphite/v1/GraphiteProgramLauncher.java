package me.hysong.atlas.sdk.graphite.v1;

import me.hysong.atlas.async.SimplePromise;
import me.hysong.atlas.interfaces.KSApplication;
import me.hysong.atlas.sharedobj.KSEnvironment;

import javax.swing.*;
import java.awt.*;

public class GraphiteProgramLauncher {

    public static boolean sleekUIEnabled = false;

    private static int launchApp(KSApplication appInstance, KSEnvironment environment, String execLocation, String[] args) {

        if (KSGraphicalApplication.class.isAssignableFrom(appInstance.getClass())) {
            JFrame window = GraphiteWindowServer.makeWindow(appInstance.getAppDisplayName());
            KSGraphicalApplication app = (KSGraphicalApplication) appInstance;
            window.setContentPane((JPanel) appInstance);
            window.setSize(app.getWidth(), app.getHeight());
            window.setResizable(app.isResizable());
            window.setAlwaysOnTop(app.isAlwaysOnTop());
            window.setDefaultCloseOperation(app.getCloseBehavior());
            window.setUndecorated(app.isUndecorated());
            window.setLocationRelativeTo(app.getLocationRelativeTo());
            window.setOpacity(app.getOpacity());
            window.setTitle(appInstance.getAppDisplayName());
            window.setIconImage(app.getAppIcon());
            window.setVisible(true);

            Thread refreshThread = new Thread(() -> {
                System.out.println("Starting refresh thread for " + appInstance.getAppDisplayName());
                while (window.isVisible()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (app.refreshByFPS()) {
                        try {
                            Thread.sleep(app.getFPSTick());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    window.repaint();
                    window.revalidate();
                }
                System.out.println("Refresh thread for " + appInstance.getAppDisplayName() + " stopped");
            });
            refreshThread.start();
        }

        int result = appInstance.appMain(environment, execLocation, args);

        System.out.println("Application exited with code: " + result);
        return result;
    }

    public static void launch(Class<?> application, String[] args) {

        try {
            // Set the look and feel to the system default
            if (sleekUIEnabled) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } else {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        KSEnvironment environment = new KSEnvironment();

        // Get current pwd
        String execLocation = System.getProperty("user.dir");

        // Check if the application implements KSApplication
        if (KSApplication.class.isAssignableFrom(application)) {
            try {
                // Create an instance of the application
                KSApplication appInstance = (KSApplication) application.getDeclaredConstructor().newInstance();

                // Call the appMain method
                new Thread(() -> {
                    // Check if graphical application. If so, run splash screen
                    if (KSGraphicalApplication.class.isAssignableFrom(application)) {
                        KSGraphicalApplication graphicalApplication = (KSGraphicalApplication) appInstance;

                        GPSplashWindow splashWindowContent = graphicalApplication.getSplashWindow();
                        splashWindowContent.setSize(splashWindowContent.getWidth(), splashWindowContent.getHeight());
                        splashWindowContent.setVisible(true);
                        splashWindowContent.setLayout(null);
                        splashWindowContent.setBackground(splashWindowContent.getBackgroundColor());
                        JLabel statusText = new JLabel(splashWindowContent.getCurrentStatus());
                        statusText.setFont(new Font("Arial", Font.PLAIN, 25));
                        statusText.setForeground(splashWindowContent.getForegroundColor());
                        statusText.setHorizontalAlignment(splashWindowContent.getAlignment());
                        statusText.setSize(splashWindowContent.getWidth(), statusText.getFont().getSize() * 2);
                        statusText.setBorder(BorderFactory.createLineBorder(splashWindowContent.getForegroundColor()));
                        statusText.setLocation(0, splashWindowContent.getHeight() - statusText.getHeight());
                        statusText.setVisible(true);
                        splashWindowContent.add(statusText);
                        JFrame splashFrame = new JFrame();
                        splashFrame.setContentPane(splashWindowContent);
                        splashFrame.setSize(splashWindowContent.getWidth(), splashWindowContent.getHeight());
                        splashFrame.setLocationRelativeTo(null);
                        splashFrame.setResizable(false);
                        splashFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                        splashFrame.setUndecorated(true);
                        splashFrame.setAlwaysOnTop(true);
                        splashFrame.setLocation(
                                (Toolkit.getDefaultToolkit().getScreenSize().width - splashWindowContent.getWidth()) / 2,
                                (Toolkit.getDefaultToolkit().getScreenSize().height - splashWindowContent.getHeight()) / 2
                        );
                        splashFrame.setVisible(true);
                        Thread t = splashWindowContent.getSplashBackend();
                        SimplePromise.runAsync(() -> {
                            while (splashFrame.isVisible()) {
                                statusText.setText(splashWindowContent.getCurrentStatus());
                                splashWindowContent.repaint();
                                splashWindowContent.revalidate();
                                try {
                                    // 24 fps
                                    Thread.sleep(1000 / 24);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                        SimplePromise.runAsync(t)
                                .then(() -> {
                                    splashFrame.dispose();
                                    launchApp(appInstance, environment, execLocation, args);
                                })
                                .onError(() -> {
                                    System.out.println("Error in splash screen");
                                    splashFrame.dispose();
                                })
                                .start();
                    } else {
                        // Not sure if it has splash or not. Just run it.
                        launchApp(appInstance, environment, execLocation, args);
                    }
                }).start();
                // Handle the result if needed
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("The specified class does not implement KSApplication.");
        }
    }
}
