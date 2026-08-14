package org.zstack.storage.primary.local;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

public class LocalStorageRecalculateCapacityMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private boolean needRecalculateRef = true;
    private String primaryStorageUuid;

    public boolean isNeedRecalculateRef() {
        return needRecalculateRef;
    }

    public void setNeedRecalculateRef(boolean needRecalculateRef) {
        this.needRecalculateRef = needRecalculateRef;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
}
