package org.zstack.ha.entity;

import org.zstack.ha.header.HaConstants;
import org.zstack.ha.message.VmRuleAttachFencer;
import org.zstack.kvm.KVMAgentCommands.AgentCommand;
import org.zstack.kvm.KVMAgentCommands.AgentResponse;

import java.util.List;

public class HaCommands {
    public static class AddVmFencerRuleToHostCmd extends AgentCommand {
        public static final String PATH = HaConstants.ADD_VM_FENCER_RULE_TO_HOST;
        public String hostUuid;
        public List<VmRuleAttachFencer> allowRules;
        public List<VmRuleAttachFencer> blockRules;
    }

    public static class AddVmFencerRuleToHostRsp extends AgentResponse {
    }

    public static class RemoveVmFencerRuleFromHostCmd extends AgentCommand {
        public static final String PATH = HaConstants.REMOVE_VM_FENCER_RULE_FROM_HOST;
        public String hostUuid;
        public List<VmRuleAttachFencer> allowRules;
        public List<VmRuleAttachFencer> blockRules;
    }

    public static class RemoveVmFencerRuleFromHostRsp extends AgentResponse {
    }
}
