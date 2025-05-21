package me.hysong.atlas.utils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

// Add these or ensure they are present
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects; // For Objects.requireNonNull
import java.io.File;
import java.io.FileInputStream; // Though Files.readAllBytes is often preferred
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path; // Used by Files.readAllBytes(realFile.toPath())
// No explicit import for java.math.BigInteger is needed with the chosen hex conversion

public class VFS2 {

    // Disk state
    private byte[][] disk;
    private int[][] readCounter; // For potential wear-leveling simulation or stats
    private int[][] writeCounter; // For potential wear-leveling simulation or stats
    private int diskSize; // Use a clear name instead of just 'size'

    // --- Header Structure Constants ---
    // All offsets assume the start of the disk (address 0)

    // Magic Number ("ATLAS_VFS2")
    private static final int OFF_MAGIC = 0;
    private static final int LEN_MAGIC = 10;

    // Version ("001") - Store as int for easier comparison if needed later
    private static final int OFF_VERSION_MAJOR = 10; // Not used in original, adding for structure
    private static final int LEN_VERSION_MAJOR = 1; // Example
    private static final int OFF_VERSION_MINOR = 11; // Adjusted offset
    private static final int LEN_VERSION_MINOR = 1; // Example: store 1 byte = 1
    private static final int OFF_VERSION_PATCH = 12; // Adjusted offset
    private static final int LEN_VERSION_PATCH = 1; // Example: store 1 byte = 0

    // Pointer Length (Bytes needed for address pointers, e.g., 4 for 32-bit, 8 for 64-bit)
    // We use 4 bytes (int) for pointers in this version, matching original logic.
    private static final int OFF_PTR_LEN = 13; // Adjusted offset
    private static final int LEN_PTR_LEN = 1; // Value = 4 or 8

    // Header End Pointer (Address of the first byte AFTER the header)
    private static final int OFF_HEADER_END_PTR = 14; // Adjusted offset
    private static final int LEN_HEADER_END_PTR = 4; // Stored as int

    // File Table End Pointer (Address of the first byte AFTER the file table)
    private static final int OFF_TABLE_END_PTR = 18; // Adjusted offset
    private static final int LEN_TABLE_END_PTR = 4; // Stored as int

    // Max File Size (Bytes) - Limit per file
    private static final int OFF_MAX_FILE_BYTES = 22; // Adjusted offset
    private static final int LEN_MAX_FILE_BYTES = 4; // Stored as int

    // Max Number of Files (Capacity of the file table)
    private static final int OFF_MAX_FILES = 26; // Adjusted offset
    private static final int LEN_MAX_FILES = 4; // Stored as int

    // Last Data Block Pointer (End of contiguous data - for simple append allocation)
    // Storing the single 'long' address is simpler than row/col for logic.
    // Let's switch to a single 'long' offset pointer.
    private static final int OFF_LAST_DATA_PTR = 30; // Adjusted offset
    private static final int LEN_LAST_DATA_PTR = 8; // Stored as long

    // Reserved space / End of defined header fields
    private static final int HEADER_SIZE = 38; // Byte offset AFTER the last defined header field (30 + 8)

    // --- File Table Entry Structure Constants ---
    private static final int FILE_ENTRY_SIZE = 109; // Original fixed size

    // Field offsets relative to the start of an entry
    private static final int FENTRY_OFF_ID = 0;         // 8 bytes (long) - Unique ID (can be slot index)
    private static final int FENTRY_OFF_EXISTS = 8;       // 1 byte (0 or 1)
    private static final int FENTRY_OFF_SIZE_BYTES = 9;   // 4 bytes (int) - Actual file size
    private static final int FENTRY_OFF_SIZE_BLOCKS = 13; // 4 bytes (int) - DEPRECATED (use SIZE_BYTES)? Orig had this. Let's keep but maybe ignore.
    private static final int FENTRY_OFF_CREATED_MS = 17;  // 8 bytes (long)
    private static final int FENTRY_OFF_ACCESSED_MS = 25; // 8 bytes (long)
    private static final int FENTRY_OFF_MODIFIED_MS = 33; // 8 bytes (long)
    private static final int FENTRY_OFF_START_ADDR = 41;  // 4 bytes (int) - Starting address of file data
    private static final int FENTRY_OFF_FILENAME = 45;    // 64 bytes (UTF-8, null-padded)

    // --- Constructors ---

    /**
     * Creates an empty VFS instance (needs formatting or loading).
     * Used by the console application.
     */
    public VFS2() {
        // empty constructor for console usage
    }

    /**
     * Loads a VFS from a serialized file.
     * @param diskFile Path to the file containing the serialized byte[][] disk image.
     */
    public VFS2(String diskFile) {
        loadDisk(diskFile);
    }

    /**
     * Loads the VFS disk image from a file.
     * @param diskFile Path to the file.
     * @throws RuntimeException if loading fails or the file format is invalid.
     */
    public void loadDisk(String diskFile) {
        System.out.println("Loading VFS from: " + diskFile);
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(diskFile))) {
            Object obj = ois.readObject();
            if (!(obj instanceof byte[][])) {
                throw new IllegalArgumentException("Invalid VFS image: Object is not byte[][]");
            }
            this.disk = (byte[][]) obj;
            if (this.disk.length == 0 || this.disk.length != this.disk[0].length) {
                 throw new IllegalArgumentException("Invalid VFS image: Disk must be a non-empty square array.");
            }
            this.diskSize = this.disk.length;
            initializeCounters();
            // Basic validation: Check magic number
            if (!validateHeaderBasics()) {
                 System.err.println("Warning: VFS header magic number mismatch or looks uninitialized.");
                 // Allow loading anyway, but warn the user. Formatting might be needed.
            }
            System.out.println("VFS loaded successfully (" + this.diskSize + "x" + this.diskSize + ").");
        } catch (IOException | ClassNotFoundException | IllegalArgumentException e) {
            // Make sure disk is null if loading fails
            this.disk = null;
            this.diskSize = 0;
            this.readCounter = null;
            this.writeCounter = null;
            throw new RuntimeException("Failed to load VFS disk from " + diskFile + ": " + e.getMessage(), e);
        }
    }

     /**
     * Saves the current VFS disk image to a file.
     * @param diskFile Path to the file.
     * @throws RuntimeException if saving fails.
     */
    public void saveDisk(String diskFile) {
        ensureFormatted();
        System.out.println("Saving VFS to: " + diskFile);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(diskFile))) {
            oos.writeObject(this.disk);
            System.out.println("VFS saved successfully.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to save VFS disk to " + diskFile + ": " + e.getMessage(), e);
        }
    }

    private void initializeCounters() {
        if (this.diskSize > 0) {
            this.readCounter = new int[this.diskSize][this.diskSize];
            this.writeCounter = new int[this.diskSize][this.diskSize];
            // No need to initialize to 0, default for new int[][] is 0.
        } else {
             this.readCounter = null;
             this.writeCounter = null;
        }
    }

    // Place this private helper method within your VFS1 class,
// for example, near other private helper methods.
    /**
     * Calculates the hash of a byte array using the specified algorithm.
     *
     * @param data The byte array to hash.
     * @param algorithm The hashing algorithm (e.g., "MD5", "SHA-1", "SHA-256", "SHA-512").
     * @return The hexadecimal string representation of the hash.
     * @throws NoSuchAlgorithmException if the specified algorithm is not available.
     */
    private String calculateHash(byte[] data, String algorithm) throws NoSuchAlgorithmException {
        if (data == null) {
            // Or handle as an error, but for hashing, null data could mean an empty hash or error
            // Depending on strictness, an empty byte array might be more appropriate for "empty" content
            return null; // Or specific handling for null if needed
        }
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] hashBytes = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

