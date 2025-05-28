package me.hysong.atlas.utils.vfs.v3;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

// VFS3HeaderComposition class (Marked as Serializable)
@Getter
@Setter
@Accessors(chain = true)
public class VFS3HeaderComposition implements Serializable {
    private static final long serialVersionUID = 1L;

    private String magicNumberContent = "ATLAS_VFS3";
    private byte[] versionNumber = new byte[]{3, 0, 0};
    private int maxFilesCount = 10;
    private int maxFileNameLength = 128;
    private long maxFileSize = 4L * 1024 * 1024 * 1024; // 4GB
    private long diskSize = 64L * 1024 * 1024; // 64MB

    private boolean enableFastRecovery = false;
    private String ioProcessorClassName = VFS3GenericIOController.class.getName();

    public VFS3HeaderComposition() {}

    public int getFileEntryByteSize() {
        int nameBytes = this.maxFileNameLength;
        int actualFileSizePersistenceBytes = Long.BYTES;
        int fileOffsetPersistenceBytes = Long.BYTES;
        int statusAndFlagsBytes = Byte.BYTES;
        int timestampsBytes = Long.BYTES * 2; // created, modified
        return nameBytes + actualFileSizePersistenceBytes + fileOffsetPersistenceBytes + statusAndFlagsBytes + timestampsBytes;
    }

    public long getBaseHeaderMetaSizeExcludingPointersAndDynamic() {
        long size = 0;
        size += magicNumberContent.getBytes(StandardCharsets.UTF_8).length;
        size += versionNumber.length;
        size += Integer.BYTES; // maxFilesCount
        size += Integer.BYTES; // maxFileNameLength
        size += Long.BYTES;    // maxFileSize
        size += Long.BYTES;    // diskSize
        size += Byte.BYTES;    // enableFastRecovery flag
        size += Byte.BYTES;    // IOProcessor presence flag
        return size;
    }

    public long calculateSerializedHeaderSizeOnDisk() {
        long size = getBaseHeaderMetaSizeExcludingPointersAndDynamic();
        if (ioProcessorClassName != null && !ioProcessorClassName.trim().isEmpty()) {
            size += Short.BYTES;
            size += ioProcessorClassName.getBytes(StandardCharsets.UTF_8).length;
        }
        size += Long.BYTES;    // Pointer to FAT Start
        size += Long.BYTES;    // Pointer to Data Area Start
        size += Long.BYTES;    // Pointer to IO Processor Dedicated Storage Start
        size += Integer.BYTES; // Size of IO Processor Dedicated Storage
        size += Long.BYTES;    // Last Known Data End Pointer
        return size;
    }

    public long getFileTableSizeBytes() {
        if (this.maxFilesCount <= 0) return 0;
        return (long) this.maxFilesCount * getFileEntryByteSize();
    }

    public long getMinimumTotalFootprintOnDisk(int ioProcessorStorageSize) {
        return calculateSerializedHeaderSizeOnDisk() + getFileTableSizeBytes() + ioProcessorStorageSize;
    }

    public VFS3HeaderComposition computeMaxFilesCountByHeaderRatio(long targetDiskSize, double headerRatio, int currentIoProcStorageSize) {
        this.diskSize = targetDiskSize;
        if (headerRatio <= 0.0 || headerRatio >= 1.0 || this.diskSize <= 0) {
            this.maxFilesCount = 0;
            return this;
        }
        long baseMetaSize = getBaseHeaderMetaSizeExcludingPointersAndDynamic();
        if (ioProcessorClassName != null && !ioProcessorClassName.trim().isEmpty()) {
            baseMetaSize += Short.BYTES;
            baseMetaSize += ioProcessorClassName.getBytes(StandardCharsets.UTF_8).length;
        }
        baseMetaSize += Long.BYTES * 3 + Integer.BYTES + Long.BYTES;
        long spaceForIoProc = currentIoProcStorageSize;
        long availableForHeaderAndFat = (long)(this.diskSize * headerRatio) - spaceForIoProc;
        if (availableForHeaderAndFat <= baseMetaSize) {
            this.maxFilesCount = 0;
            return this;
        }
        double availableSpaceForTableBytes = availableForHeaderAndFat - baseMetaSize;
        int singleFileEntrySizeBytes = getFileEntryByteSize();
        if (singleFileEntrySizeBytes <= 0) {
            this.maxFilesCount = 0;
            return this;
        }
        this.maxFilesCount = (int) Math.floor(availableSpaceForTableBytes / singleFileEntrySizeBytes);
        if (this.maxFilesCount < 0) this.maxFilesCount = 0;
        return this;
    }
}