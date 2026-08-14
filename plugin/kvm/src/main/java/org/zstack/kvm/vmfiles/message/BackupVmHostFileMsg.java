package org.zstack.kvm.vmfiles.message;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class BackupVmHostFileMsg extends NeedReplyMessage {
    private String vmUuid;
    private String hostUuid;
    private List<String> toResourceUuidList;

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

    public List<String> getToResourceUuidList() {
        return toResourceUuidList;
    }

    public void setToResourceUuidList(List<String> toResourceUuidList) {
        this.toResourceUuidList = toResourceUuidList;
    }
}
