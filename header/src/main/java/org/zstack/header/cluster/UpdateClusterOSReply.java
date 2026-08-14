package org.zstack.header.cluster;

import org.zstack.header.message.MessageReply;

import java.util.Map;

/**
 * Created by GuoYi on 3/12/18
 */
public class UpdateClusterOSReply extends MessageReply {
    private Map<String, String> results;

    public Map<String, String> getResults() {
        return results;
    }

    public void setResults(Map<String, String> results) {
        this.results = results;
    }
}