package me.hysong.atlas.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;

@Getter
@Setter
@Accessors(chain = true)
public class VFS3HeaderComposition {

    private String magicNumberContent = "ATLAS_VFS3";
    private byte[] versionNumber = new byte[]{3, 0, 0}; // Always fixed to 3 digits
    private int maxFilesCount = 10; // By default
    private int maxFileNameLength = 128; // 128 characters in UTF-8 (interpreted as max bytes for name field)
    private long maxFileSize = 4L * 1024 * 1024 * 1024; // 4GB
    private long diskSize = 64L * 1024; // 64KB by default, explicitly long

    public VFS3HeaderComposition() {}

    /**
     * Calculates the size of a single file entry in the file allocation table.
     * Assumes maxFileNameLength is the number of bytes allocated for the name.
     *
     * @return The size of one file entry in bytes.
     */
    private int getFileEntryByteSizeInternal() {
        // Size of the filename field (assuming maxFileNameLength is in bytes for UTF-8 string storage)
        int nameBytes = this.maxFileNameLength;
        // Size of the field storing the actual size of the file
        int actualFileSizePersistenceBytes = Long.BYTES;
        // Size of the field storing the starting offset of the file data
        int fileOffsetPersistenceBytes = Long.BYTES;
        // Size of a field for status/flags (e.g., 0 for free, 1 for used, file type flags)
        int statusAndFlagsBytes = Byte.BYTES;

        return nameBytes + actualFileSizePersistenceBytes + fileOffsetPersistenceBytes + statusAndFlagsBytes;
    }

    /**
     * Calculates the size of the fixed metadata fields in the header
     * (excluding the file allocation table itself).
     *
     * @return The size of fixed header metadata in bytes.
     */
    private long getFixedHeaderMetaSize() {
        long size = 0;
        // Size of the magic number string when encoded (e.g., "ATLAS_VFS3" is 10 bytes in UTF-8)
        size += magicNumberContent.getBytes(StandardCharsets.UTF_8).length;
        // Size of the version number array (fixed at 3 bytes)
        size += versionNumber.length;
        // Size of the field that stores maxFilesCount (int)
        size += Integer.BYTES;
        // Size of the field that stores maxFileNameLength (int)
        size += Integer.BYTES;
        // Size of the field that stores maxFileSize (long)
        size += Long.BYTES;
        // Size of the field that stores diskSize (long)
        size += Long.BYTES;
        return size;
    }

    /**
     * Calculates the total size of the file allocation table in bytes.
     * <p>
     * <strong>Warning:</strong> This method returns an {@code int}. If {@code maxFilesCount} is
     * very large (e.g., tens of millions), the actual table length
     * ({@code maxFilesCount * entrySize}) might exceed {@code Integer.MAX_VALUE}.
     * In such cases, the returned {@code int} will be an incorrect, truncated value.
     * This implementation adheres to the provided method signature. Consider implications
     * if the table can indeed grow beyond 2GB.
     * </p>
     *
     * @return The calculated total size of the file table in bytes, potentially truncated if it exceeds Integer.MAX_VALUE.
     */
    public int getTableLength() {
        if (this.maxFilesCount <= 0) {
            return 0;
        }
        // Calculate the full table size as a long first
        long tableLengthAsLong = (long) this.maxFilesCount * getFileEntryByteSizeInternal();

        // Check for potential overflow before casting to int
        if (tableLengthAsLong > Integer.MAX_VALUE) {
            System.err.println("Warning: VFS3HeaderComposition.getTableLength() - Potential overflow. " +
                               "Actual table size (" + tableLengthAsLong +
                               " bytes) exceeds Integer.MAX_VALUE. Returning truncated value.");
            // Depending on desired behavior, could throw an exception or return Integer.MAX_VALUE.
            // Current behavior is to truncate as per cast.
        }
        return (int) tableLengthAsLong;
    }

    /**
     * Calculates the minimum total disk size required to store all header information,
     * including the file allocation table. This represents the overhead of the VFS
     * before any actual file data is stored.
     * <p>
     * This method uses {@code long} for calculations to ensure accuracy even if the
     * file table is very large.
     * </p>
     *
     * @return The minimum required disk size for the header and file table in bytes.
     */
    public long getMinimumRequiredDiskSize() {
        long fixedMetaSize = getFixedHeaderMetaSize();

        long actualTableSizeBytes = 0;
        if (this.maxFilesCount > 0) {
            // Calculate the true table size as a long to ensure accuracy for this method's long return type.
            actualTableSizeBytes = (long) this.maxFilesCount * getFileEntryByteSizeInternal();
        }

        // Note: The public getTableLength() method returns an int and might truncate for very large tables.
        // This method calculates the minimum required size using the full long value for table size for accuracy.
        return fixedMetaSize + actualTableSizeBytes;
    }

    /**
     * Computes and updates {@code maxFilesCount} based on a desired ratio of the total
     * {@code diskSize} that the header (fixed metadata + file table) should occupy.
     * <p>
     * For example, if {@code headerRatio} is 0.1, this method will attempt to set
     * {@code maxFilesCount} such that the entire header uses approximately 10% of {@code diskSize}.
     * </p>
     *
     * @param headerRatio The desired ratio (0.0 to 1.0 exclusive) of disk space to be used by the header.
     * If the ratio is outside the sensible range (e.g., <= 0 or >= 1),
     * or if diskSize is too small, maxFilesCount might be set to 0.
     * @return This {@code VFS3HeaderComposition} instance for chaining.
     */
    public VFS3HeaderComposition computeMaxFilesCountByHeaderRatio(double headerRatio) {
        // Validate headerRatio and diskSize early
        if (headerRatio <= 0.0 || headerRatio >= 1.0 || this.diskSize <= 0) {
            // Invalid ratio or non-positive disk size, cannot meaningfully compute.
            // Set to 0 files, or could throw IllegalArgumentException.
            this.maxFilesCount = 0;
            return this;
        }

        long fixedMetaSizeBytes = getFixedHeaderMetaSize();

        // If diskSize is not even enough for fixed metadata, no files can be indexed.
        if (this.diskSize <= fixedMetaSizeBytes) {
            this.maxFilesCount = 0;
            return this;
        }

        // Calculate the target total size for the header (fixed metadata + file table)
        double targetTotalHeaderSizeBytes = this.diskSize * headerRatio;

        // If the target header size is less than what fixed metadata alone requires,
        // then no space is left for the file table.
        if (targetTotalHeaderSizeBytes < fixedMetaSizeBytes) {
            this.maxFilesCount = 0;
            return this;
        }

        double availableSpaceForTableBytes = targetTotalHeaderSizeBytes - fixedMetaSizeBytes;
        int singleFileEntrySizeBytes = getFileEntryByteSizeInternal();

        // If for some reason entry size is not positive, cannot divide.
        if (singleFileEntrySizeBytes <= 0) {
            this.maxFilesCount = 0; // Should not happen with current getFileEntryByteSizeInternal logic
            return this;
        }

        // Calculate the number of file entries that can fit in the available space.
        // Math.floor is used because we can't have a fraction of a file entry.
        int newMaxFilesCount = (int) Math.floor(availableSpaceForTableBytes / singleFileEntrySizeBytes);

        // Ensure newMaxFilesCount is not negative (it shouldn't be if availableSpaceForTableBytes is non-negative).
        if (newMaxFilesCount < 0) {
            newMaxFilesCount = 0;
        }

        this.maxFilesCount = newMaxFilesCount;
        return this;
    }
}
