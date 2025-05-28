package me.hysong.atlas.utils.vfs.v3;

import java.io.Serializable; // Required as VFS3IOProcessor extends Serializable

// Assuming VFS3HeaderComposition and VFS3IOProcessor are in the same package or imported.

public class VFS3GenericIOController implements VFS3IOProcessor {

    private static final long serialVersionUID = 1L; // For Serializable interface compliance
    private transient int diskSideLength; // Cached disk dimension, re-calculated on load/format

    /**
     * Initializes or updates the diskSideLength.
     * This method is crucial for mapping linear addresses to 2D array coordinates.
     * It prioritizes the actual disk dimensions if available, then falls back to
     * information from the header composition or total disk size.
     *
     * @param disk The VFS disk (byte[][]), can be null if called during early init.
     * @param headerComposition The VFS header configuration, can be null.
     * @param totalDiskSizeBytes The total size of the disk in bytes, used as a fallback.
     */
    private void ensureDiskSideLength(byte[][] disk, VFS3HeaderComposition headerComposition, long totalDiskSizeBytes) {
        // If diskSideLength is already positive, assume it's correctly set,
        // unless a disk is provided and its dimensions differ.
        if (this.diskSideLength > 0) {
            if (disk != null && disk.length > 0 && disk.length != this.diskSideLength) {
                // Disk dimensions might have changed unexpectedly if the byte[][] array was swapped.
                // For this controller, we'll trust the current disk's dimensions.
                this.diskSideLength = disk.length;
            }
            return;
        }

        // Prioritize actual disk dimensions if available
        if (disk != null && disk.length > 0) {
            this.diskSideLength = disk.length;
        } else if (headerComposition != null && headerComposition.getDiskSize() > 0) {
            // Fallback to header composition
            this.diskSideLength = (int) Math.sqrt(headerComposition.getDiskSize());
            // Basic sanity check for perfect square, though VFS format should ensure this
            // if it relies on a square byte[][].
            if ((long)this.diskSideLength * this.diskSideLength != headerComposition.getDiskSize()) {
                // This could happen if diskSize is not a perfect square.
                // Math.sqrt() will floor, so this is the effective side length.
                // System.err.println("VFS3GenericIOController: Warning - diskSize from composition is not a perfect square.");
            }
        } else if (totalDiskSizeBytes > 0) {
            // Fallback to totalDiskSizeBytes
            this.diskSideLength = (int) Math.sqrt(totalDiskSizeBytes);
            if ((long)this.diskSideLength * this.diskSideLength != totalDiskSizeBytes) {
                // System.err.println("VFS3GenericIOController: Warning - totalDiskSizeBytes is not a perfect square.");
            }
        }
        // If diskSideLength is still 0, readBytes/writeBytes will likely fail or return empty/0.
    }

    /**
     * Converts a linear disk address to a 2D [row, column] pair.
     * @param linearAddress The absolute byte offset from the beginning of the disk.
     * @return An int array [row, column], or [-1, -1] if diskSideLength is not initialized or invalid.
     */
    private int[] getRowCol(long linearAddress) {
        if (diskSideLength <= 0) { // Check for positive diskSideLength
            // This indicates an uninitialized state or an error in setup.
            // System.err.println("VFS3GenericIOController: diskSideLength is not positive in getRowCol. Call onFormat/onLoad first.");
            return new int[]{-1, -1};
        }
        return new int[]{(int) (linearAddress / diskSideLength), (int) (linearAddress % diskSideLength)};
    }

