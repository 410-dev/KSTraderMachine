package me.hysong.atlas.sharedobj;

import liblks.security.CoreSHA;
import liblks.utils.StringDeriver;
import me.hysong.atlas.interfaces.KSDeepSystemCommunicator;
import me.hysong.atlas.utils.KSHostTool;
import me.hysong.atlas.utils.VFS2;

import java.util.ArrayList;

public class ActivationData {

    private final VFS2 storage;
    public static final String NOT_ACTIVATED = StringDeriver.deriveStringFrom("BELLO", new int[]{5, 8, 13, 8, 5}, "-");

    public ActivationData(VFS2 storage) {
        this.storage = storage;
    }

    public boolean isValidLicenseKey(String licenseKey) {
        return storage.exists(licenseKey);
    }

    public boolean isActivatedForThisMachine(String licenseKey) {
        KSDeepSystemCommunicator com = KSHostTool.getSystemCommunicator();
        ArrayList<String> macs = com.getMACAddressesAsHumanReadableString();
        macs.replaceAll(toDigest -> CoreSHA.hash512(toDigest, toDigest));
        licenseKey = licenseKey.trim();
        if (!isValidLicenseKey(licenseKey)) {
            return false;
        }
        String activatedString = storage.read(licenseKey);
        System.out.println("License: " + licenseKey);
        for (String mac : macs) {
            String derived = StringDeriver.deriveStringFrom(mac, new int[]{5, 8, 13, 8, 5}, "-");
            System.out.println("Derived: " + derived + " to " + activatedString);
            if (activatedString != null && activatedString.equals(derived)) {
                return true;
            }
        }
        return false;
    }

    public boolean isActivated(String licenseKey) {
        return isValidLicenseKey(licenseKey) && !storage.read(licenseKey.trim()).equals(NOT_ACTIVATED);
    }

    public boolean canActivate(String licenseKey) {
        return isValidLicenseKey(licenseKey) && !isActivatedForThisMachine(licenseKey.trim()) && storage.read(licenseKey).equals(NOT_ACTIVATED);
    }

    public boolean activate(String licenseKey) {
        if (!canActivate(licenseKey)) return false;
        KSDeepSystemCommunicator com = KSHostTool.getSystemCommunicator();
        ArrayList<String> macs = com.getMACAddressesAsHumanReadableString();
        if (macs.isEmpty()) {
            throw new RuntimeException("No hardware key. Add network card with MAC address.");
        }
        String target = CoreSHA.hash512(macs.getFirst(), macs.getFirst());
        String derived = StringDeriver.deriveStringFrom(target, new int[]{5, 8, 13, 8, 5}, "-");
        storage.write(licenseKey, derived.getBytes());
        String view = storage.read(licenseKey);
        return view.equals(derived);
    }

    public void save(String path) {
        storage.saveDisk(path);
    }

    public void load(String path) {
        storage.loadDisk(path);
    }

}

