package org.zstack.header.baremetal.chassis;

import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NoJsonSchema;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by GuoYi on 2018-10-08.
 */
public class BatchCreateBaremetalChassisReply extends MessageReply {
    @NoJsonSchema
    private List<CreateBaremetalChassisResult> results = new ArrayList<>();

    public List<CreateBaremetalChassisResult> getResults() {
        return results;
    }

    public void setResults(List<CreateBaremetalChassisResult> results) {
        this.results = results;
    }
}
