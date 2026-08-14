package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by MaJin on 2021/3/18.
 */
public class CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply extends MessageReply {
    private String locateHostUuid;
    private ImageCacheInventory inventory;
    private boolean incremental;

    public String getLocateHostUuid() {
        return locateHostUuid;
    }

    public void setLocateHostUuid(String locateHostUuid) {
        this.locateHostUuid = locateHostUuid;
    }

    public long getActualSize() {
        return inventory == null ? 0 : inventory.getSize();
    }

    public void setInventory(ImageCacheInventory inventory) {
        this.inventory = inventory;
    }

    public ImageCacheInventory getInventory() {
        return inventory;
    }

    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    public boolean isIncremental() {
        return incremental;
    }
}
