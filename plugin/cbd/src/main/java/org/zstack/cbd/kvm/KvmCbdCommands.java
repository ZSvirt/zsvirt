package org.zstack.cbd.kvm;

import org.zstack.cbd.MdsInfo;
import org.zstack.kvm.KVMAgentCommands;

import java.util.List;

/**
 * @author Xingwei Yu
 * @date 2024/4/10 15:40
 */
public class KvmCbdCommands {
    public static final String SETUP_CBD_SELF_FENCER_PATH = "/ha/cbd/setupselffencer";
    public static final String CANCEL_CBD_SELF_FENCER_PATH = "/ha/cbd/cancelselffencer";
    public static final String CBD_CHECK_VMSTATE_PATH = "/cbd/check/vmstate";

    public static class AgentRsp {
        public boolean success = true;
        public String error;
    }

    public static class KvmUpdateClientConfCmd extends AgentCmd {
        private List<MdsInfo> mdsInfos;

        public List<MdsInfo> getMdsInfos() {
            return mdsInfos;
        }

        public void setMdsInfos(List<MdsInfo> mdsInfos) {
            this.mdsInfos = mdsInfos;
        }
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
    }

    public static class AgentCmd extends KVMAgentCommands.AgentCommand {
        public String uuid;
    }
}
