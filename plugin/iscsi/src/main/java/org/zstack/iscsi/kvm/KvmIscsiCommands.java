package org.zstack.iscsi.kvm;

import org.zstack.kvm.KVMAgentCommands;

import java.util.List;

public class KvmIscsiCommands {
    public static final String ISCSI_SELF_FENCER_PATH = "/ha/iscsi/setupselffencer";
    public static final String CANCEL_ISCSI_SELF_FENCER_PATH = "/ha/iscsi/cancelselffencer";
    public static final String ISCSI_CHECK_VMSTATE_PATH = "/iscsi/check/vmstate";

    public static class AgentCmd extends KVMAgentCommands.AgentCommand {
        public String uuid;
    }

    public static class AgentRsp {
        public boolean success = true;
        public String error;
    }

    public static class KvmSetupSelfFencerCmd extends AgentCmd {
        public long interval;
        public int maxAttempts;
        public List<String> coveringPaths;
        public String heartbeatUrl;
        public int storageCheckerTimeout;
        public String hostUuid;
        public Integer hostId;
        public Long heartbeatRequiredSpace;
        public String strategy;
        public List<String> fencers;
    }

    public static class KvmCancelSelfFencerCmd extends AgentCmd {
        public String installPath;
        public String hostUuid;
        public Integer hostId;
    }
}
