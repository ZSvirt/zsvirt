package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CleanUpStorageTrashOnPrimaryStorageReply extends MessageReply {
    private Map<String, List<String>> result = new HashMap<>();

    public Map<String, List<String>> getResult() {
        return result;
    }

    public void setResult(Map<String, List<String>> result) {
        this.result = result;
    }
}
