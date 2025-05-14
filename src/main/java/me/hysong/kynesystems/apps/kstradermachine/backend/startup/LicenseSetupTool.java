package me.hysong.kynesystems.apps.kstradermachine.backend.startup;

import lombok.Getter;
import me.hysong.atlas.interfaces.KSApplication;
import me.hysong.atlas.sdk.graphite.v1.GPSplashWindow;
import me.hysong.atlas.sdk.graphite.v1.GraphiteProgramLauncher;
import me.hysong.atlas.sdk.graphite.v1.KSGraphicalApplication;
import me.hysong.atlas.utils.MFS1;
import me.hysong.atlas.utils.VFS1;
import me.hysong.kynesystems.apps.kstradermachine.objects.ActivationData;

import javax.swing.*;

@Getter
public class LicenseSetupTool extends JFrame {

    private static String licensePath;

    private static ActivationData activationData;


    public LicenseSetupTool() {
        // TODO: Implement the license setup tool UI and functionality.
        // TODO: Once activation is made, fill activationData with the license information.
    }


    public static void openLicensingTool() {
        LicenseSetupTool lst = new LicenseSetupTool();
        lst.setVisible(true);

        while (activationData == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        VFS1 vfsDisk = new VFS1();
        vfsDisk.format(64 * 1024);
        activationData.save(vfsDisk);
        vfsDisk.saveDisk(licensePath);
    }

    public static boolean isLicensed(GPSplashWindow splashWindow, String storagePath) {
        splashWindow.setCurrentStatus("Checking license...");
        licensePath = storagePath + "/license.vfs";
        if (!MFS1.isFile(licensePath)) {
            System.out.println("License file not found.");
            splashWindow.setCurrentStatus("Opening Licensing Tool...");
            openLicensingTool();
        }
        splashWindow.setCurrentStatus("Checking license...");
        VFS1 vfsDisk = new VFS1();
        vfsDisk.loadDisk(MFS1.realPath(licensePath));
        ActivationData activationData = new ActivationData();
        activationData.load(vfsDisk);
        if (!activationData.isActivated()) {
            splashWindow.setCurrentStatus("License not activated.");
            MFS1.delete(licensePath);
            return false;
        } else {
            splashWindow.setCurrentStatus("License activated.");
            return true;
        }
    }
}
