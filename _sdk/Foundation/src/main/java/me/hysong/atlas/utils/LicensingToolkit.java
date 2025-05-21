package me.hysong.atlas.utils;

import liblks.utils.StringDeriver;
import me.hysong.atlas.interfaces.KSDeepSystemCommunicator;
import me.hysong.atlas.sharedobj.ActivationData;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.UUID;

public class LicensingToolkit {

    static Scanner input = new Scanner(System.in);
    static VFS2 licenseFile = new VFS2();
    static String currentFile = "";

    private static void make() {
        licenseFile.format(32*1024);
        int licensesPerVolume = 20;
        System.out.print("How many ? (" + licensesPerVolume + ") >> ");
        try {
            licensesPerVolume = Integer.parseInt(input.nextLine());
        } catch (Exception e) {
            System.out.println("Error, please enter number.");
            return;
        }

        for (int i = 0; i < licensesPerVolume; i++) {
            String licenseGenerated = StringDeriver.deriveStringFrom(UUID.randomUUID().toString(), new int[]{5, 8, 13, 8, 5}, "-");
            licenseFile.write(licenseGenerated.toUpperCase(), ActivationData.NOT_ACTIVATED.getBytes());
        }
    }

    private static void view() {
        ArrayList<String> keys = licenseFile.list();
        ActivationData ad = new ActivationData(licenseFile);
        System.out.println("                     Key                   | Is Used |                 Linked To                 ");
        System.out.println("===========================================|=========|===========================================");
        for (String key : keys) {
            System.out.print(key + "|");
            System.out.print(ad.isActivated(key) ? (ad.isActivatedForThisMachine(key) ? "   YES*  |" : "   YES   |") : "   N O   |");
            System.out.println(licenseFile.read(key));
        }
    }

    public static void main(String[] args) {
        KSDeepSystemCommunicator com = KSHostTool.getSystemCommunicator();
        ArrayList<String> addrs = com.getMACAddressesAsHumanReadableString();
        System.out.println(addrs);
        System.out.println("COM: Found " + addrs.getFirst().length() + " bytes per address: " + addrs.getFirst());
        while (true) {
            System.out.print("(" + currentFile + ") >> ");
            String userIn = input.nextLine();
            switch (userIn) {
                case "exit": {
                    input.close();
                    System.exit(0);
                    break;
                }

                case "make": {
                    make();
                    break;
                }

                case "view": {
                    view();
                    break;
                }

                case "select": {
                    System.out.print("File Path >> ");
                    currentFile = input.nextLine();
                    break;
                }

                case "save": {
                    licenseFile.saveDisk(currentFile);
                    break;
                }

                case "load": {
                    licenseFile.loadDisk(currentFile);
                    break;
                }

                case "activate": {
                    ActivationData ad = new ActivationData(licenseFile);
                    System.out.print("Enter license key >> ");
                    boolean success = ad.activate(input.nextLine());
                    if (!success) {
                        System.out.println("ERROR: Failed to activate!");
                    } else {
                        System.out.println("Successfully activated.");
                    }
                }

                default: {
                    System.out.println("Unknown command. Use any of: exit make view select save load");
                }
            }
        }

    }
}
