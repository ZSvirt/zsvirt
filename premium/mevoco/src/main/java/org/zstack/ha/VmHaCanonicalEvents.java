package org.zstack.ha;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.NeedJsonSchema;
import org.zstack.header.vm.VmInstanceState;

import java.util.List;

/**
 * Created by kayo on 2018/11/15.
 */
public class VmHaCanonicalEvents {
    public static final String VM_HA_HOST_SELF_FENCER_TRIGGERED_PATH = "/vmha/selffencertriggered";
    public static final String VM_HA_STARTED_PATH = "/vmha/started";

    @NeedJsonSchema
    public static class VMHaSelfFencerTriggeredData {
        private String primaryStorageUuid;
        private String hostUuid;
        private String vmUuids;
        private String reason;

        public String getPrimaryStorageUuid() {
            return primaryStorageUuid;
        }

        public void setPrimaryStorageUuid(String primaryStorageUuid) {
            this.primaryStorageUuid = primaryStorageUuid;
        }

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getVmUuids() {
            return vmUuids;
        }

        public void setVmUuids(String vmUuids) {
            this.vmUuids = vmUuids;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    @NeedJsonSchema
    public static class VMHaStartedData {
        private String hostUuid;
        private String vmUuid;
        private ErrorCode error;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public ErrorCode getError() {
            return error;
        }

        public void setError(ErrorCode error) {
            this.error = error;
        }
    }
}
