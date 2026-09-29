package org.zstack.ha.entity;

import org.zstack.ha.header.HaConstants;
import org.zstack.ha.message.VmRuleAttachFencer;
import org.zstack.kvm.KVMAgentCommands.AgentCommand;
import org.zstack.kvm.KVMAgentCommands.AgentResponse;

import java.util.ArrayList;
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

    public static class SetupSelfFencerCmd extends AgentCommand {
        public static final String PATH = HaConstants.SETUP_SELF_FENCER_PATH;
        public String hostUuid;
        public long interval;
        public int maxAttempts;
        public List<String> mountPaths = new ArrayList<>();
        public List<String> uuids = new ArrayList<>();
        public List<String> urls = new ArrayList<>();
        public List<Boolean> mountedByZStack = new ArrayList<>();
        public List<String> mountOptions = new ArrayList<>();
        public int storageCheckerTimeout;
        public String strategy;
        public List<String> fencers;
    }

    public static class CancelSelfFencerCmd extends AgentCommand {
        public static final String PATH = HaConstants.CANCEL_SELF_FENCER_PATH;
        public String hostUuid;
        public List<String> mountPoints = new ArrayList<>();
        public List<String> psUuids = new ArrayList<>();
    }

    public static class ReportVmSelfFencerCmd extends AgentCommand {
        public static final String PATH = HaConstants.KVM_REPORT_VM_FENCED_EVENT;
        public List<ReportVmSelfFencerTuple> values = new ArrayList<>();
        public String hostUuid;
    }

    public static class ReportVmSelfFencerTuple {
        public String vmUuid;
        public String fencerName;
    }
}
