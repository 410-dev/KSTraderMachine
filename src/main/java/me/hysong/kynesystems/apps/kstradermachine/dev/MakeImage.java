package me.hysong.kynesystems.apps.kstradermachine.dev;

import me.hysong.atlas.utils.vfs.v3.VFS3;
import me.hysong.atlas.utils.vfs.v3.VFS3HeaderComposition;

import java.io.File;

public class MakeImage {
    public static void main(String[] args) {
        VFS3 vfs = new VFS3();
        VFS3HeaderComposition formatter = new VFS3HeaderComposition()
                .setDiskSize(16*1024*1024)
                .setMaxFileNameLength(128)
                .setMaxFilesCount(1000)
                .setEnableFastRecovery(false);
        vfs.format(formatter);
        System.out.println(vfs.getReportString());
        File defaults = new File("Storage" + File.separator + "defaults");
        vfs.imageFromRealDisk(defaults, defaults.getAbsolutePath(), -1);
        for (String s : vfs.list()) {
            System.out.println("Imaged: " + s);
        }
        vfs.saveDisk("struct.img.vfs3");
    }
}
