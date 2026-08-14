package org.zstack.header.volume;

import org.zstack.header.message.MessageReply;

/**
 * Created by frank on 6/18/2015.
 */
public class VolumeReportPrimaryStorageCapacityUsageReply extends MessageReply {
    private long usedCapacity;

    public long getUsedCapacity() {
        return usedCapacity;
    }

    public void setUsedCapacity(long usedCapacity) {
        this.usedCapacity = usedCapacity;
    }
}
