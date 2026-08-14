package org.zstack.kvm;

import org.zstack.header.core.Completion;
import org.zstack.header.storage.primary.PrimaryStorageInventory;

import java.util.List;

/**
 * Created by xing5 on 2016/5/10.
 */
public interface KvmSetupSelfFencerExtensionPoint {
    class KvmCancelSelfFencerParam {
        private String hostUuid;
        private PrimaryStorageInventory primaryStorage;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public PrimaryStorageInventory getPrimaryStorage() {
            return primaryStorage;
        }

        public void setPrimaryStorage(PrimaryStorageInventory primaryStorage) {
            this.primaryStorage = primaryStorage;
        }
    }

    class KvmSetupSelfFencerParam {
        private String hostUuid;
        private long interval;
        private int maxAttempts;
        private int storageCheckerTimeout;
        private PrimaryStorageInventory primaryStorage;
        private String strategy;
        private List<String> fencers;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public long getInterval() {
            return interval;
        }

        public void setInterval(long interval) {
            this.interval = interval;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public int getStorageCheckerTimeout() {
            return storageCheckerTimeout;
        }

        public void setStorageCheckerTimeout(int storageCheckerTimeout) {
            this.storageCheckerTimeout = storageCheckerTimeout;
        }

        public PrimaryStorageInventory getPrimaryStorage() {
            return primaryStorage;
        }

        public void setPrimaryStorage(PrimaryStorageInventory primaryStorage) {
            this.primaryStorage = primaryStorage;
        }

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }

        public List<String> getFencers() {
            return fencers;
        }

        public void setFencers(List<String> fencers) {
            this.fencers = fencers;
        }
    }

    String kvmSetupSelfFencerStorageType();

    default boolean storageConsistencySupported() {
        return false;
    }

    void kvmSetupSelfFencer(KvmSetupSelfFencerParam param, Completion completion);

    void kvmCancelSelfFencer(KvmCancelSelfFencerParam param, Completion completion);
}
