package org.zstack.ha.spi;

import org.zstack.ha.header.VmHaLevel;

import java.util.Objects;

public interface VmHaVOUpdater {
    default VmHAExecutorParameters setHALevelForVM(String vmUuid) {
        return new VmHAExecutorParameters(vmUuid, this);
    }

    void update(VmHAExecutorParameters parameters);

    default VmHAFenceParameters fenceForVM(String vmUuid) {
        return new VmHAFenceParameters(vmUuid, this);
    }

    void updateFence(VmHAFenceParameters parameters);

    String findVmFencedReason(String vmUuid);

    class VmHAExecutorParameters {
        private final String vmUuid;
        private final VmHaVOUpdater parent;
        private VmHaLevel HaLevelTo;
        private boolean inhibitHA;
        private String inhibitHAReason;

        private VmHAExecutorParameters(String vmUuid, VmHaVOUpdater parent) {
            this.vmUuid = Objects.requireNonNull(vmUuid);
            this.parent = Objects.requireNonNull(parent);
        }

        public VmHAExecutorParameters toNeverStop() {
            return to(VmHaLevel.NeverStop);
        }

        public VmHAExecutorParameters toOnHostFailure() {
            return to(VmHaLevel.OnHostFailure);
        }

        public VmHAExecutorParameters toDisabled() {
            return to(VmHaLevel.None);
        }

        public VmHAExecutorParameters to(String level) {
            return to(VmHaLevel.valueOf(level));
        }

        public VmHAExecutorParameters to(VmHaLevel level) {
            HaLevelTo = level;
            return this;
        }

        public VmHAExecutorParameters inhibitHATemporarily(String reason) {
            inhibitHA = true;
            inhibitHAReason = reason;
            return this;
        }

        public VmHAExecutorParameters clearInhibitHABlocking(String reason) {
            inhibitHA = false;
            inhibitHAReason = reason;
            return this;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public VmHaLevel getHaLevelTo() {
            return HaLevelTo;
        }

        public boolean isInhibitHA() {
            return inhibitHA;
        }

        public String getInhibitHAReason() {
            return inhibitHAReason;
        }

        public void update() {
            parent.update(this);
        }
    }

    class VmHAFenceParameters {
        private final String vmUuid;
        private final VmHaVOUpdater parent;
        private boolean clear;
        private String fencerName;
        private String hostUuid;

        private VmHAFenceParameters(String vmUuid, VmHaVOUpdater parent) {
            this.vmUuid = Objects.requireNonNull(vmUuid);
            this.parent = Objects.requireNonNull(parent);
        }

        public VmHAFenceParameters record(String fencerName, String hostUuid) {
            this.clear = false;
            this.fencerName = Objects.requireNonNull(fencerName);
            this.hostUuid = hostUuid;
            return this;
        }

        public VmHAFenceParameters clear() {
            this.clear = true;
            this.fencerName = null;
            this.hostUuid = null;
            return this;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public boolean isClear() {
            return clear;
        }

        public String getFencerName() {
            return fencerName;
        }

        public String getHostUuid() {
            return hostUuid;
        }

        public void update() {
            parent.updateFence(this);
        }
    }
}
