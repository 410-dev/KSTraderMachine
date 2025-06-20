package me.hysong.atlas.sdk.graphite.v1;

import me.hysong.atlas.async.SimplePromise;
import me.hysong.atlas.enums.OSKernelDistro;
import me.hysong.atlas.interfaces.KSApplication;
import me.hysong.atlas.sharedobj.KSEnvironment;
import me.hysong.atlas.utils.KSHostTool;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Enumeration;

public class GraphiteProgramLauncher {

    public static boolean sleekUIEnabled = false;

    public static void setUIFont(FontUIResource f){
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get (key);
            if (value instanceof FontUIResource)
                UIManager.put (key, f);
        }
    }

    private static int launchApp(KSApplication appInstance, KSEnvironment environment, String execLocation, String[] args) {

        if (KSGraphicalApplication.class.isAssignableFrom(appInstance.getClass())) {
            JFrame window = GraphiteWindowServer.makeWindow(appInstance.getAppDisplayName());
            KSGraphicalApplication app = (KSGraphicalApplication) appInstance;
            window.setSize(app.getWindowWidth(), app.getWindowHeight());
            window.setResizable(app.isResizable());
            window.setAlwaysOnTop(app.isAlwaysOnTop());
            window.setDefaultCloseOperation(app.getCloseBehavior());
            window.setUndecorated(app.isUndecorated());
            window.setLocationRelativeTo(app.getLocationRelativeTo());
            window.setOpacity(app.getOpacity());
            window.setTitle(appInstance.getAppDisplayName());
            window.setIconImage(app.getAppIcon());
            window.setVisible(true);
//            app.setBorder(BorderFactory.createLineBorder(Color.GREEN));
//            window.setContentPane((JPanel) appInstance);
            window.add((JPanel)appInstance);

            Thread refreshThread = new Thread(() -> {
                System.out.println("Starting refresh thread for " + appInstance.getAppDisplayName());
                while (window.isVisible()) {
                    if (app.isDisposeQueue()) {
                        window.dispose();
                    }
                    if (app.refreshByFPS()) {
                        try {
                            Thread.sleep(app.getFPSTick());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            Thread.sleep(100);
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
                if (KSHostTool.getOSKernelDistro().equals(OSKernelDistro.WINDOWS)) {
                    setUIFont(new FontUIResource("Malgun Gothic", Font.PLAIN, 13));
                } else {
                    setUIFont(new FontUIResource("Arial", Font.PLAIN, 13));
                }
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

                        // Get current resolution
                        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                        int screenWidth = (int) screenSize.getWidth();
                        int screenHeight = (int) screenSize.getHeight();

                        // Get current PPI
                        double ppi = Toolkit.getDefaultToolkit().getScreenResolution();

                        // PPI explained:
                        //  Possible range of PPI values:
                        //  - 50: Low resolution (e.g., older monitors)
                        //  - 100: Standard resolution (e.g., 1080p monitors)
                        //  - 200: High resolution (e.g., 4K monitors)
                        //  - 300: Very high resolution (e.g., 8K monitors)

                        System.out.println("Current screen resolution: " + screenWidth + "x" + screenHeight);
                        System.out.println("Current screen PPI: " + ppi);

                        KSGraphicalApplication graphicalApplication = (KSGraphicalApplication) appInstance;

                        GPSplashWindow splashWindowContent = graphicalApplication.getSplashWindow(args);
                        Thread splashBackend = new Thread(() -> System.out.println("There's no splash backend to run."));
                        JFrame splashFrame;
                        if (splashWindowContent != null) {
                            splashWindowContent.setSize(splashWindowContent.getWidth(), splashWindowContent.getHeight());
                            splashWindowContent.setVisible(true);
                            splashWindowContent.setLayout(null);
                            splashWindowContent.setBackground(splashWindowContent.getBackgroundColor());
                            splashWindowContent.setOpaque(true);
                            JLabel statusText = new JLabel(splashWindowContent.getCurrentStatus());

                            // Appropriate font size is based on screen resolution, and window size
                            // Size: 10% of splash window height if resolution is greater than 1080p
                            // Otherwise, 20% of splash window height
                            // PPI: 1.5x the font size if PPI is greater than 100
                            int fontSize = (int) (splashWindowContent.getHeight() * 0.05);
                            if (screenHeight < 1080) {
                                fontSize = (int) (splashWindowContent.getHeight() * 0.1);
                            }
                            if (ppi > 300) {
                                fontSize = (int) (fontSize * 1.5);
                            } else if (ppi > 200) {
                                fontSize = (int) (fontSize * 1.25);
                            } else if (ppi > 100) {
                                fontSize = (int) (fontSize * 1.0);
                            }

                            statusText.setFont(new Font("Arial", Font.PLAIN, fontSize));
                            statusText.setForeground(splashWindowContent.getForegroundColor());
                            statusText.setHorizontalAlignment(splashWindowContent.getAlignment());
                            statusText.setSize(splashWindowContent.getWidth(), statusText.getFont().getSize() * 2);
//                            statusText.setBorder(BorderFactory.createLineBorder(splashWindowContent.getForegroundColor()));
                            statusText.setLocation(0, splashWindowContent.getHeight() - statusText.getHeight());
                            statusText.setVisible(true);
                            splashWindowContent.add(statusText);
                            splashFrame = new JFrame();
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
                            splashBackend = splashWindowContent.getSplashBackend();
                            SimplePromise.runAsync(() -> {
                                while (splashFrame.isVisible()) {
//                                new Thread(() -> {
                                    statusText.setText(splashWindowContent.getCurrentStatus());
                                    splashWindowContent.repaint();
                                    splashWindowContent.revalidate();
//                                }).start();
                                    try {
                                        // 24 fps
                                        Thread.sleep(1000 / 24);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                        } else {
                            splashFrame = null;
                        }

                        SimplePromise.runAsync(splashBackend)
                                .then(() -> {
                                    if (splashFrame != null) {
                                        splashFrame.dispose();
                                    }
                                    launchApp(appInstance, environment, execLocation, args);
                                })
                                .onError(() -> {
                                    System.out.println("Error in splash screen");
                                    if (splashFrame != null) {
                                        splashFrame.dispose();
                                    }
                                    System.exit(0);
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
