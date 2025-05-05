package me.hysong.atlas.drivers;

import me.hysong.atlas.interfaces.KSDeepSystemCommunicator;

public class KSDarwinCommunicator extends KSLinuxCommunicator implements KSDeepSystemCommunicator {

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
        return "";
    }

    @Override
    public String getUserHomePath() {
        return "";
    }
}
