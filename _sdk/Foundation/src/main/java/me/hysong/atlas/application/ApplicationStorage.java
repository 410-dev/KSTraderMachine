package me.hysong.atlas.application;

import me.hysong.atlas.interfaces.KSDeepSystemCommunicator;
import me.hysong.atlas.utils.KSHostTool;
import me.hysong.atlas.utils.MFS1;

public class ApplicationStorage {

    public static String appName = "ATLAS_DEFAULT_APP_NAME";
    public static KSDeepSystemCommunicator dsc = KSHostTool.getSystemCommunicator();

    public static boolean mkdirs(String virtualPath) {
        return MFS1.mkdirs(dsc.getApplicationDataPath() + "/" + appName + "/" + virtualPath);
    }
}
