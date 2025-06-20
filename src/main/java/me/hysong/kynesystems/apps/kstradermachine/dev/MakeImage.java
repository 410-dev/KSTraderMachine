package me.hysong.kynesystems.apps.kstradermachine.dev;

import liblks.files.File2;
import me.hysong.atlas.utils.vfs.v3.VFS3;
import me.hysong.atlas.utils.vfs.v3.VFS3HeaderComposition;

import java.io.File;

public class MakeImage {
    public static void main(String[] args) {
        File2 f = new File2("Storage/defaults/");
        for (String s : f.childrenDirectories()) {
            VFS3 vfs = new VFS3();
            VFS3HeaderComposition formatter = new VFS3HeaderComposition()
                    .setDiskSize(16*1024*1024)
                    .setMaxFileNameLength(128)
                    .setMaxFilesCount(1000)
                    .setEnableFastRecovery(false);
            vfs.format(formatter);
            System.out.println(vfs.getReportString());
            File2 defaults = f.child(s);
            vfs.imageFromRealDisk(defaults, defaults.getAbsolutePath(), -1);
            for (String s2 : vfs.list()) {
                System.out.println("Imaged: " + s2);
            }
            vfs.saveDisk(s + ".img.vfs3");
        }
    }
}
