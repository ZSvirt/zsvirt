package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class GetVirtualizerInfoReply extends MessageReply {
    private String hostVirtualizer;
    private String hostInstalledQemuVersion;
    private List<VmVirtualizerInfo> vmInfoList;

    public String getHostVirtualizer() {
        return hostVirtualizer;
    }

    public void setHostVirtualizer(String hostVirtualizer) {
        this.hostVirtualizer = hostVirtualizer;
    }

    public String getHostInstalledQemuVersion() {
        return hostInstalledQemuVersion;
    }

    public void setHostInstalledQemuVersion(String hostInstalledQemuVersion) {
        this.hostInstalledQemuVersion = hostInstalledQemuVersion;
    }

    public List<VmVirtualizerInfo> getVmInfoList() {
        return vmInfoList;
    }

    public void setVmInfoList(List<VmVirtualizerInfo> vmInfoList) {
        this.vmInfoList = vmInfoList;
    }

    public static class VmVirtualizerInfo {
        private String vmInstanceUuid;
        private String currentQemuVersion;

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public String getCurrentQemuVersion() {
            return currentQemuVersion;
        }

        public void setCurrentQemuVersion(String currentQemuVersion) {
            this.currentQemuVersion = currentQemuVersion;
        }
    }
}
