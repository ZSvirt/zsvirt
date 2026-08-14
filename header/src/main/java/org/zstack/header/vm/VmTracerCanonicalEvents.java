package org.zstack.header.vm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.NeedJsonSchema;

/**
 */
public class VmTracerCanonicalEvents {
    public static final String VM_STATE_CHANGED_PATH = "/vmTracer/vmStateChanged";
    public static final String STRANGER_VM_FOUND_PATH = "/vmTracer/strangerVmFound";
    public static final String VM_OPERATE_FAIL_ON_HYPERVISOR_PATH = "/vmTracer/vmOperateFailOnHypervisor";
    public static final String VM_STATE_IN_SHUTDOWN_PATH = "/vmTracer/vmStateInShutdown";
    public static final String VM_SKIP_TRACE_PATH = "/vmTracer/skipTrace";
    public static final String VM_CONTINUE_TRACE_PATH = "/vmTrace/continueTrace";
    public static final String MIGRATE_VM_FAILED_WITH_HOST_MAINTAIN_PATH = "/vmTrace/MigrateVMFailedWithHostMaintain";

    @NeedJsonSchema
    public static class VmSkipTraceData {
        private String vmUuid;
        private String apiId;
        private String msgName;
        private String managementNodeId;

        public String getMsgName() {
            return msgName;
        }

        public void setMsgName(String msgName) {
            this.msgName = msgName;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getApiId() {
            return apiId;
        }

        public void setApiId(String apiId) {
            this.apiId = apiId;
        }

        public String getManagementNodeId() {
            return managementNodeId;
        }

        public void setManagementNodeId(String managementNodeId) {
            this.managementNodeId = managementNodeId;
        }
    }

    @NeedJsonSchema
    public static class VmContinueTraceData {
        private String vmUuid;
        private String apiId;
        private String managementNodeId;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getApiId() {
            return apiId;
        }

        public void setApiId(String apiId) {
            this.apiId = apiId;
        }

        public String getManagementNodeId() {
            return managementNodeId;
        }

        public void setManagementNodeId(String managementNodeId) {
            this.managementNodeId = managementNodeId;
        }
    }

    @NeedJsonSchema
    public static class VmStateInShutdownData {
        private String vmUuid;
        private ErrorCode reason;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public ErrorCode getReason() {
            return reason;
        }

        public void setReason(ErrorCode reason) {
            this.reason = reason;
        }
    }

    @NeedJsonSchema
    public static class OperateFailOnHypervisorData {
        private String vmUuid;
        private String hostUuid;
        private String operate;
        private String result;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getOperate() {
            return operate;
        }

        public void setOperate(String operate) {
            this.operate = operate;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }

    @NeedJsonSchema
    public static class StrangerVmFoundData {
        private String vmIdentity;
        private String hostUuid;
        private String vmState;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getVmIdentity() {
            return vmIdentity;
        }

        public void setVmIdentity(String vmIdentity) {
            this.vmIdentity = vmIdentity;
        }

        public VmInstanceState getVmState() {
            if (vmState != null) {
                return VmInstanceState.valueOf(vmState);
            }
            return null;
        }

        public void setVmState(VmInstanceState vmState) {
            if (vmState != null) {
                this.vmState = vmState.toString();
            }
        }
    }

    @NeedJsonSchema
    public static class VmStateChangedOnHostData {
        private String vmUuid;
        private String from;
        private String to;
        private String originalHostUuid;
        private String currentHostUuid;

        public String getOriginalHostUuid() {
            return originalHostUuid;
        }

        public void setOriginalHostUuid(String originalHostUuid) {
            this.originalHostUuid = originalHostUuid;
        }

        public String getCurrentHostUuid() {
            return currentHostUuid;
        }

        public void setCurrentHostUuid(String currentHostUuid) {
            this.currentHostUuid = currentHostUuid;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public VmInstanceState getFrom() {
            if (from != null) {
                return VmInstanceState.valueOf(from);
            }
            return null;
        }

        public void setFrom(VmInstanceState from) {
            if (from != null) {
                this.from = from.toString();
            }
        }

        public VmInstanceState getTo() {
            if (to != null) {
                return VmInstanceState.valueOf(to);
            }
            return null;
        }

        public void setTo(VmInstanceState to) {
            if (to != null) {
                this.to = to.toString();
            }
        }
    }

    @NeedJsonSchema
    public static class MigrateVMFailedWithHostMaintainData {
        private String vmUuid;
        private String hostUuid;
        private String reason;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