// Add the following public methods to your VFS1 class:

    /**
     * Calculates the hash of a file stored within the VFS.
     *
     * @param vfsFileName The name of the file in the VFS.
     * @param algorithm The hashing algorithm to use (e.g., "MD5", "SHA-1", "SHA-256", "SHA-512").
     * @return The hexadecimal string representation of the hash, or null if the file is not found,
     * the algorithm is not supported, or an error occurs.
     */
    public String hashFile(String vfsFileName, String algorithm) {
        ensureFormatted(); // Ensures VFS is loaded/formatted
        Objects.requireNonNull(vfsFileName, "VFS file name cannot be null.");
        Objects.requireNonNull(algorithm, "Algorithm cannot be null.");

        // Validate algorithm early
        try {
            MessageDigest.getInstance(algorithm); // Check if algorithm is supported by attempting to get an instance
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Hash Error: Algorithm '" + algorithm + "' is not supported. " + e.getMessage());
            return null;
        }

        byte[] fileData = readFileBytes(vfsFileName);
        if (fileData == null) {
            // readFileBytes should ideally print its own "not found" or error messages.
            // Adding a specific message for hash context can be useful.
            System.err.println("Hash Error: File '" + vfsFileName + "' not found in VFS or could not be read for hashing.");
            return null;
        }

        try {
            return calculateHash(fileData, algorithm);
        } catch (NoSuchAlgorithmException e) {
            // This should theoretically be caught by the initial check, but as a safeguard:
            System.err.println("Hash Error: Unexpected issue with algorithm '" + algorithm + "' during hash calculation. " + e.getMessage());
            return null;
        }
    }

    /**
     * Imports a file from the real disk into the VFS.
     *
     * @param vfsFilename The desired name for the file within the VFS.
     * @param realFile    The {@link File} object representing the source file on the real disk.
     * @param checkByHash If true, verifies the integrity of the copy by comparing hashes
     * (using SHA-256) of the source and the VFS copy.
     * @return True if the file was successfully imported and (if checked) hashes match, false otherwise.
     */
    public boolean fromDisk(String vfsFilename, File realFile, boolean checkByHash) {
        ensureFormatted();
        Objects.requireNonNull(vfsFilename, "VFS file name cannot be null.");
        Objects.requireNonNull(realFile, "Real file object cannot be null.");

        if (!realFile.exists() || !realFile.isFile() || !realFile.canRead()) {
            System.err.println("FromDisk Error: Real file '" + realFile.getAbsolutePath() + "' does not exist, is not a regular file, or cannot be read.");
            return false;
        }

        byte[] fileData;
        try {
            long fileSize = realFile.length();
            if (fileSize > Integer.MAX_VALUE) {
                System.err.println("FromDisk Error: Real file '" + realFile.getAbsolutePath() + "' is too large (" + fileSize + " bytes) to be stored in this VFS version (max " + Integer.MAX_VALUE + " bytes).");
                return false;
            }
            fileData = Files.readAllBytes(realFile.toPath());
        } catch (IOException e) {
            System.err.println("FromDisk Error: Failed to read real file '" + realFile.getAbsolutePath() + "'. " + e.getMessage());
            return false;
        } catch (OutOfMemoryError oom) {
            System.err.println("FromDisk Error: Not enough memory to read real file '" + realFile.getAbsolutePath() + "' (size: " + realFile.length() + " bytes). " + oom.getMessage());
            return false;
        }

        // Write to VFS
        boolean writeSuccess = write(vfsFilename, fileData);
        if (!writeSuccess) {
            // The VFS 'write' method should print its own detailed errors (e.g., out of space, name too long)
            System.err.println("FromDisk Error: Failed to write file '" + vfsFilename + "' to VFS.");
            return false;
        }

        if (checkByHash) {
            System.out.println("FromDisk: Performing hash check for '" + vfsFilename + "' (Source: '" + realFile.getName() + "')...");
            String sourceHash;
            String vfsHash;
            // Using a fixed, strong algorithm for this internal integrity check.
            // This could be a class constant or configurable if needed.
            String hashAlgorithmForCheck = "SHA-256";

            try {
                sourceHash = calculateHash(fileData, hashAlgorithmForCheck);
            } catch (NoSuchAlgorithmException e) {
                System.err.println("FromDisk Hash Check Error: Algorithm '" + hashAlgorithmForCheck + "' not supported for source file. Cannot verify. " + e.getMessage());
                // If hash check is critical, fail the operation.
                return false;
            }

            // Read back from VFS for verification
            byte[] vfsFileData = readFileBytes(vfsFilename);
            if (vfsFileData == null) {
                System.err.println("FromDisk Hash Check Error: Failed to read back file '" + vfsFilename + "' from VFS for verification. This indicates a serious issue.");
                return false; // File should exist as we just wrote it and write reported success.
            }

            try {
                vfsHash = calculateHash(vfsFileData, hashAlgorithmForCheck);
            } catch (NoSuchAlgorithmException e) {
                System.err.println("FromDisk Hash Check Error: Algorithm '" + hashAlgorithmForCheck + "' not supported for VFS file. Cannot verify. " + e.getMessage());
                return false;
            }

            if (sourceHash != null && vfsHash != null && sourceHash.equals(vfsHash)) {
                System.out.println("FromDisk Hash Check: OK. Hashes match for '" + vfsFilename + "'.");
            } else {
                System.err.println("FromDisk Hash Check Error: HASH MISMATCH for '" + vfsFilename + "'. Data integrity compromised.");
                System.err.println("  Source Hash (" + hashAlgorithmForCheck + "): " + sourceHash);
                System.err.println("  VFS Hash    (" + hashAlgorithmForCheck + "): " + vfsHash);
                // Consider if the file in VFS should be deleted if hashes don't match.
                // For now, we just report the failure of the 'fromDisk' operation.
                // delete(vfsFilename); // Example: if you want to auto-cleanup on mismatch
                return false;
            }
        }

        System.out.println("Successfully imported '" + realFile.getAbsolutePath() + "' to VFS as '" + vfsFilename + "'.");
        return true;
    }

    /**
     * Exports a file from the VFS to the real disk.
     *
     * @param vfsFileName   The name of the file within the VFS.
     * @param realFile      The {@link File} object representing the destination file on the real disk.
     * If the file exists, it will be overwritten.
     * @param checkByHash   If true, verifies the integrity of the copy by comparing hashes
     * (using SHA-256) of the VFS source and the real disk copy.
     * Note: The parameter name `booleanCheckByHash` in the prompt was assumed to be a typo for `checkByHash`.
     * @return True if the file was successfully exported and (if checked) hashes match, false otherwise.
     */
    public boolean toDisk(String vfsFileName, File realFile, boolean checkByHash) {
        ensureFormatted();
        Objects.requireNonNull(vfsFileName, "VFS file name cannot be null.");
        Objects.requireNonNull(realFile, "Real file object cannot be null.");

        byte[] vfsFileData = readFileBytes(vfsFileName);
        if (vfsFileData == null) {
            System.err.println("ToDisk Error: File '" + vfsFileName + "' not found in VFS.");
            return false;
        }

        // Attempt to write to the real file system
        try (FileOutputStream fos = new FileOutputStream(realFile)) { // This will overwrite if the file exists
            fos.write(vfsFileData);
        } catch (IOException e) {
            System.err.println("ToDisk Error: Failed to write VFS file '" + vfsFileName + "' to real disk at '" + realFile.getAbsolutePath() + "'. " + e.getMessage());
            return false;
        } catch (SecurityException se) {
            System.err.println("ToDisk Error: Security manager denied write access to '" + realFile.getAbsolutePath() + "'. " + se.getMessage());
            return false;
        }


        if (checkByHash) {
            System.out.println("ToDisk: Performing hash check for '" + vfsFileName + "' (Destination: '" + realFile.getName() + "')...");
            String vfsHash;
            String realFileHash;
            String hashAlgorithmForCheck = "SHA-256"; // Consistent algorithm for integrity checks

            try {
                vfsHash = calculateHash(vfsFileData, hashAlgorithmForCheck);
            } catch (NoSuchAlgorithmException e) {
                System.err.println("ToDisk Hash Check Error: Algorithm '" + hashAlgorithmForCheck + "' not supported for VFS file. Cannot verify. " + e.getMessage());
                return false;
            }

            byte[] realFileBytesReadBack;
            try {
                // Ensure the file was actually created and is readable before attempting to read for hash
                if (!realFile.exists() || !realFile.isFile() || !realFile.canRead()) {
                    System.err.println("ToDisk Hash Check Error: Real file '" + realFile.getAbsolutePath() + "' not found or not readable after write for verification.");
                    return false; // Cannot verify if we can't read it back
                }
                realFileBytesReadBack = Files.readAllBytes(realFile.toPath());
            } catch (IOException e) {
                System.err.println("ToDisk Hash Check Error: Failed to read back real file '" + realFile.getAbsolutePath() + "' for verification. " + e.getMessage());
                return false;
            } catch (OutOfMemoryError oom) {
                System.err.println("ToDisk Hash Check Error: Not enough memory to read back real file '" + realFile.getAbsolutePath() + "' for verification. " + oom.getMessage());
                return false;
            }

            try {
                realFileHash = calculateHash(realFileBytesReadBack, hashAlgorithmForCheck);
            } catch (NoSuchAlgorithmException e) {
                System.err.println("ToDisk Hash Check Error: Algorithm '" + hashAlgorithmForCheck + "' not supported for real disk file. Cannot verify. " + e.getMessage());
                return false;
            }

            if (vfsHash != null && vfsHash.equals(realFileHash)) {
                System.out.println("ToDisk Hash Check: OK. Hashes match for '" + realFile.getAbsolutePath() + "'.");
            } else {
                System.err.println("ToDisk Hash Check Error: HASH MISMATCH for '" + realFile.getAbsolutePath() + "'. Data integrity compromised.");
                System.err.println("  VFS Hash      (" + hashAlgorithmForCheck + "): " + vfsHash);
                System.err.println("  RealFile Hash (" + hashAlgorithmForCheck + "): " + realFileHash);
                // Consider if the written realFile should be deleted if hashes don't match.
                // realFile.delete(); // Example: Cautious cleanup
                return false;
            }
        }

        System.out.println("Successfully exported VFS file '" + vfsFileName + "' to '" + realFile.getAbsolutePath() + "'.");
        return true;
    }
