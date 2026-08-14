package org.zstack.ovf.datatype;

import javax.annotation.Nullable;

/**
 * Disk is the name defined in ovf, and can refer to the image concept in ZStack.
 *
 * Created by Qi Le on 2022/3/3
 */
public class OvfDiskInfo {
    private int index;
    private String diskId;
    private String fileRef;
    private String fileName;
    private String format;
    private Long populatedSize;
    private Long capacity;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getDiskId() {
        return diskId;
    }

    public void setDiskId(String diskId) {
        this.diskId = diskId;
    }

    public String getFileRef() {
        return fileRef;
    }

    public void setFileRef(String fileRef) {
        this.fileRef = fileRef;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Nullable
    public Long getPopulatedSize() {
        return populatedSize;
    }

    public void setPopulatedSize(Long populatedSize) {
        this.populatedSize = populatedSize;
    }

    public Long getCapacity() {
        return capacity;
    }

    public void setCapacity(Long capacity) {
        this.capacity = capacity;
    }
}
