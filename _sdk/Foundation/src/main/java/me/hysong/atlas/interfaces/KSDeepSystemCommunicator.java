package me.hysong.atlas.interfaces;

import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;

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

    default ArrayList<String> getMACAddressesAsString() {
        ArrayList<byte[]> macs = getMACAddressesInByteArray();
        ArrayList<String> result = new ArrayList<>();
        for (byte[] mac : macs) {
            result.add(new String(mac, StandardCharsets.UTF_8));
        }
        return result;
    }

    default ArrayList<String> getMACAddressesAsHumanReadableString() {
        ArrayList<byte[]> macs = getMACAddressesInByteArray();
        ArrayList<String> macAddresses = new ArrayList<>();
        for (byte[] mac : macs) {
            StringBuilder sb = new StringBuilder(18);
            for (byte b : mac) {
                if (!sb.isEmpty())
                    sb.append(':');
                sb.append(String.format("%02x", b));
            }
            macAddresses.add(sb.toString().toUpperCase());
        }
        return macAddresses;
    }

    default ArrayList<byte[]> getMACAddressesInByteArray() {
        try {
            Iterator<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces().asIterator();
            ArrayList<byte[]> macAddresses = new ArrayList<>();
            while (interfaces.hasNext()) {
                NetworkInterface netInterface = interfaces.next();
                byte[] addr = netInterface.getHardwareAddress();
                if (addr == null) continue;
                macAddresses.add(addr);
            }
            return macAddresses;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
}
