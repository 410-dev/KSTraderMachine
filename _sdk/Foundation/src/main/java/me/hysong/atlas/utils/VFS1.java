package me.hysong.atlas.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class VFS1 {

    private byte[][] disk;

    // DOCUMENTATION
    /*

    Bytes
    0 ~ 9: Magic Number: "ATLAS_VFS1"
    10 ~ 13: Minor revision: "001"

     */

    public VFS1(String diskFile) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(diskFile));
            Object obj = ois.readObject();
            // Check if the object is of type byte[][]
            if (obj instanceof byte[][]) {
                disk = (byte[][]) obj;
            } else {
                System.err.println("Invalid object type in VFS1 file.");
                return;
            }
            ois.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
