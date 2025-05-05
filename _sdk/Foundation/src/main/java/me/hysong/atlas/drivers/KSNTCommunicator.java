package me.hysong.atlas.drivers;

import me.hysong.atlas.interfaces.KSDeepSystemCommunicator;

import java.io.File;

public class KSNTCommunicator implements KSDeepSystemCommunicator {
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
        return virtualPath.replace("/", File.separator);
    }

    @Override
    public String toVirtualPath(String realPath) {
        return realPath.replace(File.separator, "/");
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
        // AppData roaming path on Windows
        return System.getenv("APPDATA");
    }

    @Override
    public String getUserHomePath() {
        return "";
    }
}
