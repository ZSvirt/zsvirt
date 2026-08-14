package org.zstack.header.cluster;

import org.zstack.header.message.MessageReply;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class PowerOffHardwareReply extends MessageReply {
    private List<PowerOffHardwareResult> results = Collections.synchronizedList(new ArrayList<>());

    public List<PowerOffHardwareResult> getResults() {
        return results;
    }

    public void setResults(List<PowerOffHardwareResult> results) {
        this.results = results;
    }

    public void addResult(PowerOffHardwareResult result) {
        results.add(result);
    }
}
