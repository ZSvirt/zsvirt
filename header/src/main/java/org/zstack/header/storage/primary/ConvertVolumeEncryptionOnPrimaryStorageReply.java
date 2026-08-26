package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

import java.util.HashMap;
import java.util.Map;

public class ConvertVolumeEncryptionOnPrimaryStorageReply extends MessageReply {
    private Map<String, Long> actualSizes = new HashMap<>();

    public Map<String, Long> getActualSizes() {
        return actualSizes;
    }

    public void setActualSizes(Map<String, Long> actualSizes) {
        this.actualSizes = actualSizes;
    }
}
