package me.hysong.atlas.interfaces;

public interface KSDeepSystemCommunicator {
    boolean canUseEncryptedUserHomeDirectoryIsolation();
    boolean canUseHomeDirectoryIsolation();
    String toRealPath(String virtualPath);
    String toVirtualPath(String realPath);
    String getCurrentDistribution();
    String getCurrentPlatform();
    String getCurrentArchitecture();
    String getCurrentDistributionVersion();
    String getCurrentDistributionName();
    String getCurrentDistributionCodename();
    String getCurrentDistributionRelease();
    String getCurrentDistributionDescription();

    String getApplicationDataPath();
    String getUserHomePath();
}
