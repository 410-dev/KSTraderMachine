package me.hysong.atlas.utils;

import me.hysong.atlas.drivers.KSDarwinCommunicator;
import me.hysong.atlas.drivers.KSLinuxCommunicator;
import me.hysong.atlas.drivers.KSNTCommunicator;
import me.hysong.atlas.enums.OSKernelDistro;
import me.hysong.atlas.interfaces.KSDeepSystemCommunicator;

public class KSHostTool {

    public static OSKernelDistro getOSKernelDistro() {
        // This method should return the current OS kernel distribution.
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("linux")) {
            return OSKernelDistro.LINUX;
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            return OSKernelDistro.DARWIN;
        } else if (osName.contains("windows")) {
            return OSKernelDistro.WINDOWS;
        } else {
            return OSKernelDistro.UNKNOWN;
        }
    }

    public static KSDeepSystemCommunicator getSystemCommunicator() {
        OSKernelDistro osKernelDistro = getOSKernelDistro();
        return switch (osKernelDistro) {
            case LINUX -> new KSLinuxCommunicator();
            case DARWIN -> new KSDarwinCommunicator();
            case WINDOWS -> new KSNTCommunicator();
            default -> throw new UnsupportedOperationException("Unsupported OS kernel distribution: " + osKernelDistro);
        };
    }
}
