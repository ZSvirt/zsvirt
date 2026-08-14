package org.zstack.header.storage.primary;

import org.zstack.header.message.CancelMessage;
import org.zstack.header.volume.VolumeInventory;

import java.util.List;

/**
 * Created by MaJin on 2019/7/23.
 */
public class CancelJobOnPrimaryStorageMsg extends CancelMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
}
