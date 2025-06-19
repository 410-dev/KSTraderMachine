package me.hysong.atlas.application;

import liblks.files.File2;
import me.hysong.atlas.interfaces.KSDeepSystemCommunicator;
import me.hysong.atlas.utils.KSHostTool;

public class ApplicationStorage {

    public static String appName = "ATLAS_DEFAULT_APP_NAME";
    public static KSDeepSystemCommunicator dsc = KSHostTool.getSystemCommunicator();

    public static boolean mkdirs(String virtualPath) {
        return new File2(dsc.getApplicationDataPath() + "/" + appName + "/" + virtualPath).mkdirs();
    }
}
