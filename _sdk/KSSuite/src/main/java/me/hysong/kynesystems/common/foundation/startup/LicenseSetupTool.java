package me.hysong.kynesystems.common.foundation.startup;

import lombok.Getter;
import me.hysong.atlas.sharedobj.ActivationData;
import me.hysong.atlas.utils.MFS1;
import me.hysong.atlas.utils.VFS2;
import me.hysong.kynesystems.common.foundation.SystemLogs;

import javax.swing.*;

@Getter
public class LicenseSetupTool extends JFrame {

    private static String licensePath;
    private static String licensePlainPath;

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

        activationData.save(MFS1.realPath(licensePath));
    }

    public static boolean isLicensed(String storagePath) {
        licensePath = storagePath + "/license.vfs";
        licensePlainPath = storagePath + "/license.txt";
        if (!MFS1.isFile(licensePlainPath)) {
            SystemLogs.log("INFO", "License file not found.");
            openLicensingTool();
        }
        if (!MFS1.isFile(licensePath)) {
            SystemLogs.log("INFO", "License container not found. Unable to continue.");
            JOptionPane.showMessageDialog(null, "License container not found. Unable to continue.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        VFS2 vfs2 = new VFS2();
        vfs2.loadDisk(licensePath);
        ActivationData activationData = new ActivationData(vfs2);
        try {
            return activationData.isActivatedForThisMachine(MFS1.readString(licensePlainPath));
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
