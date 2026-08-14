package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

import java.util.Map;

/**
 * Created by frank on 11/8/2015.
 */
public class CheckVmStateOnHypervisorReply extends MessageReply {
    private Map<String, String> states;

    public Map<String, String> getStates() {
        return states;
    }

    public void setStates(Map<String, String> states) {
        this.states = states;
    }
}