//
//    public boolean imageFromDisk(File source, String head, int maxDepth) {
//        // TODO
//    }
//
//    public boolean imageToDisk(File target) {
//        // TODO
//    }

    // --- Low-Level Disk Access ---

    /**
     * Writes a single byte at the specified absolute address.
     * Handles mapping the linear address to the 2D array.
     * @param address The absolute byte offset from the beginning of the disk.
     * @param data The byte to write.
     * @return true if successful, false if the address is out of bounds.
     */
    public boolean writeAt(long address, byte data) {
        if (this.disk == null || this.diskSize == 0) return false; // Not initialized

        long rowLong = address / this.diskSize;
        long colLong = address % this.diskSize;

        // Check bounds using long comparison first
        if (rowLong < 0 || rowLong >= this.diskSize || colLong < 0 || colLong >= this.diskSize) {
            // System.err.println("Write out of bounds: address=" + address + ", size=" + diskSize);
            return false;
        }

        // Cast to int ONLY for array indexing, after bounds check
        int r = (int) rowLong;
        int c = (int) colLong;

        this.disk[r][c] = data;
        if (this.writeCounter != null) {
            this.writeCounter[r][c]++;
        }
        return true;
    }

    /**
     * Reads a single byte from the specified absolute address.
     * Handles mapping the linear address to the 2D array.
     * @param address The absolute byte offset from the beginning of the disk.
     * @return The byte read, or 0 if the address is out of bounds or disk uninitialized.
     */
    public byte readAt(long address) {
        if (this.disk == null || this.diskSize == 0) return 0; // Not initialized

        long rowLong = address / this.diskSize;
        long colLong = address % this.diskSize;

        // Check bounds using long comparison first
        if (rowLong < 0 || rowLong >= this.diskSize || colLong < 0 || colLong >= this.diskSize) {
             // System.err.println("Read out of bounds: address=" + address + ", size=" + diskSize);
            return 0; // Or throw exception? Returning 0 is safer for some ops.
        }

        // Cast to int ONLY for array indexing, after bounds check
        int r = (int) rowLong;
        int c = (int) colLong;

        if (this.readCounter != null) {
            this.readCounter[r][c]++;
        }
        return this.disk[r][c];
    }

    /**
     * Writes an array of bytes starting at the specified address.
     * @param startAddress The starting absolute byte offset.
     * @param data The byte array to write.
     * @return true if all bytes were written successfully, false otherwise (e.g., out of bounds).
     */
    public boolean batchFill(long startAddress, byte[] data) {
        if (data == null) return true; // Nothing to write
        for (int i = 0; i < data.length; i++) {
            if (!writeAt(startAddress + i, data[i])) {
                System.err.println("Batch fill failed at offset " + i + " (address " + (startAddress + i) + ")");
                return false; // Stop and report failure if any write fails
            }
        }
        return true;
    }

    /**
     * Reads bytes into the provided data array starting from the specified address.
     * @param startAddress The starting absolute byte offset.
     * @param data The byte array to fill with read data. Its length determines how many bytes are read.
     * @return true if all bytes were read successfully (even if address was out of bounds, resulting in 0s),
     * false if the `data` array itself is null.
     */
    public boolean batchRead(long startAddress, byte[] data) {
        if (data == null) return false; // Cannot read into null array
        for (int i = 0; i < data.length; i++) {
            // readAt handles bounds checking internally and returns 0 for OOB
            data[i] = readAt(startAddress + i);
        }
        return true;
    }

    // --- Helper Methods for Reading/Writing Data Types ---

    private int readInt(long offset) {
        ensureFormatted(); // Basic check
        byte[] buf = new byte[4];
        // Read 4 bytes starting from offset
        for (int i = 0; i < 4; i++) {
             // readAt handles bounds check, will return 0 if OOB
             buf[i] = readAt(offset + i);
        }
        // Use ByteBuffer to convert byte array (Big Endian by default) to int
        return ByteBuffer.wrap(buf).getInt();
    }

    private void writeInt(long offset, int value) {
        ensureFormatted(); // Basic check
        // Allocate a 4-byte buffer, put the int, convert to byte array (Big Endian)
        byte[] buf = ByteBuffer.allocate(4).putInt(value).array();
        // Write the 4 bytes starting at offset
        batchFill(offset, buf); // batchFill handles internal writeAt calls and checks
    }

     private long readLong(long offset) {
        ensureFormatted(); // Basic check
        byte[] buf = new byte[8];
        for (int i = 0; i < 8; i++) {
             buf[i] = readAt(offset + i);
        }
        return ByteBuffer.wrap(buf).getLong();
    }

    private void writeLong(long offset, long value) {
        ensureFormatted(); // Basic check
        byte[] buf = ByteBuffer.allocate(8).putLong(value).array();
        batchFill(offset, buf);
    }

    private String readString(long offset, int length) {
        ensureFormatted();
        byte[] buf = new byte[length];
        batchRead(offset, buf);
        // Trim null characters from the end
        int actualLength = length;
        while (actualLength > 0 && buf[actualLength - 1] == 0) {
            actualLength--;
        }
        return new String(buf, 0, actualLength, StandardCharsets.UTF_8);
    }

    private void writeString(long offset, String value, int fixedLength) {
        ensureFormatted();
        byte[] stringBytes = value.getBytes(StandardCharsets.UTF_8);
        byte[] buffer = new byte[fixedLength]; // Initialize with zeros

        // Copy string bytes, truncating if necessary
        int bytesToCopy = Math.min(stringBytes.length, fixedLength);
        System.arraycopy(stringBytes, 0, buffer, 0, bytesToCopy);

        // Fill remaining space with null bytes (already done by initialization)

        batchFill(offset, buffer);
    }


    // --- Core VFS Operations ---

    /**
     * Formats the VFS, initializing the disk and writing the header.
     * This DELETES all existing data.
     * @param totalBytes The desired total capacity in bytes. The actual capacity will be size*size where size = floor(sqrt(totalBytes)).
     */
    public void format(long totalBytes) {
        System.out.println("Formatting VFS with requested capacity " + totalBytes + " bytes.");
        if (totalBytes <= HEADER_SIZE) {
            throw new IllegalArgumentException("Requested size (" + totalBytes + " bytes) is too small to even hold the header (" + HEADER_SIZE + " bytes).");
        }

        // Calculate square dimension size
        this.diskSize = (int) Math.floor(Math.sqrt(totalBytes));
        if (this.diskSize <= 0) {
             throw new IllegalArgumentException("Calculated disk dimension (size) must be positive.");
        }
        long actualCapacity = (long)this.diskSize * this.diskSize;

        System.out.println("Initializing disk with dimensions: " + this.diskSize + "x" + this.diskSize + " (" + actualCapacity + " bytes)");

        // Initialize the disk array (filled with 0s by default)
        this.disk = new byte[this.diskSize][this.diskSize];
        initializeCounters(); // Reset counters

        System.out.println("Writing VFS Header...");

        // --- Write Header Fields ---
        // Magic Number
        batchFill(OFF_MAGIC, "ATLAS_VFS2".getBytes(StandardCharsets.US_ASCII));

        // Version (Example: 1.1.0)
        writeAt(OFF_VERSION_MAJOR, (byte)1);
        writeAt(OFF_VERSION_MINOR, (byte)1);
        writeAt(OFF_VERSION_PATCH, (byte)0);


        // Pointer Length (Using 4 bytes/32-bit for addresses in this version)
        // If we need > 4GB files or > 4 billion files, this needs changing.
        // Original code used int (4 bytes) for pointers like start_addr.
        byte pointerLength = 4;
        writeAt(OFF_PTR_LEN, pointerLength);

        // Max File Size (Using original value: ~2GB)
        int maxFileBytes = Integer.MAX_VALUE; // Example limit per file
        writeInt(OFF_MAX_FILE_BYTES, maxFileBytes);

        // Max Number of Files (Determine based on available space after header)
        // Calculate space available for table and data
        long spaceAfterHeader = actualCapacity - HEADER_SIZE;
        // Estimate max files based on entry size, leaving some room for data
        // This is a heuristic; a fixed number or percentage might be better.
        // Let's use the original hardcoded default approach for simplicity now.
        int maxFiles = 127; // From original byte[]{0, 0, 0, Byte.MAX_VALUE}
        writeInt(OFF_MAX_FILES, maxFiles);


        // Calculate Header End and Table End addresses
        int headerEndAddress = HEADER_SIZE; // First byte *after* the header
        int tableEndAddress = headerEndAddress + (maxFiles * FILE_ENTRY_SIZE);

        // Check if table fits
        if (tableEndAddress >= actualCapacity) {
             throw new IllegalArgumentException("Disk size (" + actualCapacity + " bytes) is too small to hold the header and the file table for " + maxFiles + " files.");
        }

        // Write Header End Pointer
        writeInt(OFF_HEADER_END_PTR, headerEndAddress);

        // Write Table End Pointer
        writeInt(OFF_TABLE_END_PTR, tableEndAddress);

        // Initialize Last Data Pointer to point right after the file table
        // Data starts here. Use 'long' for this pointer.
        writeLong(OFF_LAST_DATA_PTR, (long)tableEndAddress);


        System.out.println("VFS Formatted. Header Size: " + headerEndAddress + " bytes. Max Files: " + maxFiles + ". Table End: " + tableEndAddress + ". Data Area Start: " + tableEndAddress);
    }

    /**
     * Writes data to a file. If the file exists, it's overwritten.
     * If it doesn't exist, a new file entry is created.
     * Uses a simple contiguous allocation strategy (find first fit).
     * @param fileName The name of the file (max 64 bytes UTF-8).
     * @param data The byte array containing the file content.
     * @return true if successful, false otherwise (e.g., no space, invalid name, too large).
     */
    public boolean write(String fileName, byte[] data) {
        ensureFormatted();
        if (fileName == null || fileName.trim().isEmpty()) {
            System.err.println("Write Error: File name cannot be empty.");
            return false;
        }
        if (data == null) {
            data = new byte[0]; // Allow writing empty files
        }

        // Validate file name length
        if (fileName.getBytes(StandardCharsets.UTF_8).length > 64) {
            System.err.println("Write Error: File name exceeds 64 bytes in UTF-8 encoding.");
            return false;
        }

        // Read header info needed for writing
        int headerEndAddress = readInt(OFF_HEADER_END_PTR);
        int tableEndAddress = readInt(OFF_TABLE_END_PTR);
        int maxFiles = readInt(OFF_MAX_FILES);
        int maxFileBytes = readInt(OFF_MAX_FILE_BYTES);
        long totalDiskSizeBytes = (long)this.diskSize * this.diskSize;


        if (data.length > maxFileBytes) {
             System.err.println("Write Error: Data size (" + data.length + ") exceeds maximum file size (" + maxFileBytes + ").");
             return false;
        }


        // --- Find or Create File Table Entry ---
        int fileEntrySlot = -1;
        long fileEntryOffset = -1;
        int existingFileSize = -1;
        long existingFileStartAddr = -1;

        // Scan table for existing file or a free slot
        int firstFreeSlot = -1;
        long firstFreeSlotOffset = -1;

        for (int i = 0; i < maxFiles; i++) {
            long currentEntryOffset = (long)headerEndAddress + ((long)i * FILE_ENTRY_SIZE);
            byte exists = readAt(currentEntryOffset + FENTRY_OFF_EXISTS);

            if (exists == 1) {
                String currentFileName = readString(currentEntryOffset + FENTRY_OFF_FILENAME, 64);
                if (currentFileName.equals(fileName)) {
                    // Found existing file
                    fileEntrySlot = i;
                    fileEntryOffset = currentEntryOffset;
                    existingFileSize = readInt(currentEntryOffset + FENTRY_OFF_SIZE_BYTES);
                    existingFileStartAddr = readInt(currentEntryOffset + FENTRY_OFF_START_ADDR); // Read as int, store as long
                    System.out.println("Found existing file '" + fileName + "' at slot " + i + ". Overwriting.");
                    break; // Stop searching
                }
            } else {
                // Found a free slot
                if (firstFreeSlot == -1) {
                    firstFreeSlot = i;
                    firstFreeSlotOffset = currentEntryOffset;
                    // Don't break, keep checking if the file already exists elsewhere
                }
            }
        }

        // Decide which slot to use
        if (fileEntryOffset == -1) { // Existing file not found
            if (firstFreeSlot != -1) {
                // Use the first free slot found
                fileEntrySlot = firstFreeSlot;
                fileEntryOffset = firstFreeSlotOffset;
                existingFileSize = -1; // Mark as new file
                existingFileStartAddr = -1;
                 System.out.println("Using free slot " + fileEntrySlot + " for new file '" + fileName + "'.");
            } else {
                // No existing file and no free slots
                System.err.println("Write Error: File table is full. Cannot write '" + fileName + "'.");
                return false;
            }
        }

        // --- Allocate Space for Data ---
        // Simple "first fit" allocation strategy after the table end.
        // IMPORTANT: This doesn't reuse space freed by delete/overwrite unless optimizeDisk is run.
        // A more robust FS would use a free list or bitmap.

        // Zero out old data if overwriting
        // This helps prevent reading stale data if the new write fails partway,
        // but it adds write overhead. Optional.
        if (existingFileSize > 0 && existingFileStartAddr >= tableEndAddress) {
            System.out.println("Zeroing out old data blocks for '" + fileName + "'...");
            byte[] zeros = new byte[existingFileSize]; // Allocate zero buffer
            batchFill(existingFileStartAddr, zeros); // Fill old location with zeros
        }


        // Find contiguous free space
        long requiredSpace = data.length;
        long dataStartAddress = -1;

        // Start search from the end of the table.
        // This is very inefficient for fragmented disks!
        long searchStart = tableEndAddress; // Start searching after the file table

        // Linear scan for a large enough contiguous block of zeros
        // Warning: SLOW!
        // A more efficient way would be to use the OFF_LAST_DATA_PTR and just append,
        // relying on optimizeDisk to reclaim space. Let's try that simpler approach first.

        // ---- Simpler Append Allocation ----
        long lastDataPtr = readLong(OFF_LAST_DATA_PTR);
        if (lastDataPtr < tableEndAddress) { // Sanity check/reset if pointer is wrong
            lastDataPtr = tableEndAddress;
        }

        if (lastDataPtr + requiredSpace <= totalDiskSizeBytes) {
             dataStartAddress = lastDataPtr;
             // Write the new data
             if (!batchFill(dataStartAddress, data)) {
                 System.err.println("Write Error: Failed to write data content for '" + fileName + "'.");
                 // Attempt to rollback? Difficult without transactions.
                 // Best effort: Try to mark entry as invalid if we created it.
                 if (existingFileSize == -1) { // If it was a new file attempt
                     writeAt(fileEntryOffset + FENTRY_OFF_EXISTS, (byte) 0);
                 }
                 return false;
             }
             // Update the last data pointer in the header
             writeLong(OFF_LAST_DATA_PTR, dataStartAddress + requiredSpace);
              System.out.println("Data written for '" + fileName + "' starting at address " + dataStartAddress);
        } else {
             System.err.println("Write Error: Not enough contiguous space found for '" + fileName + "' (" + requiredSpace + " bytes). Try running optimize.");
             // TODO: Implement the slow "first-fit" scan as a fallback? Or just fail?
             // For now, we fail if simple append doesn't work.
             return false;
        }
        // ---- End Simpler Append Allocation ----


        // --- Update File Table Entry ---
        long now = System.currentTimeMillis();
        writeLong(fileEntryOffset + FENTRY_OFF_ID, fileEntrySlot); // Use slot index as ID
        writeAt(fileEntryOffset + FENTRY_OFF_EXISTS, (byte) 1);
        writeInt(fileEntryOffset + FENTRY_OFF_SIZE_BYTES, data.length);
        writeInt(fileEntryOffset + FENTRY_OFF_SIZE_BLOCKS, data.length); // Keep original logic, though redundant

        if (existingFileSize == -1) { // Only set created time if it's a new file
            writeLong(fileEntryOffset + FENTRY_OFF_CREATED_MS, now);
        }
        writeLong(fileEntryOffset + FENTRY_OFF_ACCESSED_MS, now);
        writeLong(fileEntryOffset + FENTRY_OFF_MODIFIED_MS, now);
        writeInt(fileEntryOffset + FENTRY_OFF_START_ADDR, (int)dataStartAddress); // Store start address as int
        writeString(fileEntryOffset + FENTRY_OFF_FILENAME, fileName, 64);

        System.out.println("File entry updated for '" + fileName + "'.");
        return true;
    }


     /**
     * Reads the content of a file.
     * @param fileName The name of the file.
     * @return A String containing the file content, or null if the file is not found or deleted.
     */
    public String read(String fileName) {
        byte[] data = readFileBytes(fileName);
        if (data != null) {
            return new String(data, StandardCharsets.UTF_8);
        }
        return null;
    }

    /**
     * Reads the content of a file as raw bytes.
     * Updates the last accessed timestamp.
     * @param fileName The name of the file.
     * @return A byte array containing the file content, or null if the file is not found or deleted.
     */
    public byte[] readFileBytes(String fileName) {
        ensureFormatted();
        if (fileName == null || fileName.trim().isEmpty()) {
            return null;
        }

        int headerEndAddress = readInt(OFF_HEADER_END_PTR);
        int maxFiles = readInt(OFF_MAX_FILES);

        // Scan table for the file
        for (int i = 0; i < maxFiles; i++) {
            long currentEntryOffset = (long)headerEndAddress + ((long)i * FILE_ENTRY_SIZE);
            byte exists = readAt(currentEntryOffset + FENTRY_OFF_EXISTS);

            if (exists == 1) {
                String currentFileName = readString(currentEntryOffset + FENTRY_OFF_FILENAME, 64);
                if (currentFileName.equals(fileName)) {
                    // Found the file
                    int dataLength = readInt(currentEntryOffset + FENTRY_OFF_SIZE_BYTES);
                    int startAddress = readInt(currentEntryOffset + FENTRY_OFF_START_ADDR); // Read as int

                    if (dataLength < 0) { // Corrupted entry?
                         System.err.println("Read Error: Invalid file size (" + dataLength + ") for '" + fileName + "'");
                         return null;
                    }
                     if (startAddress < headerEndAddress) { // Data pointer seems invalid
                          System.err.println("Read Error: Invalid start address (" + startAddress + ") for '" + fileName + "'");
                          return null;
                     }


                    byte[] data = new byte[dataLength];
                    if (!batchRead(startAddress, data)) {
                        // Should not happen with current batchRead logic unless data array is huge?
                         System.err.println("Read Error: Failed during batch read for '" + fileName + "'");
                         return null; // Indicate error
                    }

                    // Update last accessed time
                    writeLong(currentEntryOffset + FENTRY_OFF_ACCESSED_MS, System.currentTimeMillis());

                    return data;
                }
            }
        }

        // File not found
        return null;
    }

    /**
     * Deletes a file by marking its entry as non-existent in the file table.
     * Note: This does NOT zero out the actual file data blocks on disk (fragmentation).
     * Run optimizeDisk() to potentially reclaim the space.
     * @param fileName The name of the file to delete.
     * @return true if the file was found and marked for deletion, false otherwise.
     */
    public boolean delete(String fileName) {
         ensureFormatted();
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }

        int headerEndAddress = readInt(OFF_HEADER_END_PTR);
        int maxFiles = readInt(OFF_MAX_FILES);

        // Scan table for the file
        for (int i = 0; i < maxFiles; i++) {
            long currentEntryOffset = (long)headerEndAddress + ((long)i * FILE_ENTRY_SIZE);
            byte exists = readAt(currentEntryOffset + FENTRY_OFF_EXISTS);

            if (exists == 1) {
                String currentFileName = readString(currentEntryOffset + FENTRY_OFF_FILENAME, 64);
                if (currentFileName.equals(fileName)) {
                    // Found the file, mark as deleted
                    writeAt(currentEntryOffset + FENTRY_OFF_EXISTS, (byte) 0);
                     System.out.println("Marked '" + fileName + "' as deleted.");
                    // We could potentially zero out the entry fields here too for tidiness
                    // but just marking exists=0 is sufficient for logic.
                    return true;
                }
            }
        }

        // File not found
        return false;
    }

    /**
     * Represents a segment of file data on disk. Used internally by optimizeDisk.
     */
    private static class FileSegment {
        int slotIndex;         // The index in the file table
        long currentStartAddress; // Where it currently starts
        int length;             // Size in bytes

        FileSegment(int slotIndex, long currentStartAddress, int length) {
            this.slotIndex = slotIndex;
            this.currentStartAddress = currentStartAddress;
            this.length = length;
        }
    }

    public boolean exists(String fileName) {
        ensureFormatted();
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }

        int headerEndAddress = readInt(OFF_HEADER_END_PTR);
        int maxFiles = readInt(OFF_MAX_FILES);

        // Scan table for the file
        for (int i = 0; i < maxFiles; i++) {
            long currentEntryOffset = (long)headerEndAddress + ((long)i * FILE_ENTRY_SIZE);
            byte exists = readAt(currentEntryOffset + FENTRY_OFF_EXISTS);
            if (exists == 1) {
                return true;
            }
        }

        // File not found
        return false;
    }

    /**
     * Defragments the data area of the disk.
     * It collects all active file segments, sorts them by their current position,
     * and moves them contiguously starting right after the file table.
     * Updates file table entries with the new start addresses.
     * Updates the header's Last Data Pointer.
     * Warning: This can be a very I/O intensive operation.
     */
    public void optimizeDisk() {
        ensureFormatted();
        System.out.println("Starting disk optimization (defragmentation)...");

        int headerEndAddress = readInt(OFF_HEADER_END_PTR);
        int tableEndAddress = readInt(OFF_TABLE_END_PTR);
        int maxFiles = readInt(OFF_MAX_FILES);
        long totalDiskSizeBytes = (long)this.diskSize * this.diskSize;


        // 1. Collect all active file segments
        List<FileSegment> segments = new ArrayList<>();
        for (int i = 0; i < maxFiles; i++) {
            long currentEntryOffset = (long)headerEndAddress + ((long)i * FILE_ENTRY_SIZE);
            byte exists = readAt(currentEntryOffset + FENTRY_OFF_EXISTS);
            if (exists == 1) {
                int length = readInt(currentEntryOffset + FENTRY_OFF_SIZE_BYTES);
                int startAddrInt = readInt(currentEntryOffset + FENTRY_OFF_START_ADDR); // Read as int
                if (length > 0 && startAddrInt >= tableEndAddress) { // Basic validation
                    segments.add(new FileSegment(i, startAddrInt, length));
                } else if (length > 0) {
                     System.err.println("Warning: Skipping segment for file in slot " + i + " due to invalid start address ("+startAddrInt+") or size ("+length+").");
                }
            }
        }

        if (segments.isEmpty()) {
            System.out.println("No active file segments found to optimize.");
            // Reset Last Data Pointer to just after table
             writeLong(OFF_LAST_DATA_PTR, tableEndAddress);
            return;
        }

        // 2. Sort segments by their current start address
        segments.sort(Comparator.comparingLong(s -> s.currentStartAddress));

        // 3. Move segments contiguously starting after the table
        long nextFreeAddress = tableEndAddress; // Where the next segment should start
        long bytesMoved = 0;
        long bytesZeroed = 0;

        // Use a temporary buffer for moving data to avoid issues if source/dest overlap heavily
        // Find max segment size to allocate buffer once
        int maxSegmentSize = 0;
        for (FileSegment seg : segments) {
            if (seg.length > maxSegmentSize) {
                maxSegmentSize = seg.length;
            }
        }
        // Check if we have enough memory for the buffer
        byte[] moveBuffer = null;
        try {
             moveBuffer = new byte[maxSegmentSize];
             System.out.println("Allocated move buffer of size: " + maxSegmentSize + " bytes.");
        } catch (OutOfMemoryError oom) {
             System.err.println("Error: Not enough memory to allocate buffer for optimization (" + maxSegmentSize + " bytes). Aborting optimize.");
             // We could try moving block by block, but that's much more complex. Abort for now.
             return;
        }


        for (FileSegment seg : segments) {
            if (seg.currentStartAddress == nextFreeAddress) {
                // Segment is already in the correct place, just advance the pointer
                nextFreeAddress += seg.length;
                continue; // No move needed
            }

            // Need to move the segment
            System.out.println("Moving segment for slot " + seg.slotIndex + " from " + seg.currentStartAddress + " to " + nextFreeAddress + " (" + seg.length + " bytes)");

            // Read data into buffer
            if (!batchRead(seg.currentStartAddress, moveBuffer, seg.length)) { // Use specific length read
                 System.err.println("Error: Failed to read segment data during optimize. Aborting.");
                 return; // Abort on read failure
            }

            // Write data to new location
            if (!batchFill(nextFreeAddress, moveBuffer, seg.length)) { // Use specific length write
                 System.err.println("Error: Failed to write segment data during optimize. Disk might be inconsistent! Aborting.");
                 return; // Abort on write failure
            }
            bytesMoved += seg.length;

            // Update the file table entry with the new start address
            long entryOffset = (long)headerEndAddress + ((long)seg.slotIndex * FILE_ENTRY_SIZE);
            writeInt(entryOffset + FENTRY_OFF_START_ADDR, (int)nextFreeAddress); // Write new address

            // IMPORTANT: Zero out the *original* location AFTER successfully moving
            // This prevents leaving stale data behind.
            System.out.println("Zeroing old location " + seg.currentStartAddress + "...");
            batchFill(seg.currentStartAddress, new byte[seg.length]); // Fill with zeros
            bytesZeroed += seg.length;


            // Update the pointer for the next segment
            nextFreeAddress += seg.length;
        }

        // 4. Update the header's Last Data Pointer
        writeLong(OFF_LAST_DATA_PTR, nextFreeAddress);

        // 5. Zero out the remaining space from the new end of data to the end of the disk (optional but good practice)
        System.out.println("Zeroing space from " + nextFreeAddress + " to end of disk ("+totalDiskSizeBytes+")...");
        long zeroCount = 0;
        byte[] zeroBuf = new byte[4096]; // Zero out in chunks
        for(long addr = nextFreeAddress; addr < totalDiskSizeBytes; addr += zeroBuf.length) {
            int len = (int) Math.min(zeroBuf.length, totalDiskSizeBytes - addr);
            if (len <= 0) break;
            if (!batchFill(addr, zeroBuf, len)) {
                 System.err.println("Warning: Failed to zero out trailing space at address " + addr);
                 break; // Stop if zeroing fails
            }
            zeroCount += len;
        }

        System.out.println("Optimization complete. Bytes moved: " + bytesMoved + ". Old data bytes zeroed: " + bytesZeroed + ". Trailing bytes zeroed: "+ zeroCount + ". New data end pointer: " + nextFreeAddress);
    }

    // Helper for optimizeDisk to read/write specific lengths into/from buffer
    private boolean batchRead(long startAddress, byte[] buffer, int length) {
        if (buffer == null || length > buffer.length || length < 0) return false;
        for (int i = 0; i < length; i++) {
            buffer[i] = readAt(startAddress + i);
        }
        return true;
    }
     private boolean batchFill(long startAddress, byte[] buffer, int length) {
        if (buffer == null || length > buffer.length || length < 0) return false;
        for (int i = 0; i < length; i++) {
            if (!writeAt(startAddress + i, buffer[i])) {
                 System.err.println("Batch fill (length) failed at offset " + i + " (address " + (startAddress + i) + ")");
                return false;
            }
        }
        return true;
    }


    /**
     * Calculates the total number of bytes currently used by active files
     * according to the file table entries. Does not account for fragmentation.
     * @return Total bytes used by file data.
     */
    public long getUsedBytesLogical() {
        ensureFormatted();
        long usedBytes = 0;
        int headerEndAddress = readInt(OFF_HEADER_END_PTR);
        int maxFiles = readInt(OFF_MAX_FILES);

        for (int i = 0; i < maxFiles; i++) {
            long currentEntryOffset = (long)headerEndAddress + ((long)i * FILE_ENTRY_SIZE);
            byte exists = readAt(currentEntryOffset + FENTRY_OFF_EXISTS);
            if (exists == 1) {
                int size = readInt(currentEntryOffset + FENTRY_OFF_SIZE_BYTES);
                if (size > 0) {
                    usedBytes += size;
                }
            }
        }
        return usedBytes;
    }

     /**
     * Gets the total capacity of the formatted disk.
     * @return Total capacity in bytes, or 0 if not formatted.
     */
    public long getTotalBytes() {
        if (this.disk == null || this.diskSize == 0) {
            return 0;
        }
        return (long)this.diskSize * this.diskSize;
    }

    /**
     * Calculates the free space based on the logical usage reported by file entries.
     * Does not account for fragmentation.
     * @return Logically free bytes.
     */
    public long getFreeBytesLogical() {
        return getTotalBytes() - getUsedBytesLogical();
    }

     /**
     * Calculates the free space based on the high-water mark (Last Data Pointer).
     * This reflects space potentially available for simple append allocation.
     * @return Physically free bytes after the last known data block.
     */
    public long getFreeBytesPhysical() {
        ensureFormatted();
        long total = getTotalBytes();
        long lastData = readLong(OFF_LAST_DATA_PTR);
         if (lastData > total) return 0; // Pointer somehow beyond disk end
         if (lastData < HEADER_SIZE) return total - HEADER_SIZE; // Pointer invalid, assume only header used
        return total - lastData;
    }

    public ArrayList<String> list() {
        int headerEndAddress = readInt(OFF_HEADER_END_PTR);
        int maxFiles = readInt(OFF_MAX_FILES);
        ArrayList<String> fileNames = new ArrayList<>();
        for (int i = 0; i < maxFiles; i++) {
            long entryOffset = (long)headerEndAddress + ((long)i * VFS2.FILE_ENTRY_SIZE);
            if (readAt(entryOffset + VFS2.FENTRY_OFF_EXISTS) == 1) {
                fileNames.add(readString(entryOffset + VFS2.FENTRY_OFF_FILENAME, 64));
            }
        }
        return fileNames;
    }

    /**
     * Performs basic checks to see if the header looks initialized.
     * Checks magic number and if essential pointers are non-zero.
     * @return true if header seems valid, false otherwise.
     */
    private boolean validateHeaderBasics() {
        if (this.disk == null || this.diskSize == 0) return false;
        try {
             // Check Magic Number
            String magic = readString(OFF_MAGIC, LEN_MAGIC);
            if (!"ATLAS_VFS2".equals(magic)) {
                 System.err.println("Header Check Failed: Magic number mismatch. Expected 'ATLAS_VFS2', got '" + magic + "'");
                return false;
            }
             // Check essential pointers (should be > 0 after format)
             int headerEnd = readInt(OFF_HEADER_END_PTR);
             int tableEnd = readInt(OFF_TABLE_END_PTR);
             long lastData = readLong(OFF_LAST_DATA_PTR);
             if (headerEnd <= 0 || tableEnd <= headerEnd || lastData < tableEnd ) {
                 System.err.println("Header Check Failed: Pointers seem invalid (headerEnd="+headerEnd+", tableEnd="+tableEnd+", lastData="+lastData+").");
                 return false;
             }

        } catch (Exception e) {
             System.err.println("Header Check Failed: Exception during validation - " + e.getMessage());
             return false; // Error during read likely means invalid format
        }
        return true;
    }

    /**
     * Ensures the VFS appears to be formatted by checking if the disk exists
     * and performing a basic header validation. Throws IllegalStateException if not.
     */
    private void ensureFormatted() {
        if (this.disk == null || this.diskSize == 0) {
            throw new IllegalStateException("VFS is not loaded or formatted. Use format() or loadDisk() first.");
        }
        // Optionally add a stricter check like validateHeaderBasics() here if needed on every operation
         // if (!validateHeaderBasics()) {
         //     throw new IllegalStateException("VFS header validation failed. Disk may be corrupted or unformatted.");
         // }
    }

    /**
     * Formats a byte count into a human-readable string (KiB, MiB, GiB, etc.).
     * Includes the original byte count for precision.
     * Uses base 1024 (binary prefixes).
     *
     * @param bytes The number of bytes.
     * @return A formatted string like "1536 bytes (1.50 KiB)".
     */
    private static String formatBytes(long bytes) {
        if (bytes < 0) {
            // Handle negative values if they might occur (e.g., fragmentation calculation)
             return String.format("%d bytes (Negative? Recalculate?)", bytes);
        }
        if (bytes < 1024) {
            // Keep as bytes if less than 1 KiB
            return String.format("%d bytes", bytes);
        }

        // Using 1024 base (KiB, MiB, GiB)
        final String[] units = new String[]{"bytes", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB"};
        int unitIndex = 0;
        double value = bytes;

        // Iterate to find the appropriate unit (stop before dividing below 1.0 for the next unit)
        while (value >= 1024.0 && unitIndex < units.length - 1) {
            value /= 1024.0;
            unitIndex++;
        }

        // Format the output string: original bytes and human-readable with 2 decimal places
        return String.format("%d bytes (%.2f %s)", bytes, value, units[unitIndex]);
    }

    // --- Console Application ---

    public static void main(String[] args) {
        VFS2 vfs = new VFS2();
        Scanner scanner = new Scanner(System.in);
        System.out.println("--- ATLAS VFS2 Console ---");
        System.out.println("Type 'help' for commands.");
        String currentVfsFile = null; // Track loaded/saved file

        while (true) {
            System.out.print("> ");
            String command = scanner.next(); // Read command
            String line = scanner.nextLine().trim(); // Read the rest of the line for arguments

            try {
                switch (command.toLowerCase()) {
                    // Inside the main method, in the "help" case:
                    case "help":
                        System.out.println("Commands:");
                        System.out.println("  help                - Show this help message.");
                        System.out.println("  format <number> <unit> - Format a new VFS (e.g., 'format 100 m'). Destroys current VFS in memory.");
                        System.out.println("      Units: g (GiB), m (MiB), k (KiB), b (bytes)");
                        System.out.println("  load <filepath>     - Load VFS from a file. Discards current VFS in memory.");
                        System.out.println("  save [filepath]     - Save current VFS to a file. Uses last loaded/saved path if none provided.");
                        System.out.println("  info                - Show VFS properties (size, usage).");
                        System.out.println("  list                - List files in the VFS.");
                        System.out.println("  write <filename> <content> - Write content to a file (overwrites if exists).");
                        System.out.println("  read <filename>     - Read content of a file.");
                        System.out.println("  delete <filename>   - Delete a file (marks for deletion).");
                        System.out.println("  copy <src> <dest>   - Copy a file within the VFS.");
                        System.out.println("  optimize            - Defragment the disk. Can take time.");
                        System.out.println("  hash <vfsfile> <algo> - Calculate hash of a VFS file (MD5, SHA-1, SHA-256, SHA-512).");
                        System.out.println("  import <realfilepath> <vfsfile> [true|false] - Import file from disk to VFS. Optional hash check (default: false).");
                        System.out.println("  export <vfsfile> <realfilepath> [true|false] - Export file from VFS to disk. Optional hash check (default: false).");
                        System.out.println("  randomfill <filename_prefix> <size_bytes> <num_files> - Create multiple random files.");
                        System.out.println("  exit                - Exit the console.");
                        break;

                    // Inside the main method's while loop and switch statement:

                    // ... (other cases like format, load, save, info, list, write, read, delete, copy, optimize) ...

                    case "hash": {
                        // Usage: hash <vfsfilename> <algorithm>
                        String[] parts = line.split("\\s+", 2);
                        if (parts.length != 2) {
                            System.err.println("Usage: hash <vfsfilename> <algorithm>");
                            System.err.println("Supported algorithms: MD5, SHA-1, SHA-256, SHA-512");
                            break;
                        }
                        String vfsFileName = parts[0];
                        String algorithm = parts[1].toUpperCase(); // Standardize algorithm name

                        // Basic validation of algorithm string format
                        if (!(algorithm.equals("MD5") || algorithm.equals("SHA-1") ||
                                algorithm.equals("SHA-256") || algorithm.equals("SHA-512"))) {
                            System.err.println("Invalid or unsupported algorithm specified: " + parts[1]);
                            System.err.println("Please use one of: MD5, SHA-1, SHA-256, SHA-512");
                            break;
                        }

                        try {
                            vfs.ensureFormatted(); // Ensures VFS is active
                            String hashValue = vfs.hashFile(vfsFileName, algorithm);
                            if (hashValue != null) {
                                System.out.println("Hash (" + algorithm + ") for VFS file '" + vfsFileName + "': " + hashValue);
                            } else {
                                // vfs.hashFile() method should print specific errors (e.g., file not found, algorithm unsupported by provider)
                                System.err.println("Failed to calculate hash for '" + vfsFileName + "'. Check previous messages for details.");
                            }
                        } catch (IllegalStateException e) {
                            System.err.println("Hash Error: " + e.getMessage());
                        } catch (Exception e) { // Catch any other unexpected errors during the operation
                            System.err.println("An unexpected error occurred during hash operation: " + e.getMessage());
                            e.printStackTrace();
                        }
                        break;
                    }

                    case "import": { // Corresponds to vfs.fromDisk()
                        // Usage: import <realfilepath> <vfsfilename> [checkhash_true_false]
                        String[] parts = line.split("\\s+", 3); // Expect realpath, vfsname, and optional checkhash flag
                        if (parts.length < 2) {
                            System.err.println("Usage: import <realfilepath> <vfsfilename> [true|false]");
                            System.err.println("       (checkhash is an optional boolean, e.g., 'true' or 'false', defaults to false)");
                            break;
                        }
                        String realFilePath = parts[0];
                        String vfsFileName = parts[1];
                        boolean performCheckByHash = false;
                        if (parts.length == 3) {
                            performCheckByHash = "true".equalsIgnoreCase(parts[2]);
                        }

                        try {
                            vfs.ensureFormatted();
                            File realFile = new File(realFilePath); // Create a File object for the real file system path

                            // The fromDisk method will print detailed status messages
                            boolean success = vfs.fromDisk(vfsFileName, realFile, performCheckByHash);

                            if (success) {
                                System.out.println("Import command completed for '" + realFilePath + "' to VFS as '" + vfsFileName + "'.");
                            } else {
                                System.err.println("Import command failed for '" + realFilePath + "'. See previous messages for details.");
                            }
                        } catch (IllegalStateException e) {
                            System.err.println("Import Error: " + e.getMessage());
                        } catch (Exception e) {
                            System.err.println("An unexpected error occurred during import operation: " + e.getMessage());
                            e.printStackTrace();
                        }
                        break;
                    }

                    case "export": { // Corresponds to vfs.toDisk()
                        // Usage: export <vfsfilename> <realfilepath> [checkhash_true_false]
                        String[] parts = line.split("\\s+", 3); // Expect vfsname, realpath, and optional checkhash flag
                        if (parts.length < 2) {
                            System.err.println("Usage: export <vfsfilename> <realfilepath> [true|false]");
                            System.err.println("       (checkhash is an optional boolean, e.g., 'true' or 'false', defaults to false)");
                            break;
                        }
                        String vfsFileName = parts[0];
                        String realFilePath = parts[1];
                        boolean performCheckByHash = false;
                        if (parts.length == 3) {
                            performCheckByHash = "true".equalsIgnoreCase(parts[2]);
                        }

                        try {
                            vfs.ensureFormatted();
                            File realFile = new File(realFilePath); // Create a File object for the real file system path

                            // Pre-flight check: Ensure parent directory for the real file exists
                            File parentDir = realFile.getParentFile();
                            if (parentDir != null && !parentDir.exists()) {
                                System.out.println("Info: Parent directory '" + parentDir.getAbsolutePath() + "' does not exist. Attempting to create it.");
                                if (!parentDir.mkdirs()) {
                                    // Log a warning but proceed; FileOutputStream might still succeed or fail more explicitly
                                    System.err.println("Warning: Could not create parent directory hierarchy '" + parentDir.getAbsolutePath() + "'. Export might fail.");
                                }
                            }
                            // Pre-flight check: If the file exists, is it writable?
                            if (realFile.exists() && !realFile.canWrite()) {
                                System.err.println("Export Error: Destination file '" + realFilePath + "' exists but is not writable.");
                                break;
                            }

                            // The toDisk method will print detailed status messages
                            boolean success = vfs.toDisk(vfsFileName, realFile, performCheckByHash);

                            if (success) {
                                System.out.println("Export command completed for VFS file '" + vfsFileName + "' to '" + realFilePath + "'.");
                            } else {
                                System.err.println("Export command failed for VFS file '" + vfsFileName + "'. See previous messages for details.");
                            }
                        } catch (IllegalStateException e) {
                            System.err.println("Export Error: " + e.getMessage());
                        } catch (SecurityException se) {
                            System.err.println("Export Security Error: Could not write to '" + realFilePath + "'. " + se.getMessage());
                        } catch (Exception e) {
                            System.err.println("An unexpected error occurred during export operation: " + e.getMessage());
                            e.printStackTrace();
                        }
                        break;
                    }

                    // ... (case "randomfill", "exit", and default case) ...

                    case "format": {
                        String[] parts = line.split("\\s+");
                        if (parts.length != 2) {
                            System.err.println("Usage: format <number> <unit: g, m, k, b>");
                            break;
                        }
                        try {
                            long num = Long.parseLong(parts[0]);
                            String unit = parts[1].toLowerCase();
                            long bytes = switch (unit) {
                                case "g" -> num * 1024L * 1024L * 1024L;
                                case "m" -> num * 1024L * 1024L;
                                case "k" -> num * 1024L;
                                case "b" -> num;
                                default -> {
                                    System.err.println("Invalid unit: " + unit + ". Use g, m, k, or b.");
                                    yield -1L;
                                }
                            };

                            if (bytes > 0) {
                                vfs = new VFS2(); // Create a new instance to clear old state
                                vfs.format(bytes);
                                currentVfsFile = null; // Reset file path after format
                                System.out.println("Formatted new VFS in memory. Size: " + vfs.diskSize + "x" + vfs.diskSize
                                                   + " (" + vfs.getTotalBytes() + " bytes).");
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid number: " + parts[0]);
                        } catch (IllegalArgumentException | IllegalStateException e) {
                             System.err.println("Formatting Error: " + e.getMessage());
                        }
                        break;
                    }

                    case "load": {
                         if (line.isEmpty()) {
                              System.err.println("Usage: load <filepath>");
                              break;
                         }
                         try {
                             vfs = new VFS2(); // Create new instance before loading
                             vfs.loadDisk(line);
                             currentVfsFile = line; // Store path on successful load
                         } catch (RuntimeException e) {
                             System.err.println("Load Error: " + e.getMessage());
                             vfs = new VFS2(); // Ensure vfs is reset to empty state on error
                             currentVfsFile = null;
                         }
                         break;
                    }

                    case "save": {
                         String savePath = line.isEmpty() ? currentVfsFile : line;
                         if (savePath == null || savePath.trim().isEmpty()) {
                              System.err.println("Error: No file path specified and no previous file loaded/saved.");
                              System.err.println("Usage: save [filepath]");
                              break;
                         }
                          try {
                             vfs.ensureFormatted(); // Check if there's something to save
                             vfs.saveDisk(savePath);
                             currentVfsFile = savePath; // Update path on successful save
                         } catch (Exception e) {
                             System.err.println("Save Error: " + e.getMessage());
                         }
                         break;
                    }

                     case "info": {
                         try {
                             vfs.ensureFormatted();
                             long totalBytes = vfs.getTotalBytes();
                             long usedLogical = vfs.getUsedBytesLogical();
                             long freeLogical = vfs.getFreeBytesLogical();
                             long lastData = vfs.readLong(OFF_LAST_DATA_PTR); // Address
                             long freePhysical = vfs.getFreeBytesPhysical();
                             int headerEnd = vfs.readInt(OFF_HEADER_END_PTR); // Address
                             int tableEnd = vfs.readInt(OFF_TABLE_END_PTR);   // Address
                             int maxFiles = vfs.readInt(OFF_MAX_FILES);
                             long tableSize = (long)maxFiles * FILE_ENTRY_SIZE; // Size
                             long fragmentation = freeLogical - freePhysical; // Size difference

                             System.out.println("--- VFS Info ---");
                             System.out.println("Status: Loaded" + (currentVfsFile != null ? " from " + currentVfsFile : " (in memory)"));
                             System.out.println("Dimensions: " + vfs.diskSize + "x" + vfs.diskSize);
                             System.out.println("Total Capacity: " + formatBytes(totalBytes)); // Formatted
                             System.out.println("Header End Address: " + headerEnd + " bytes"); // Keep address as raw number
                             System.out.println("File Table Size: " + formatBytes(tableSize) + " (Max Files: " + maxFiles + ", End Addr: " + tableEnd + ")"); // Formatted size, raw address
                             System.out.println("Data Area Start Address: " + tableEnd + " bytes"); // Keep address as raw number
                             System.out.println("--- Usage ---");
                             System.out.println("Logical Used (Sum of file sizes): " + formatBytes(usedLogical)); // Formatted
                             System.out.println("Logical Free (Total - Logical Used): " + formatBytes(freeLogical)); // Formatted
                             System.out.println("Last Data Pointer (High-water mark): " + lastData); // Keep address as raw number
                             System.out.println("Physical Free (Total - Last Data Ptr): " + formatBytes(freePhysical)); // Formatted
                             System.out.println("Fragmentation Estimate: " + formatBytes(fragmentation)); // Formatted
                         } catch (IllegalStateException e) {
                             System.err.println("Info Error: " + e.getMessage());
                         }
                         break;
                     }

                    case "list": {
                        try {
                            vfs.ensureFormatted();
                            int headerEndAddress = vfs.readInt(OFF_HEADER_END_PTR);
                            int maxFiles = vfs.readInt(OFF_MAX_FILES);
                            System.out.println("--- File List ---");
                            int fileCount = 0;
                            long totalSize = 0;
                            System.out.printf("%-3s %-20s %-10s %-10s %-25s%n", "Idx", "Name", "Size (B)", "Start Addr", "Modified"); // Header
                            for (int i = 0; i < maxFiles; i++) {
                                long entryOffset = (long)headerEndAddress + ((long)i * VFS2.FILE_ENTRY_SIZE);
                                if (vfs.readAt(entryOffset + VFS2.FENTRY_OFF_EXISTS) == 1) {
                                    String name = vfs.readString(entryOffset + VFS2.FENTRY_OFF_FILENAME, 64);
                                    int len = vfs.readInt(entryOffset + VFS2.FENTRY_OFF_SIZE_BYTES);
                                    int startAddr = vfs.readInt(entryOffset + FENTRY_OFF_START_ADDR);
                                    long modifiedMs = vfs.readLong(entryOffset + FENTRY_OFF_MODIFIED_MS);
                                    String modDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(modifiedMs));

                                     System.out.printf("%-3d %-20s %-10s %-10d %-25s%n", i, name, formatBytes(len), startAddr, modDate);
                                    fileCount++;
                                    totalSize += len;
                                }
                            }
                            System.out.println("-----------------");
//                            System.out.println("Total Files: " + fileCount + ", Total Size: " + totalSize + " bytes");
                            System.out.println("Total Files: " + fileCount + ", Total Size: " + formatBytes(totalSize));
                        } catch (IllegalStateException e) {
                            System.err.println("List Error: " + e.getMessage());
                        }
                        break;
                    }

                    case "write": {
                        String[] parts = line.split("\\s+", 2); // Split into filename and content
                        if (parts.length != 2) {
                            System.err.println("Usage: write <filename> <content>");
                            break;
                        }
                        String fileName = parts[0];
                        String content = parts[1];
                         try {
                             boolean success = vfs.write(fileName, content.getBytes(StandardCharsets.UTF_8));
                             if (success) {
                                 System.out.println("Wrote " + content.getBytes(StandardCharsets.UTF_8).length + " bytes to '" + fileName + "'.");
                             } else {
                                 System.err.println("Failed to write '" + fileName + "'. Check error messages above.");
                             }
                        } catch (IllegalStateException e) {
                             System.err.println("Write Error: " + e.getMessage());
                         } catch (Exception e) { // Catch other potential runtime issues
                             System.err.println("Unexpected Write Error: " + e.getMessage());
                             e.printStackTrace(); // Print stack trace for unexpected errors
                         }
                        break;
                    }

                    case "read": {
                         if (line.isEmpty()) {
                              System.err.println("Usage: read <filename>");
                              break;
                         }
                         String fileName = line;
                          try {
                             String content = vfs.read(fileName);
                             if (content != null) {
                                 System.out.println(content);
                             } else {
                                 System.out.println("File '" + fileName + "' not found or empty.");
                             }
                        } catch (IllegalStateException e) {
                             System.err.println("Read Error: " + e.getMessage());
                         }
                        break;
                    }

                    case "delete": {
                         if (line.isEmpty()) {
                              System.err.println("Usage: delete <filename>");
                              break;
                         }
                         String fileName = line;
                         try {
                             boolean success = vfs.delete(fileName);
                             if (!success) {
                                 System.out.println("File '" + fileName + "' not found.");
                             }
                         } catch (IllegalStateException e) {
                              System.err.println("Delete Error: " + e.getMessage());
                         }
                        break;
                    }

                    case "copy": {
                        String[] parts = line.split("\\s+", 2);
                        if (parts.length != 2) {
                            System.err.println("Usage: copy <source_filename> <destination_filename>");
                            break;
                        }
                        String srcFileName = parts[0];
                        String dstFileName = parts[1];
                        try {
                             byte[] data = vfs.readFileBytes(srcFileName); // Use byte version to avoid encoding issues
                             if (data != null) {
                                 boolean success = vfs.write(dstFileName, data);
                                 if (success) {
                                     System.out.println("Copied '" + srcFileName + "' to '" + dstFileName + "'.");
                                 } else {
                                      System.err.println("Failed to write destination file '" + dstFileName + "'.");
                                 }
                             } else {
                                 System.out.println("Source file '" + srcFileName + "' not found.");
                             }
                         } catch (IllegalStateException e) {
                             System.err.println("Copy Error: " + e.getMessage());
                         } catch (Exception e) {
                             System.err.println("Unexpected Copy Error: " + e.getMessage());
                             e.printStackTrace();
                         }
                        break;
                    }

                   case "optimize": {
                        try {
                             vfs.ensureFormatted();
                             System.out.println("Disk usage before optimization:");
                             long freeBefore = vfs.getFreeBytesPhysical();
                             System.out.println(" Physical Free Space: " + freeBefore + " bytes");

                             vfs.optimizeDisk(); // optimizeDisk now prints its own progress

                             System.out.println("Disk usage after optimization:");
                             long freeAfter = vfs.getFreeBytesPhysical();
                             System.out.println(" Physical Free Space: " + freeAfter + " bytes");
                             System.out.println(" Space Reclaimed: " + (freeAfter - freeBefore) + " bytes");

                         } catch (IllegalStateException e) {
                             System.err.println("Optimize Error: " + e.getMessage());
                         } catch (Exception e) {
                             System.err.println("Unexpected Optimize Error: " + e.getMessage());
                             e.printStackTrace();
                         }
                         break;
                    }

                    case "randomfill": {
                        // Usage: randomfill <filename> <size in bytes> <number of files>
                        String[] parts = line.split("\\s+");
                        if (parts.length != 3) {
                            System.err.println("Usage: randomfill <filename> <size in bytes> <number of files>");
                            break;
                        }
                        String fileName = parts[0];
                        int size = Integer.parseInt(parts[1]);
                        int numFiles = Integer.parseInt(parts[2]);
                        Random random = new Random();
                        for (int i = 0; i < numFiles; i++) {
                            byte[] data = new byte[size];
                            random.nextBytes(data);
                            String randomFileName = fileName + "_" + i + ".bin";
                            boolean success = vfs.write(randomFileName, data);
                            if (success) {
                                System.out.println("Wrote " + size + " bytes to '" + randomFileName + "'.");
                            } else {
                                System.err.println("Failed to write '" + randomFileName + "'. Check error messages above.");
                            }
                        }
                        break;
                    }

                    case "exit":
                        System.out.println("Exiting VFS console.");
                        scanner.close();
                        return; // Exit the main loop

                    default:
                        System.out.println("Unknown command: '" + command + "'. Type 'help' for commands.");
                }
            } catch (Exception e) {
                // Catch-all for unexpected errors in command processing
                System.err.println("An unexpected error occurred: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}