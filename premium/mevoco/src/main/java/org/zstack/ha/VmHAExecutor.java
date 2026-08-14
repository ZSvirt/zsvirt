package org.zstack.ha;

import java.util.Objects;

public interface VmHAExecutor {
    default VmHAExecutorParameters setHALevelForVM(String vmUuid) {
        return new VmHAExecutorParameters(vmUuid, this);
    }
    void update(VmHAExecutorParameters parameters);

    public class VmHAExecutorParameters {
        private final String vmUuid;
        private final VmHAExecutor parent;
        private VmHaLevel HaLevelTo;
        private boolean inhibitHA;
        private String inhibitHAReason;

        private VmHAExecutorParameters(String vmUuid, VmHAExecutor parent) {
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
}
