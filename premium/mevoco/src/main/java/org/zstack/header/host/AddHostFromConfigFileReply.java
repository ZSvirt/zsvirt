package org.zstack.header.host;

import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NoJsonSchema;

import java.util.ArrayList;
import java.util.List;

public class AddHostFromConfigFileReply extends MessageReply {
    @NoJsonSchema
    private List<AddHostFromFileResult> results = new ArrayList<>();

    public List<AddHostFromFileResult> getResults() {
        return results;
    }

    public void setResults(List<AddHostFromFileResult> results) {
        this.results = results;
    }
}
