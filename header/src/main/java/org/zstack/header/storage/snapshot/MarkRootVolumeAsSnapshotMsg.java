package org.zstack.header.storage.snapshot;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.volume.VolumeInventory;

/**
 * Created by liangbo.zhou on 17-6-23.
 */
public class MarkRootVolumeAsSnapshotMsg extends NeedReplyMessage{

    private VolumeInventory volume;
    private String accountUuid;

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
}
