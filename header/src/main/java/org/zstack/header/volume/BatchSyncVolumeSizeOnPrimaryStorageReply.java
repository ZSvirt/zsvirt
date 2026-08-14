package org.zstack.header.volume;

import org.zstack.header.message.MessageReply;

import java.util.HashMap;
import java.util.Map;

public class BatchSyncVolumeSizeOnPrimaryStorageReply extends MessageReply {
    private Map<String, Long> actualSizes = new HashMap<>();

    public void setActualSizes(Map<String, Long> actualSizes) {
        this.actualSizes = actualSizes;
    }

    public Map<String, Long> getActualSizes() {
        return actualSizes;
    }
}
