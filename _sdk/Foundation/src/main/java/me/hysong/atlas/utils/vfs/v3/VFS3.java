package me.hysong.atlas.utils.vfs.v3;


import me.hysong.atlas.async.Promise;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.text.SimpleDateFormat;


public class VFS3 implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final byte FILE_STATUS_FREE = 0x00;
    public static final byte FILE_STATUS_USED = 0x01;
    public static final byte FILE_STATUS_RECOVERABLE_DELETED = 0x02;
    public static final byte FILE_STATUS_PREVIOUS_VERSION = 0x03;
    public static final byte FILE_STATUS_CORRUPT_OR_INVALID = (byte)0xFF;

    private byte[][] disk;
    private byte[] ioProcessorDedicatedStorage;
    private VFS3HeaderComposition headerComposition;
    private transient VFS3IOProcessor ioProcessor;
    private transient ClassLoader externalClassLoader;
    private String loadedVfsPath = null;

    private long headerSizeOnDisk;
    private long fatStartAddress;
    private long dataAreaStartAddress;
    private long ioProcessorStorageStartAddress = -1;
    private int ioProcessorStorageSize = 0;
    private long lastKnownDataEndPtr;

    public VFS3() { this.headerComposition = new VFS3HeaderComposition(); }

    public VFS3(String diskPath) {
        this.headerComposition = new VFS3HeaderComposition();
        loadDisk(diskPath);
    }

    public VFS3(VFS3HeaderComposition comp) { this.headerComposition = Objects.requireNonNull(comp); }

    public void setExternalClassLoader(ClassLoader cl) { this.externalClassLoader = cl; }

    private void instantiateAndLoadIOProcessor() throws Exception {
        String cn = this.headerComposition.getIoProcessorClassName();
        if (cn == null || cn.trim().isEmpty()) cn = VFS3GenericIOController.class.getName();
        this.headerComposition.setIoProcessorClassName(cn);
        try {
            Class<?> pc = (externalClassLoader != null) ? Class.forName(cn, true, externalClassLoader) : Class.forName(cn);
            if (VFS3IOProcessor.class.isAssignableFrom(pc)) {
                this.ioProcessor = (VFS3IOProcessor) pc.getDeclaredConstructor().newInstance();
                this.ioProcessor.onLoad(this.ioProcessorDedicatedStorage, this.headerComposition, this.headerComposition.getDiskSize());
            } else throw new IllegalArgumentException("Class " + cn + " does not implement VFS3IOProcessor.");
        } catch (Exception e) {
            System.err.println("Failed to instantiate IOProcessor '" + cn + "'. Using Generic. Error: " + e.getMessage());
            this.ioProcessor = new VFS3GenericIOController();
            this.headerComposition.setIoProcessorClassName(VFS3GenericIOController.class.getName());
            this.ioProcessor.onLoad(this.ioProcessorDedicatedStorage, this.headerComposition, this.headerComposition.getDiskSize());
        }
    }

    public void format(VFS3HeaderComposition comp) { /* ... (previous version, assumed correct) ... */
        this.headerComposition = Objects.requireNonNull(comp);
        System.out.println("Formatting VFS3 with disk size: " + formatBytes(this.headerComposition.getDiskSize()));

        try {
            instantiateAndLoadIOProcessor();
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up IOProcessor for format.", e);
        }

        this.ioProcessorStorageSize = this.ioProcessor.getRequiredDedicatedStorageSize(this.headerComposition, this.headerComposition.getDiskSize());
        this.headerSizeOnDisk = this.headerComposition.calculateSerializedHeaderSizeOnDisk();
        long minTotalFootprint = this.headerComposition.getMinimumTotalFootprintOnDisk(this.ioProcessorStorageSize);

        if (this.headerComposition.getDiskSize() < minTotalFootprint) {
            throw new IllegalArgumentException("Disk size (" + formatBytes(this.headerComposition.getDiskSize()) +
                    ") too small. Min required: " + formatBytes(minTotalFootprint));
        }
        int side = (int) Math.sqrt(this.headerComposition.getDiskSize());
        this.disk = new byte[side][side];
        if (this.ioProcessorStorageSize > 0) this.ioProcessorDedicatedStorage = new byte[this.ioProcessorStorageSize];
        else this.ioProcessorDedicatedStorage = null;
        this.ioProcessor.onFormat(this.ioProcessorDedicatedStorage, this.headerComposition, this.headerComposition.getDiskSize());

        ByteBuffer hw = ByteBuffer.allocate((int)this.headerSizeOnDisk);
        hw.put(this.headerComposition.getMagicNumberContent().getBytes(StandardCharsets.UTF_8));
        hw.put(this.headerComposition.getVersionNumber());
        hw.putInt(this.headerComposition.getMaxFilesCount());
        hw.putInt(this.headerComposition.getMaxFileNameLength());
        hw.putLong(this.headerComposition.getMaxFileSize());
        hw.putLong(this.headerComposition.getDiskSize());
        hw.put(this.headerComposition.isEnableFastRecovery() ? (byte)1 : (byte)0);
        String pn = this.headerComposition.getIoProcessorClassName();
        if (pn != null && !pn.trim().isEmpty()) {
            hw.put((byte)1); byte[] pnb = pn.getBytes(StandardCharsets.UTF_8);
            hw.putShort((short)pnb.length); hw.put(pnb);
        } else hw.put((byte)0);

        this.fatStartAddress = this.headerSizeOnDisk;
        long fatSize = this.headerComposition.getFileTableSizeBytes();
        if (this.ioProcessorStorageSize > 0) {
            this.ioProcessorStorageStartAddress = this.fatStartAddress + fatSize;
            this.dataAreaStartAddress = this.ioProcessorStorageStartAddress + this.ioProcessorStorageSize;
        } else {
            this.ioProcessorStorageStartAddress = -1;
            this.dataAreaStartAddress = this.fatStartAddress + fatSize;
        }
        this.lastKnownDataEndPtr = this.dataAreaStartAddress;

        hw.putLong(this.fatStartAddress); hw.putLong(this.dataAreaStartAddress);
        hw.putLong(this.ioProcessorStorageStartAddress); hw.putInt(this.ioProcessorStorageSize);
        hw.putLong(this.lastKnownDataEndPtr);
        directBatchFill(0, hw.array());

        int fes = this.headerComposition.getFileEntryByteSize(); byte[] ee = new byte[fes];
        for (int i = 0; i < this.headerComposition.getMaxFilesCount(); i++) {
            directBatchFill(this.fatStartAddress + ((long)i * fes), ee);
        }
        this.loadedVfsPath = null;
        System.out.println("VFS3 Formatted. HeaderOnDisk: " + formatBytes(this.headerSizeOnDisk) +
                ", FAT@ " + this.fatStartAddress + ", Data@ " + this.dataAreaStartAddress +
                ", LKD@ " + this.lastKnownDataEndPtr);
    }
    private void ensureFormatted() { /* ... (previous version) ... */
        if (this.disk == null || this.headerSizeOnDisk == 0 || this.ioProcessor == null) {
            throw new IllegalStateException("VFS is not loaded/formatted or IOProcessor not initialized.");
        }
    }
    private boolean directWriteAt(long a, byte d) {int s=disk.length; long r=a/s,c=a%s; if(r<0||r>=s||c<0||c>=s)return false; disk[(int)r][(int)c]=d; return true;}
    private byte directReadAt(long a) {int s=disk.length; long r=a/s,c=a%s; if(r<0||r>=s||c<0||c>=s)return 0; return disk[(int)r][(int)c];}
    private boolean directBatchFill(long sa, byte[] d) {for(int i=0;i<d.length;i++)if(!directWriteAt(sa+i,d[i]))return false; return true;}
    private boolean directBatchRead(long sa, byte[] d) {for(int i=0;i<d.length;i++)d[i]=directReadAt(sa+i); return true;}
    public byte[] readRawBytes(long sa, int l) {ensureFormatted(); return ioProcessor.readBytes(disk,ioProcessorDedicatedStorage,sa,l);}
    public long writeRawBytes(long sa, byte[] c) {ensureFormatted(); return ioProcessor.writeBytes(disk,ioProcessorDedicatedStorage,sa,c);}
    protected int readIntFromDisk(long o) {byte[]b=new byte[4];directBatchRead(o,b);return ByteBuffer.wrap(b).getInt();}
    protected void writeIntToDisk(long o,int v){directBatchFill(o,ByteBuffer.allocate(4).putInt(v).array());}
    protected long readLongFromDisk(long o){byte[]b=new byte[8];directBatchRead(o,b);return ByteBuffer.wrap(b).getLong();}
    protected void writeLongToDisk(long o,long v){directBatchFill(o,ByteBuffer.allocate(8).putLong(v).array());}
    protected byte readByteFromDisk(long o){return directReadAt(o);}
    protected void writeByteToDisk(long o,byte v){directWriteAt(o,v);}
    protected String readStringFromDisk(long o,int l){byte[]b=new byte[l];directBatchRead(o,b);int al=0;for(int i=0;i<l;i++){if(b[i]==0)break;al++;}return new String(b,0,al,StandardCharsets.UTF_8);}
    protected void writeStringToDisk(long o,String v,int fl){byte[]sb=v.getBytes(StandardCharsets.UTF_8);byte[]bf=new byte[fl];System.arraycopy(sb,0,bf,0,Math.min(sb.length,fl));directBatchFill(o,bf);}
    private void writeObject(ObjectOutputStream o)throws IOException{o.defaultWriteObject();}
    private void readObject(ObjectInputStream i)throws IOException,ClassNotFoundException{i.defaultReadObject();externalClassLoader=null;try{instantiateAndLoadIOProcessor();}catch(Exception e){throw new IOException("Fail reinit IOProc",e);}}
    public void loadDisk(String f){try(ObjectInputStream o=new ObjectInputStream(new FileInputStream(f))){VFS3 l=(VFS3)o.readObject();disk=l.disk;ioProcessorDedicatedStorage=l.ioProcessorDedicatedStorage;headerComposition=l.headerComposition;ioProcessor=l.ioProcessor;externalClassLoader=null;loadedVfsPath=f;headerSizeOnDisk=l.headerSizeOnDisk;fatStartAddress=l.fatStartAddress;dataAreaStartAddress=l.dataAreaStartAddress;ioProcessorStorageStartAddress=l.ioProcessorStorageStartAddress;ioProcessorStorageSize=l.ioProcessorStorageSize;lastKnownDataEndPtr=l.lastKnownDataEndPtr;System.out.println("VFS3 loaded from "+f+". LKD@ "+lastKnownDataEndPtr);}catch(Exception e){resetToDefaultState();throw new RuntimeException("Fail load VFS3 "+f,e);}}
    private void resetToDefaultState(){disk=null;ioProcessorDedicatedStorage=null;headerComposition=new VFS3HeaderComposition();ioProcessor=null;headerSizeOnDisk=0;fatStartAddress=0;dataAreaStartAddress=0;ioProcessorStorageStartAddress=-1;ioProcessorStorageSize=0;lastKnownDataEndPtr=0;loadedVfsPath=null;}
    public void saveDisk(String f){ensureFormatted();try(ObjectOutputStream o=new ObjectOutputStream(new FileOutputStream(f))){o.writeObject(this);System.out.println("VFS3 saved to "+f);loadedVfsPath=f;}catch(IOException e){throw new RuntimeException("Fail save VFS3 "+f,e);}}
    public boolean write(String fn,byte[]d){/* ... (previous version, assumed correct) ... */
        ensureFormatted();
        if(fn==null||fn.trim().isEmpty()){System.err.println("Write Err: Name empty");return false;}
        byte[]fnb=fn.getBytes(StandardCharsets.UTF_8);
        if(fnb.length>headerComposition.getMaxFileNameLength()){System.err.println("Write Err: Name too long");return false;}
        if(d==null)d=new byte[0];
        if(d.length>headerComposition.getMaxFileSize()){System.err.println("Write Err: Data too large");return false;}
        int ex=-1,fr=-1;
        for(int i=0;i<headerComposition.getMaxFilesCount();i++){
            long eo=fatStartAddress+((long)i*headerComposition.getFileEntryByteSize());
            byte st=readByteFromDisk(eo+headerComposition.getMaxFileNameLength()+Long.BYTES+Long.BYTES);
            if(st==FILE_STATUS_USED||st==FILE_STATUS_RECOVERABLE_DELETED||st==FILE_STATUS_PREVIOUS_VERSION){
                if(readStringFromDisk(eo,headerComposition.getMaxFileNameLength()).equals(fn))ex=i;
            }else if(st==FILE_STATUS_FREE&&fr==-1)fr=i;
        }
        int ts;boolean iu=(ex!=-1);
        if(iu){ts=ex;System.out.println("Updating '"+fn+"' in slot "+ts);}
        else{if(fr!=-1){ts=fr;System.out.println("Using slot "+ts+" for new '"+fn+"'.");}
        else{System.err.println("Write Err: FAT full");return false;}}
        long dwa=lastKnownDataEndPtr;
        if(dwa+d.length>headerComposition.getDiskSize()){System.err.println("Write Err: No space");return false;}
        long abwod=writeRawBytes(dwa,d);
        if(abwod<=0&&d.length>0){System.err.println("Write Err: IOProc wrote 0 bytes");return false;}
        long ebo=fatStartAddress+((long)ts*headerComposition.getFileEntryByteSize());long ct=System.currentTimeMillis();
        writeStringToDisk(ebo,fn,headerComposition.getMaxFileNameLength());
        long cffo=ebo+headerComposition.getMaxFileNameLength();
        writeLongToDisk(cffo,abwod);cffo+=Long.BYTES;
        writeLongToDisk(cffo,dwa);cffo+=Long.BYTES;
        writeByteToDisk(cffo,FILE_STATUS_USED);cffo+=Byte.BYTES;
        if(!iu)writeLongToDisk(cffo,ct);cffo+=Long.BYTES;
        writeLongToDisk(cffo,ct);
        lastKnownDataEndPtr=dwa+abwod;
        long lkdo=locateLastKnownDataEndPtrOffsetInHeader();
        if(lkdo!=-1)writeLongToDisk(lkdo,lastKnownDataEndPtr);
        else{System.err.println("Crit Err: No LKD offset");return false;}
        System.out.println("Wrote '"+fn+"' ("+formatBytes(abwod)+" disk) @ "+dwa);return true;
    }
    private long locateLastKnownDataEndPtrOffsetInHeader(){if(headerSizeOnDisk<Long.BYTES)return -1;return headerSizeOnDisk-Long.BYTES;}
    public byte[] readFileBytes(String fn){/* ... (previous version, assumed correct) ... */
        ensureFormatted();
        if(fn==null||fn.trim().isEmpty())return null;
        for(int i=0;i<headerComposition.getMaxFilesCount();i++){
            long ebo=fatStartAddress+((long)i*headerComposition.getFileEntryByteSize());
            long sto=ebo+headerComposition.getMaxFileNameLength()+Long.BYTES+Long.BYTES;
            byte st=readByteFromDisk(sto);
            if(st==FILE_STATUS_USED){
                if(readStringFromDisk(ebo,headerComposition.getMaxFileNameLength()).equals(fn)){
                    long fsd=readLongFromDisk(ebo+headerComposition.getMaxFileNameLength());
                    long fdo=readLongFromDisk(ebo+headerComposition.getMaxFileNameLength()+Long.BYTES);
                    if(fsd<0||fdo<dataAreaStartAddress){System.err.println("Read Err: Corrupt FAT for '"+fn+"'");return null;}
                    if(fsd>Integer.MAX_VALUE){System.err.println("Read Err: File too large for single chunk '"+fn+"'");return null;}
                    return readRawBytes(fdo,(int)fsd);
                }
            }
        }
        System.err.println("File not found: "+fn);return null;
    }
    public String read(String fileName) { byte[] d = readFileBytes(fileName); return d != null ? new String(d, StandardCharsets.UTF_8) : null; }

    // --- New/Updated Methods Start Here ---

    public boolean exists(String fileName) {
        ensureFormatted();
        if (fileName == null || fileName.trim().isEmpty()) return false;
        for (int i = 0; i < headerComposition.getMaxFilesCount(); i++) {
            long entryBaseOffset = fatStartAddress + ((long)i * headerComposition.getFileEntryByteSize());
            long statusOffset = entryBaseOffset + headerComposition.getMaxFileNameLength() + Long.BYTES + Long.BYTES;
            byte status = readByteFromDisk(statusOffset);
            if (status == FILE_STATUS_USED) {
                String currentName = readStringFromDisk(entryBaseOffset, headerComposition.getMaxFileNameLength());
                if (currentName.equals(fileName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean delete(String fileName) {
        ensureFormatted();
        if (fileName == null || fileName.trim().isEmpty()) return false;

        for (int i = 0; i < headerComposition.getMaxFilesCount(); i++) {
            long entryBaseOffset = fatStartAddress + ((long)i * headerComposition.getFileEntryByteSize());
            long nameOffset = entryBaseOffset;
            long sizeOffset = nameOffset + headerComposition.getMaxFileNameLength();
            long dataPtrOffset = sizeOffset + Long.BYTES;
            long statusOffset = dataPtrOffset + Long.BYTES;
            // Timestamps follow status

            byte currentStatus = readByteFromDisk(statusOffset);

            if (currentStatus == FILE_STATUS_USED) {
                String currentName = readStringFromDisk(nameOffset, headerComposition.getMaxFileNameLength());
                if (currentName.equals(fileName)) {
                    if (headerComposition.isEnableFastRecovery()) {
                        writeByteToDisk(statusOffset, FILE_STATUS_RECOVERABLE_DELETED);
                        System.out.println("File '" + fileName + "' marked as recoverable deleted.");
                    } else {
                        writeByteToDisk(statusOffset, FILE_STATUS_FREE);
                        // Optionally, could zero out other fields of the FAT entry for cleanliness
                        // writeStringToDisk(nameOffset, "", headerComposition.getMaxFileNameLength());
                        // writeLongToDisk(sizeOffset, 0);
                        // writeLongToDisk(dataPtrOffset, 0);
                        System.out.println("File '" + fileName + "' marked as free.");
                    }
                    // Timestamps could be updated here to reflect deletion time if needed
                    long modTimeOffset = statusOffset + Byte.BYTES + Long.BYTES; // created_ts, then modified_ts
                    writeLongToDisk(modTimeOffset, System.currentTimeMillis());

                    return true;
                }
            }
        }
        System.err.println("Delete Error: File '" + fileName + "' not found or not in a deletable state.");
        return false;
    }

    public List<String> list() {
        ensureFormatted();
        List<String> fileNames = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println(String.format("%-4s %-"+(headerComposition.getMaxFileNameLength()-2)+"s %-12s %-12s %-20s %-20s %-6s",
                "Idx", "Name", "Size (Disk)", "Offset", "Created", "Modified", "Status"));
        System.out.println(String.format("%-4s %-"+(headerComposition.getMaxFileNameLength()-2)+"s %-12s %-12s %-20s %-20s %-6s",
                "----", "----", "-----------", "------------", "--------------------", "--------------------", "------"));

        for (int i = 0; i < headerComposition.getMaxFilesCount(); i++) {
            long ebo = fatStartAddress + ((long)i * headerComposition.getFileEntryByteSize());
            long nameOff = ebo;
            long sizeOff = nameOff + headerComposition.getMaxFileNameLength();
            long dataOff = sizeOff + Long.BYTES;
            long statOff = dataOff + Long.BYTES;
            long createdOff = statOff + Byte.BYTES;
            long modifiedOff = createdOff + Long.BYTES;

            byte status = readByteFromDisk(statOff);
            String statusStr = switch (status) {
                case FILE_STATUS_FREE -> "FREE";
                case FILE_STATUS_USED -> "USED";
                case FILE_STATUS_RECOVERABLE_DELETED -> "DEL*";
                case FILE_STATUS_PREVIOUS_VERSION -> "OLD";
                default -> "UNK";
            };

            if (status != FILE_STATUS_FREE) { // List all non-free entries for info
                String name = readStringFromDisk(nameOff, headerComposition.getMaxFileNameLength());
                long size = readLongFromDisk(sizeOff);
                long offset = readLongFromDisk(dataOff);
                String created = sdf.format(new Date(readLongFromDisk(createdOff)));
                String modified = sdf.format(new Date(readLongFromDisk(modifiedOff)));

                System.out.println(String.format("%-4d %-"+(headerComposition.getMaxFileNameLength()-2)+"s %-12s %-12d %-20s %-20s %-6s",
                        i, name, formatBytes(size), offset, created, modified, statusStr));
                if (status == FILE_STATUS_USED) {
                    fileNames.add(name);
                }
            }
        }
        return fileNames; // Returns only USED files, but prints more info
    }

    public long getTotalBytes() {
        ensureFormatted();
        return headerComposition.getDiskSize();
    }

    public long getUsedBytesLogical() {
        ensureFormatted();
        long usedBytes = 0;
        for (int i = 0; i < headerComposition.getMaxFilesCount(); i++) {
            long entryBaseOffset = fatStartAddress + ((long)i * headerComposition.getFileEntryByteSize());
            long statusOffset = entryBaseOffset + headerComposition.getMaxFileNameLength() + Long.BYTES + Long.BYTES;
            byte status = readByteFromDisk(statusOffset);
            if (status == FILE_STATUS_USED) {
                long fileSize = readLongFromDisk(entryBaseOffset + headerComposition.getMaxFileNameLength());
                usedBytes += fileSize; // This is size on disk reported by IOProcessor
            }
        }
        return usedBytes;
    }

    public long getFreeBytesLogical() {
        return getTotalBytes() - getUsedBytesLogical(); // This is a bit misleading if files are compressed
    }

    public long getFreeBytesPhysicalAppend() {
        ensureFormatted();
        return getTotalBytes() - lastKnownDataEndPtr;
    }

    private static class FileSegmentToOptimize {
        int originalSlotIndex;
        String name;
        long originalDataOffset;
        long sizeOnDisk; // Actual size on disk
        byte[] data; // Temp storage for data during optimize
        long createdTimestamp;
        long modifiedTimestamp;

        FileSegmentToOptimize(int slot, String n, long off, long size, long created, long modified) {
            originalSlotIndex = slot; name = n; originalDataOffset = off;
            sizeOnDisk = size; createdTimestamp = created; modifiedTimestamp = modified;
        }
    }

    public void optimizeDisk() {
        ensureFormatted();
        System.out.println("Starting disk optimization...");
        List<FileSegmentToOptimize> activeSegments = new ArrayList<>();

        // 1. Collect all active file segments (FILE_STATUS_USED)
        for (int i = 0; i < headerComposition.getMaxFilesCount(); i++) {
            long ebo = fatStartAddress + ((long)i * headerComposition.getFileEntryByteSize());
            long nameOff = ebo;
            long sizeOff = nameOff + headerComposition.getMaxFileNameLength();
            long dataOff = sizeOff + Long.BYTES;
            long statOff = dataOff + Long.BYTES;
            long createdOff = statOff + Byte.BYTES;
            long modifiedOff = createdOff + Long.BYTES;

            byte status = readByteFromDisk(statOff);
            if (status == FILE_STATUS_USED) {
                String name = readStringFromDisk(nameOff, headerComposition.getMaxFileNameLength());
                long size = readLongFromDisk(sizeOff);
                long offset = readLongFromDisk(dataOff);
                long created = readLongFromDisk(createdOff);
                long modified = readLongFromDisk(modifiedOff);
                if (size > 0 && offset >= dataAreaStartAddress) {
                    activeSegments.add(new FileSegmentToOptimize(i, name, offset, size, created, modified));
                }
            }
        }

        if (activeSegments.isEmpty()) {
            System.out.println("No active files to optimize. Resetting data area.");
            this.lastKnownDataEndPtr = this.dataAreaStartAddress;
        } else {
            // Sort segments by their original data offset to read somewhat sequentially
            activeSegments.sort(Comparator.comparingLong(s -> s.originalDataOffset));

            // Read data for all segments first
            System.out.println("Reading data for " + activeSegments.size() + " active files...");
            for (FileSegmentToOptimize seg : activeSegments) {
                if (seg.sizeOnDisk > Integer.MAX_VALUE) {
                    System.err.println("Cannot optimize file " + seg.name + " larger than 2GB in one go. Skipping.");
                    // Mark this segment as non-optimizable or handle chunking (complex)
                    seg.data = null; // Indicate it couldn't be read
                    continue;
                }
                seg.data = readRawBytes(seg.originalDataOffset, (int)seg.sizeOnDisk);
                if (seg.data == null || seg.data.length != seg.sizeOnDisk) {
                    System.err.println("Error reading data for segment " + seg.name + " from offset " + seg.originalDataOffset + ". Skipping.");
                    seg.data = null; // Mark as failed
                }
            }

            // Clear the entire FAT first (mark all as FREE)
            System.out.println("Clearing File Allocation Table...");
            int fileEntrySize = this.headerComposition.getFileEntryByteSize();
            byte[] emptyEntry = new byte[fileEntrySize]; // status = FREE (0x00)
            for (int i = 0; i < this.headerComposition.getMaxFilesCount(); i++) {
                directBatchFill(this.fatStartAddress + ((long)i * fileEntrySize), emptyEntry);
            }

            // Write segments contiguously and update FAT
            System.out.println("Writing compacted data and updating FAT...");
            long currentNewDataOffset = this.dataAreaStartAddress;
            int newSlotIndex = 0;

            for (FileSegmentToOptimize seg : activeSegments) {
                if (seg.data == null || newSlotIndex >= headerComposition.getMaxFilesCount()) { // Skip if data read failed or no more FAT slots
                    if (seg.data == null) System.err.println("Skipping segment " + seg.name + " due to previous read error.");
                    if (newSlotIndex >= headerComposition.getMaxFilesCount()) System.err.println("No more FAT slots, cannot restore " + seg.name);
                    continue;
                }

                long bytesWritten = writeRawBytes(currentNewDataOffset, seg.data);
                if (bytesWritten != seg.sizeOnDisk) {
                    System.err.println("Error writing data for segment " + seg.name + " to offset " + currentNewDataOffset + ". Expected " + seg.sizeOnDisk + " got " + bytesWritten + ". VFS may be inconsistent.");
                    // Attempt to mark FAT entry as corrupt? Or just skip?
                    continue;
                }

                // Update new FAT entry
                long ebo = fatStartAddress + ((long)newSlotIndex * fileEntrySize);
                writeStringToDisk(ebo, seg.name, headerComposition.getMaxFileNameLength());
                long cffo = ebo + headerComposition.getMaxFileNameLength();
                writeLongToDisk(cffo, seg.sizeOnDisk); cffo += Long.BYTES;
                writeLongToDisk(cffo, currentNewDataOffset); cffo += Long.BYTES;
                writeByteToDisk(cffo, FILE_STATUS_USED); cffo += Byte.BYTES;
                writeLongToDisk(cffo, seg.createdTimestamp); cffo += Long.BYTES;
                writeLongToDisk(cffo, seg.modifiedTimestamp); // Or System.currentTimeMillis() if optimize considered a mod

                System.out.println("Moved '" + seg.name + "' to offset " + currentNewDataOffset + " (Slot " + newSlotIndex + ")");
                currentNewDataOffset += bytesWritten;
                newSlotIndex++;
            }
            this.lastKnownDataEndPtr = currentNewDataOffset;
        }

        // Update LKD pointer in header
        long lkdepOffset = locateLastKnownDataEndPtrOffsetInHeader();
        if (lkdepOffset != -1) writeLongToDisk(lkdepOffset, this.lastKnownDataEndPtr);
        else System.err.println("Critical Error: Could not update LKD pointer in header during optimize.");

        // Optionally zero out space from new LKD to end of data area or disk
        // For simplicity, we'll skip zeroing the tail for now. The space is available.
        System.out.println("Optimization complete. New last data pointer: " + this.lastKnownDataEndPtr);
    }

    private String calculateHash(byte[] data, String algorithm) throws NoSuchAlgorithmException {
        if (data == null) return null;
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] hashBytes = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public String hashFile(String vfsFileName, String algorithm) {
        ensureFormatted();
        Objects.requireNonNull(vfsFileName, "VFS file name cannot be null.");
        Objects.requireNonNull(algorithm, "Algorithm cannot be null.");
        try { MessageDigest.getInstance(algorithm); }
        catch (NoSuchAlgorithmException e) {
            System.err.println("Hash Error: Algorithm '" + algorithm + "' unsupported. " + e.getMessage());
            return null;
        }
        byte[] fileData = readFileBytes(vfsFileName);
        if (fileData == null) {
            System.err.println("Hash Error: File '" + vfsFileName + "' not found or could not be read.");
            return null;
        }
        try { return calculateHash(fileData, algorithm); }
        catch (NoSuchAlgorithmException e) { /* Should be caught above */ return null; }
    }

    public boolean fromDisk(String vfsFilename, File realFile, boolean checkByHash) {
        ensureFormatted();
        Objects.requireNonNull(vfsFilename, "VFS name cannot be null.");
        Objects.requireNonNull(realFile, "Real file cannot be null.");
        if (!realFile.exists() || !realFile.isFile() || !realFile.canRead()) {
            System.err.println("FromDisk Error: Real file '" + realFile.getAbsolutePath() + "' issue.");
            return false;
        }
        byte[] fileData;
        try {
            if (realFile.length() > headerComposition.getMaxFileSize()) {
                System.err.println("FromDisk Error: Real file too large ("+realFile.length()+" > "+headerComposition.getMaxFileSize()+").");
                return false;
            }
            fileData = Files.readAllBytes(realFile.toPath());
        } catch (IOException | OutOfMemoryError e) {
            System.err.println("FromDisk Error: Failed to read real file '" + realFile.getAbsolutePath() + "'. " + e.getMessage());
            return false;
        }
        boolean writeSuccess = write(vfsFilename, fileData);
        if (!writeSuccess) {
            System.err.println("FromDisk Error: Failed to write '" + vfsFilename + "' to VFS.");
            return false;
        }
        if (checkByHash) {
            System.out.println("FromDisk: Hash check for '" + vfsFilename + "'...");
            String sourceHash; String vfsHash; String algo = "SHA-256";
            try { sourceHash = calculateHash(fileData, algo); }
            catch (NoSuchAlgorithmException e) { System.err.println("FromDisk Hash Err: " + e.getMessage()); return false; }

            // Read back from VFS for verification (inefficient but thorough)
            byte[] vfsFileData = readFileBytes(vfsFilename);
            if (vfsFileData == null) { System.err.println("FromDisk Hash Err: Failed to read back VFS file."); return false; }
            try { vfsHash = calculateHash(vfsFileData, algo); }
            catch (NoSuchAlgorithmException e) { System.err.println("FromDisk Hash Err for VFS: " + e.getMessage()); return false; }

            if (sourceHash != null && sourceHash.equals(vfsHash)) System.out.println("FromDisk Hash OK.");
            else {
                System.err.println("FromDisk Hash MISMATCH for '" + vfsFilename + "'.");
                // delete(vfsFilename); // Optional: cleanup on mismatch
                return false;
            }
        }
        System.out.println("Imported '" + realFile.getAbsolutePath() + "' to VFS as '" + vfsFilename + "'.");
        return true;
    }

    public boolean toDisk(String vfsFileName, File realFileDest, boolean checkByHash) {
        ensureFormatted();
        Objects.requireNonNull(vfsFileName, "VFS name cannot be null.");
        Objects.requireNonNull(realFileDest, "Real file dest cannot be null.");

        byte[] vfsFileData = readFileBytes(vfsFileName);
        if (vfsFileData == null) {
            System.err.println("ToDisk Error: VFS file '" + vfsFileName + "' not found.");
            return false;
        }
        try {
            // Ensure parent directories exist for the destination file
            Path parentDir = Paths.get(realFileDest.getAbsolutePath()).getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            try (FileOutputStream fos = new FileOutputStream(realFileDest)) {
                fos.write(vfsFileData);
            }
        } catch (IOException | SecurityException e) {
            System.err.println("ToDisk Error: Failed to write to '" + realFileDest.getAbsolutePath() + "'. " + e.getMessage());
            return false;
        }
        if (checkByHash) {
            System.out.println("ToDisk: Hash check for '" + realFileDest.getName() + "'...");
            String vfsHash; String realFileHash; String algo = "SHA-256";
            try { vfsHash = calculateHash(vfsFileData, algo); }
            catch (NoSuchAlgorithmException e) { System.err.println("ToDisk Hash Err for VFS: " + e.getMessage()); return false; }

            byte[] realFileBytesReadBack;
            try { realFileBytesReadBack = Files.readAllBytes(realFileDest.toPath()); }
            catch (IOException | OutOfMemoryError e) { System.err.println("ToDisk Hash Err: Failed to read back real file. " + e.getMessage()); return false; }
            try { realFileHash = calculateHash(realFileBytesReadBack, algo); }
            catch (NoSuchAlgorithmException e) { System.err.println("ToDisk Hash Err for real file: " + e.getMessage()); return false; }

            if (vfsHash != null && vfsHash.equals(realFileHash)) System.out.println("ToDisk Hash OK.");
            else {
                System.err.println("ToDisk Hash MISMATCH for '" + realFileDest.getAbsolutePath() + "'.");
                // realFileDest.delete(); // Optional: cleanup on mismatch
                return false;
            }
        }
        System.out.println("Exported VFS file '" + vfsFileName + "' to '" + realFileDest.getAbsolutePath() + "'.");
        return true;
    }

    private ArrayList<File> rdTraverse(File source, int currentDepth, int maxDepth) {
        File[] files = source.listFiles();
        if (files == null) return new ArrayList<>();
        ArrayList<File> result = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory()) {
                if (maxDepth < 0 || currentDepth < maxDepth) { // maxDepth < 0 means infinite depth
                    result.addAll(rdTraverse(file, currentDepth + 1, maxDepth));
                }
                // Not adding directory itself if maxDepth is reached, only its files from previous levels
            } else if (file.isFile()) {
                result.add(file);
            }
        }
        return result;
    }

    public boolean imageFromRealDisk(File sourceDirectory, String pathPrefixToRemove, int maxDepth) {
        ensureFormatted();
        if (!sourceDirectory.isDirectory()) {
            System.err.println("ImageFromDisk Error: Source '" + sourceDirectory.getAbsolutePath() + "' is not a directory.");
            return false;
        }
        String prefix = pathPrefixToRemove;
        if (!prefix.endsWith(File.separator) && !prefix.isEmpty()) {
            prefix += File.separator;
        }

        ArrayList<File> filesToImport = rdTraverse(sourceDirectory, 0, maxDepth);
        boolean allSuccess = true;
        int successCount = 0;
        System.out.println("Found " + filesToImport.size() + " files to import from " + sourceDirectory.getAbsolutePath());

        for (File realFile : filesToImport) {
            String vfsPath = realFile.getAbsolutePath();
            if (vfsPath.startsWith(prefix)) {
                vfsPath = vfsPath.substring(prefix.length());
            }
            // Normalize path separators for VFS (e.g., always use '/')
            vfsPath = vfsPath.replace(File.separatorChar, '/');
            if (vfsPath.startsWith("/")) vfsPath = vfsPath.substring(1);


            System.out.println("Importing: " + realFile.getAbsolutePath() + " -> VFS:/" + vfsPath);
            if (fromDisk(vfsPath, realFile, false)) { // Hash check off by default for speed in mass import
                successCount++;
            } else {
                System.err.println("ImageFromDisk: Failed to import '" + realFile.getAbsolutePath() + "' as '" + vfsPath + "'.");
                allSuccess = false;
            }
        }
        System.out.println("ImageFromDisk: Successfully imported " + successCount + "/" + filesToImport.size() + " files.");
        return allSuccess;
    }

    public String getReportString() {
        ensureFormatted();
        StringBuilder sb = new StringBuilder();
        sb.append("--- VFS Info ---");
        sb.append("\n").append("Disk Size: ").append(formatBytes(getTotalBytes()));
        sb.append("\n").append("Header Size on Disk: ").append(formatBytes(headerSizeOnDisk));
        sb.append("\n").append("FAT Start Address: ").append(fatStartAddress);
        sb.append("\n").append("Data Area Start Address: ").append(dataAreaStartAddress);
        sb.append("\n").append("IO Proc Storage Start: ").append(ioProcessorStorageStartAddress == -1 ? "N/A" : ioProcessorStorageStartAddress).append(", Size: ").append(formatBytes(ioProcessorStorageSize));
        sb.append("\n").append("Last Known Data End Ptr: ").append(lastKnownDataEndPtr);
        sb.append("\n").append("Max Files: ").append(headerComposition.getMaxFilesCount());
        sb.append("\n").append("Max Filename Length: ").append(headerComposition.getMaxFileNameLength());
        sb.append("\n").append("Enable Fast Recovery: ").append(headerComposition.isEnableFastRecovery());
        sb.append("\n").append("IO Processor: ").append(ioProcessor.getClass().getName());
        sb.append("\n").append("--- Usage ---");
        sb.append("\n").append("Logical Used: ").append(formatBytes(getUsedBytesLogical()));
        sb.append("\n").append("Logical Free (Total - Used): ").append(formatBytes(getFreeBytesLogical()));
        sb.append("\n").append("Physical Free (Append): ").append(formatBytes(getFreeBytesPhysicalAppend()));
        sb.append("\n").append("Fragmentation (Est.): ").append(formatBytes(getFreeBytesPhysicalAppend() - getFreeBytesLogical()));
        return sb.toString();
    }

    public boolean imageToRealDisk(File targetDirectory) {
        ensureFormatted();
        if (!targetDirectory.exists()) {
            if (!targetDirectory.mkdirs()) {
                System.err.println("ImageToDisk Error: Could not create target directory '" + targetDirectory.getAbsolutePath() + "'.");
                return false;
            }
        }
        if (!targetDirectory.isDirectory()) {
            System.err.println("ImageToDisk Error: Target '" + targetDirectory.getAbsolutePath() + "' is not a directory.");
            return false;
        }

        List<String> vfsFiles = new ArrayList<>();
        // Temporarily modify list() to just get names without printing for this internal use
        for (int i = 0; i < headerComposition.getMaxFilesCount(); i++) {
            long ebo = fatStartAddress + ((long)i * headerComposition.getFileEntryByteSize());
            long statOff = ebo + headerComposition.getMaxFileNameLength() + Long.BYTES + Long.BYTES;
            if (readByteFromDisk(statOff) == FILE_STATUS_USED) {
                vfsFiles.add(readStringFromDisk(ebo, headerComposition.getMaxFileNameLength()));
            }
        }

        boolean allSuccess = true;
        int successCount = 0;
        System.out.println("Found " + vfsFiles.size() + " files to export to " + targetDirectory.getAbsolutePath());

        for (String vfsFileName : vfsFiles) {
            // Construct real file path, ensuring subdirectories are created
            Path realDestPath = (Path) Paths.get(targetDirectory.getAbsolutePath(), vfsFileName.replace('/', File.separatorChar));
            File realFileDest = realDestPath.toFile();

            System.out.println("Exporting: VFS:/" + vfsFileName + " -> " + realFileDest.getAbsolutePath());
            if (toDisk(vfsFileName, realFileDest, false)) { // Hash check off by default
                successCount++;
            } else {
                System.err.println("ImageToDisk: Failed to export '" + vfsFileName + "' to '" + realFileDest.getAbsolutePath() + "'.");
                allSuccess = false;
            }
        }
        System.out.println("ImageToDisk: Successfully exported " + successCount + "/" + vfsFiles.size() + " files.");
        return allSuccess;
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "i";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    public static void main(String[] args) {
        VFS3 vfs = new VFS3();
        Scanner scanner = new Scanner(System.in);
        System.out.println("--- ATLAS VFS3 Console ---");
        System.out.println("Type 'help' for commands.");
        VFS3HeaderComposition currentFormatComposition = new VFS3HeaderComposition();

        while (true) {
            System.out.print("vfs3> ");
            String commandWord = scanner.next().toLowerCase();
            String line = scanner.nextLine().trim();
            String[] parts = line.split("\\s+"); // General purpose split
            if (parts.length == 1 && parts[0].isEmpty()) parts = new String[0]; // Handle empty line after command

            try {
                switch (commandWord) {
                    case "help":
                        System.out.println("  format [prop=value ...] - diskSize=64MB, maxFiles=100, nameLen=128, etc.");
                        System.out.println("  load <path>             - Load VFS from file.");
                        System.out.println("  save [path]             - Save current VFS.");
                        System.out.println("  info                    - Show VFS properties.");
                        System.out.println("  ls                      - List files.");
                        System.out.println("  write <vfsPath> <text>  - Write text to file.");
                        System.out.println("  read <vfsPath>          - Read file content.");
                        System.out.println("  del <vfsPath>           - Delete file.");
                        System.out.println("  exists <vfsPath>        - Check if file exists.");
                        System.out.println("  hash <vfsPath> <algo>   - MD5, SHA-1, SHA-256, SHA-512.");
                        System.out.println("  import <realPath> <vfsPath> [checkHash:true|false]");
                        System.out.println("  export <vfsPath> <realPath> [checkHash:true|false]");
                        System.out.println("  imgimport <realDir> <stripPrefix> [maxDepth]");
                        System.out.println("  imgexport <realDir>");
                        System.out.println("  optimize                - Defragment disk.");
                        System.out.println("  exit                    - Exit.");
                        break;
                    case "format": /* ... (from previous, assumed mostly correct) ... */
                        currentFormatComposition = new VFS3HeaderComposition();
                        for (String part : parts) {
                            if (part.trim().isEmpty()) continue;
                            String[] kv = part.split("=", 2);
                            if (kv.length != 2) {System.err.println("Bad format prop: " + part); continue;}
                            String k = kv[0].toLowerCase(); String v = kv[1];
                            try {
                                switch (k) {
                                    case "disksize": currentFormatComposition.setDiskSize(parseSize(v)); break;
                                    case "maxfiles": currentFormatComposition.setMaxFilesCount(Integer.parseInt(v)); break;
                                    case "namelen": currentFormatComposition.setMaxFileNameLength(Integer.parseInt(v)); break;
                                    case "maxfilesize": currentFormatComposition.setMaxFileSize(parseSize(v)); break;
                                    case "fastrec": currentFormatComposition.setEnableFastRecovery(Boolean.parseBoolean(v)); break;
                                    case "ioproc": currentFormatComposition.setIoProcessorClassName(v); break;
                                    default: System.err.println("Unknown format prop: " + k);
                                }
                            } catch (NumberFormatException e) {System.err.println("Invalid num for " + k + ": " + v);}
                        }
                        vfs = new VFS3();
                        vfs.format(currentFormatComposition);
                        break;
                    case "load": if(line.isEmpty()){System.err.println("load <path>");break;} vfs=new VFS3(); vfs.loadDisk(line); break;
                    case "save": String sp=line.isEmpty()?vfs.loadedVfsPath:line; if(sp==null||sp.trim().isEmpty()){System.err.println("No path for save.");break;} vfs.saveDisk(sp); break;
                    case "info":
                        vfs.ensureFormatted(); // Throws if not formatted
                        System.out.println(vfs.getReportString());
                        break;
                    case "ls": vfs.list(); break;
                    case "write":
                        if (parts.length < 2) { System.err.println("write <vfsPath> <content>"); break; }
                        String vfsWritePath = parts[0];
                        String content = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                        vfs.write(vfsWritePath, content.getBytes(StandardCharsets.UTF_8));
                        break;
                    case "read": if(line.isEmpty()){System.err.println("read <vfsPath>");break;} System.out.println(vfs.read(line)); break;
                    case "del": if(line.isEmpty()){System.err.println("del <vfsPath>");break;} vfs.delete(line); break;
                    case "exists": if(line.isEmpty()){System.err.println("exists <vfsPath>");break;} System.out.println(vfs.exists(line)); break;
                    case "hash":
                        if (parts.length != 2) { System.err.println("hash <vfsPath> <MD5|SHA-1|SHA-256|SHA-512>"); break; }
                        System.out.println(vfs.hashFile(parts[0], parts[1].toUpperCase()));
                        break;
                    case "import":
                        if (parts.length < 2) { System.err.println("import <realPath> <vfsPath> [checkHash:true|false]"); break; }
                        boolean chkImport = (parts.length > 2) && "true".equalsIgnoreCase(parts[2]);
                        vfs.fromDisk(parts[1], new File(parts[0]), chkImport);
                        break;
                    case "export":
                        if (parts.length < 2) { System.err.println("export <vfsPath> <realPath> [checkHash:true|false]"); break; }
                        boolean chkExport = (parts.length > 2) && "true".equalsIgnoreCase(parts[2]);
                        vfs.toDisk(parts[0], new File(parts[1]), chkExport);
                        break;
                    case "imgimport":
                        if (parts.length < 2) { System.err.println("imgimport <realDir> <stripPrefix> [maxDepth:-1 for inf]"); break; }
                        int depth = (parts.length > 2) ? Integer.parseInt(parts[2]) : -1;
                        vfs.imageFromRealDisk(new File(parts[0]), parts[1], depth);
                        break;
                    case "imgexport":
                        if (line.isEmpty()) { System.err.println("imgexport <realDir>"); break; }
                        vfs.imageToRealDisk(new File(line));
                        break;
                    case "optimize": vfs.optimizeDisk(); break;
                    case "exit": System.out.println("Exiting."); scanner.close(); return;
                    default: System.out.println("Unknown command: " + commandWord);
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                // e.printStackTrace(System.err); // Uncomment for full stack trace
            }
        }
    }

    private static long parseSize(String sizeStr) {
        sizeStr = sizeStr.toUpperCase(); long mult = 1;
        if (sizeStr.endsWith("KB") || sizeStr.endsWith("K")) { mult = 1024L; sizeStr = sizeStr.replaceAll("[KBK]", ""); }
        else if (sizeStr.endsWith("MB") || sizeStr.endsWith("M")) { mult = 1024L * 1024L; sizeStr = sizeStr.replaceAll("[MBM]", ""); }
        else if (sizeStr.endsWith("GB") || sizeStr.endsWith("G")) { mult = 1024L * 1024L * 1024L; sizeStr = sizeStr.replaceAll("[GBG]", ""); }
        else if (sizeStr.endsWith("B")) { sizeStr = sizeStr.replaceAll("B", "");}
        return Long.parseLong(sizeStr.trim()) * mult;
    }
}