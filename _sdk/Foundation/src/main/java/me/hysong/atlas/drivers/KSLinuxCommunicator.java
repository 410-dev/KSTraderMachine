package me.hysong.atlas.drivers;

import me.hysong.atlas.interfaces.KSDeepSystemCommunicator;

public class KSLinuxCommunicator implements KSDeepSystemCommunicator {
    @Override
    public boolean canUseEncryptedUserHomeDirectoryIsolation() {
        return false;
    }

    @Override
    public boolean canUseHomeDirectoryIsolation() {
        return false;
    }

    @Override
    public String toRealPath(String virtualPath) {
        return virtualPath.replace("\\", "/");
    }

    @Override
    public String toVirtualPath(String realPath) {
        return realPath.replace("\\", "/");
    }

    @Override
    public String getCurrentDistribution() {
        return "";
    }

    @Override
    public String getCurrentPlatform() {
        return "";
    }

    @Override
    public String getCurrentArchitecture() {
        return "";
    }

    @Override
    public String getCurrentDistributionVersion() {
        return "";
    }

    @Override
    public String getCurrentDistributionName() {
        return "";
    }

    @Override
    public String getCurrentDistributionCodename() {
        return "";
    }

    @Override
    public String getCurrentDistributionRelease() {
        return "";
    }

    @Override
    public String getCurrentDistributionDescription() {
        return "";
    }

    @Override
    public String getApplicationDataPath() {
        return System.getProperty("user.home") + "/.local/share";
    }

    @Override
    public String getUserHomePath() {
        return System.getProperty("user.home");
    }
}
