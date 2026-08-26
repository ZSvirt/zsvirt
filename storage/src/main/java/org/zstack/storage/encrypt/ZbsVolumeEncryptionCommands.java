package org.zstack.storage.encrypt;

import org.zstack.header.log.NoLogging;
import org.zstack.kvm.KVMAgentCommands;

import java.io.Serializable;

final class ZbsVolumeEncryptionCommands {
    private ZbsVolumeEncryptionCommands() {
    }

    static class KVMHostLuksCloneCmd extends KVMAgentCommands.AgentCommand implements Serializable {
        public String psUuid;
        @NoLogging
        public String encryptedDek;
        public String srcPath;
        public String dstPath;
        public Long virtualSizeForLuksClone;
    }

    static class KVMHostLuksCreateEmptyCmd extends KVMAgentCommands.AgentCommand implements Serializable {
        public String psUuid;
        @NoLogging
        public String encryptedDek;
        public String installPath;
        public long size;
    }

    static class KVMHostEncryptInPlaceCmd extends KVMAgentCommands.AgentCommand implements Serializable {
        public String psUuid;
        @NoLogging
        public String encryptedDek;
        public String installPath;
        public String targetInstallPath;
    }

    static class KVMHostLuksResizeCmd extends KVMAgentCommands.AgentCommand implements Serializable {
        public String psUuid;
        @NoLogging
        public String encryptedDek;
        public String installPath;
        public Long virtualSize;
    }

    static class KVMHostLuksConvertCmd extends KVMAgentCommands.AgentCommand implements Serializable {
        public String psUuid;
        @NoLogging
        public String encryptedDek;
        public String installPath;
        public String targetInstallPath;
        public boolean targetEncrypted;
        public long virtualSize;
    }

    static class KVMHostLuksRsp extends KVMAgentCommands.AgentResponse {
        public Long actualSize;
        public String installPath;
    }
}
