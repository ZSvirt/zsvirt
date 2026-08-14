package org.zstack.header.configuration;

import org.zstack.header.message.DeletionMessage;

/**
 */
public class DiskOfferingDeletionMsg extends DeletionMessage implements DiskOfferingMessage {
    private String diskOfferingUuid;

    public String getDiskOfferingUuid() {
        return diskOfferingUuid;
    }

    public void setDiskOfferingUuid(String diskOfferingUuid) {
        this.diskOfferingUuid = diskOfferingUuid;
    }
}
