package me.hysong.atlas.utils.vfs.v3;

import java.io.Serializable;

public interface VFS3IOProcessor extends Serializable {
    static final long serialVersionUID = 1L;
    byte[] readBytes(byte[][] disk, byte[] ctlStorage, long startAddress, int length);
    long writeBytes(byte[][] disk, byte[] ctlStorage, long startAddress, byte[] content);
    int getRequiredDedicatedStorageSize(VFS3HeaderComposition headerComposition, long totalDiskSizeBytes);
    void onFormat(byte[] ctlStorage, VFS3HeaderComposition headerComposition, long totalDiskSizeBytes);
    void onLoad(byte[] ctlStorage, VFS3HeaderComposition headerComposition, long totalDiskSizeBytes);
}