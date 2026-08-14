package org.zstack.storage.device.localRaid;

import org.zstack.header.message.MessageReply;

import java.util.Map;

public class SelfTestLocalRaidForRecordReply extends MessageReply {
    private Map<String, String> results;

    public Map<String, String> getResults() {
        return results;
    }

    public void setResults(Map<String, String> results) {
        this.results = results;
    }
}
