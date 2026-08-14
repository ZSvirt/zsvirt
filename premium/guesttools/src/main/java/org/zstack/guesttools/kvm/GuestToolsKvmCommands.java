package org.zstack.guesttools.kvm;

import org.zstack.kvm.KVMAgentCommands;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by GuoYi on 2019-09-18.
 */
public class GuestToolsKvmCommands {
    public static class DownloadGuestToolsIsoToHostCmd extends KVMAgentCommands.AgentCommand {
        private String hostUuid;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }
    }

    public static class DownloadGuestToolsIsoToHostRsp extends KVMAgentCommands.AgentResponse {

    }

    public static class AttachGuestToolsIsoToVmCmd extends KVMAgentCommands.AgentCommand {
        private String vmInstanceUuid;
        private boolean needTempDisk;
        private String platform;
        private int cdromDeviceId = 0;

        public String getPlatform() { return platform;}

        public void setPlatform(String platform) { this.platform = platform;}

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public boolean isNeedTempDisk() {
            return needTempDisk;
        }

        public void setNeedTempDisk(boolean needTempDisk) {
            this.needTempDisk = needTempDisk;
        }

        public int getCdromDeviceId() {
            return cdromDeviceId;
        }

        public void setCdromDeviceId(int cdromDeviceId) {
            this.cdromDeviceId = cdromDeviceId;
        }
    }

    public static class AttachGuestToolsIsoToVmRsp extends KVMAgentCommands.AgentResponse {

    }


    public static class DetachGuestToolsIsoFromVmCmd extends KVMAgentCommands.AgentCommand {
        private String vmInstanceUuid;
        private String platform;

        public String getPlatform() { return platform;}

        public void setPlatform(String platform) { this.platform = platform;}

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }
    }

    public static class DetachGuestToolsIsoFromVmRsp extends KVMAgentCommands.AgentResponse {

    }

    public static class GetVmGuestToolsInfoCmd extends KVMAgentCommands.AgentCommand {
        private String vmInstanceUuid;
        private String platform;

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public String getPlatform() {
            return platform;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }
    }

    public static class GetVmGuestToolsInfoRsp extends KVMAgentCommands.AgentResponse {
        private String version;
        private String status;
        private final Map<String, String> features = new HashMap<>();

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Map<String, String> getFeatures() {
            return features;
        }

        public void setFeatures(Map<String, String> features) {
            this.features.clear();
            this.features.putAll(features);
        }
    }

    public static class GetVmMetricsRoutingStatusCmd extends KVMAgentCommands.AgentCommand {
        private String vmInstanceUuid;
        private Set<String> items;

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public Set<String> getItems() {
            return items;
        }

        public void setItems(Set<String> items) {
            this.items = items;
        }
    }

    public static class GetVmMetricsRoutingStatusRsp extends KVMAgentCommands.AgentResponse {
        private Map<String, String> values;

        public Map<String, String> getValues() {
            return values;
        }

        public void setValues(Map<String, String> values) {
            this.values = values;
        }
    }

    public static String ZWATCH_INSTALL_FAILED ="InstallFailed";
    public static String ZWATCH_INSTALL_RESULT_PATH = "/host/zwatchInstallResult";

    public static String QGA_VM_SYNC_NETWORK_INFO_PATH = "/vm/nicinfo/sync";
    public static String QGA_VM_STATE_REPORT_PATH = "/vm/qgastate/report";

    public static class ZwatchInstallResultCmd {
        public String vmInstanceUuid;
        public String version;
    }

    public static class QGANicInfoResultCmd {
        public String vmUuid;
        public String nicInfo;
    }

    public static class QGAVmStateResultCmd {
        public String qgaState;
        public String toolsState;
    }
}
