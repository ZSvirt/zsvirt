package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

/**
 * Created by AlanJager on 2017/5/3.
 */
public class IncreaseVmMemoryReply extends MessageReply {
    private long memorySize;

    public long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(long memorySize) {
        this.memorySize = memorySize;
    }
}

