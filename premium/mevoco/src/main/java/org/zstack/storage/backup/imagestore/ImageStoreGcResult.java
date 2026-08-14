package org.zstack.storage.backup.imagestore;

public class ImageStoreGcResult {
    private long freedSpaceInBytes;

    public long getFreedSpaceInBytes() {
        return freedSpaceInBytes;
    }

    public void setFreedSpaceInBytes(long freedSpaceInBytes) {
        this.freedSpaceInBytes = freedSpaceInBytes;
    }
}
