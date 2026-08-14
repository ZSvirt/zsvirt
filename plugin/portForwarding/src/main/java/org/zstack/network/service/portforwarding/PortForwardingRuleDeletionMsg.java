package org.zstack.network.service.portforwarding;

import org.zstack.header.message.DeletionMessage;

import java.util.List;

/**
 */
public class PortForwardingRuleDeletionMsg extends DeletionMessage {
    private List<String> ruleUuids;

    public List<String> getRuleUuids() {
        return ruleUuids;
    }

    public void setRuleUuids(List<String> ruleUuids) {
        this.ruleUuids = ruleUuids;
    }
}