    @Override
    public byte[] readBytes(byte[][] disk, byte[] ctlStorage, long startAddress, int length) {
        // Ensure diskSideLength is set, using the provided disk as the primary source for dimensions
        ensureDiskSideLength(disk, null, 0);

        if (disk == null || diskSideLength == 0 || length <= 0) {
            return new byte[0]; // Nothing to read or invalid parameters
        }
        if (startAddress < 0) {
            System.err.println("GenericIOController: Read attempt with negative startAddress: " + startAddress);
            return new byte[0];
        }

        long maxDiskCapacity = (long)diskSideLength * diskSideLength;
        if (startAddress >= maxDiskCapacity) {
            return new byte[0]; // Start address is completely out of bounds
        }

        // Adjust length if the read operation would go past the end of the disk
        int effectiveLength = length;
        if (startAddress + length > maxDiskCapacity) {
            effectiveLength = (int) (maxDiskCapacity - startAddress);
            if (effectiveLength <= 0) { // No readable bytes in the adjusted range
                return new byte[0];
            }
        }

        byte[] data = new byte[effectiveLength];
        for (int i = 0; i < effectiveLength; i++) {
            long currentAddress = startAddress + i;
            int[] pos = getRowCol(currentAddress);
            int r = pos[0];
            int c = pos[1];

            // This check should be redundant if effectiveLength and getRowCol are correct
            // and diskSideLength is positive.
            if (r < 0 || r >= diskSideLength || c < 0 || c >= diskSideLength) {
                System.err.println("GenericIOController: Read out of bounds at calculated address " + currentAddress +
                        " (r=" + r + ", c=" + c + ", side=" + diskSideLength + "). This indicates an internal logic error. Returning partial data if any.");
                if (i == 0) return new byte[0]; // Error on the very first byte
                byte[] partialData = new byte[i];
                System.arraycopy(data, 0, partialData, 0, i);
                return partialData;
            }
            data[i] = disk[r][c];
        }
        return data;
    }

    @Override
    public long writeBytes(byte[][] disk, byte[] ctlStorage, long startAddress, byte[] content) {
        // Ensure diskSideLength is set, using the provided disk for its dimensions
        ensureDiskSideLength(disk, null, 0);

        if (disk == null || diskSideLength == 0 || content == null || content.length == 0) {
            return 0; // Nothing to write or invalid parameters
        }
        if (startAddress < 0) {
            System.err.println("GenericIOController: Write attempt with negative startAddress: " + startAddress);
            return 0;
        }

        long maxDiskCapacity = (long)diskSideLength * diskSideLength;
        int bytesWritten = 0;

        for (int i = 0; i < content.length; i++) {
            long currentAddress = startAddress + i;
            if (currentAddress >= maxDiskCapacity) { // Check if current write position is out of bounds
                System.err.println("GenericIOController: Write attempt beyond disk capacity at address " + currentAddress +
                        ". Disk capacity: " + maxDiskCapacity + ". Bytes written so far: " + bytesWritten);
                break; // Stop writing
            }

            int[] pos = getRowCol(currentAddress);
            int r = pos[0];
            int c = pos[1];

            // Similar to read, this check should be redundant if the capacity check works.
            if (r < 0 || r >= diskSideLength || c < 0 || c >= diskSideLength) {
                System.err.println("GenericIOController: Write out of bounds at calculated address " + currentAddress +
                        " (r=" + r + ", c=" + c + ", side=" + diskSideLength + "). This indicates an internal logic error. Bytes written so far: " + bytesWritten);
                break; // Stop writing
            }
            disk[r][c] = content[i];
            bytesWritten++;
        }
        return bytesWritten;
    }

    @Override
    public int getRequiredDedicatedStorageSize(VFS3HeaderComposition headerComposition, long totalDiskSizeBytes) {
        // This basic controller does not require any dedicated storage.
        return 0;
    }

    @Override
    public void onFormat(byte[] ctlStorage, VFS3HeaderComposition headerComposition, long totalDiskSizeBytes) {
        // Initialize diskSideLength based on the VFS being formatted.
        this.diskSideLength = 0; // Reset first to ensure re-calculation
        ensureDiskSideLength(null, headerComposition, totalDiskSizeBytes);
        // System.out.println("VFS3GenericIOController: onFormat called. Initialized diskSideLength: " + this.diskSideLength);
    }

    @Override
    public void onLoad(byte[] ctlStorage, VFS3HeaderComposition headerComposition, long totalDiskSizeBytes) {
        // Initialize diskSideLength based on the VFS being loaded.
        // The actual disk byte[][] isn't passed directly here, so rely on composition/total size.
        this.diskSideLength = 0; // Reset first
        ensureDiskSideLength(null, headerComposition, totalDiskSizeBytes);
        // System.out.println("VFS3GenericIOController: onLoad called. Initialized diskSideLength: " + this.diskSideLength);
    }
}