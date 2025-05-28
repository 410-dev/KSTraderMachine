package me.hysong.kynesystems.apps.kstradermachine.dev;

import me.hysong.atlas.utils.VFS2;

import java.io.File;

public class MakeImage {
    public static void main(String[] args) {
        VFS2 vfs = new VFS2();
        vfs.format(128*1024);
        File defaults = new File("Storage" + File.separator + "defaults");
        vfs.imageFromRealDisk(defaults, defaults.getAbsolutePath(), -1);
        for (String s : vfs.list()) {
            System.out.println("Imaged: " + s);
        }
    }
}
