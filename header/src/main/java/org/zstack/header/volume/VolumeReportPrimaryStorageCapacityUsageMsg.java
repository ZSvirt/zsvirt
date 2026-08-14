package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by frank on 6/18/2015.
 */
public class VolumeReportPrimaryStorageCapacityUsageMsg extends NeedReplyMessage {
    private String primaryStorageUuid;

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
}
