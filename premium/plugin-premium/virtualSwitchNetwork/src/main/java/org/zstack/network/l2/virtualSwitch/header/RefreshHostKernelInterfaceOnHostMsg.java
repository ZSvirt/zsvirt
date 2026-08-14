package org.zstack.network.l2.virtualSwitch.header;

import java.util.List;

import org.zstack.header.message.NeedReplyMessage;

public class RefreshHostKernelInterfaceOnHostMsg extends NeedReplyMessage {

    private String hostUuid;
    private List<String> l2NetworkUuids;
    private boolean deleteAllInterfaces;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<String> getL2NetworkUuids() {
        return l2NetworkUuids;
    }

    public void setL2NetworkUuids(List<String> l2NetworkUuids) {
        this.l2NetworkUuids = l2NetworkUuids;
    }

    public boolean isDeleteAllInterfaces() {
        return deleteAllInterfaces;
    }

    public void setDeleteAllInterfaces(boolean deleteAllInterfaces) {
        this.deleteAllInterfaces = deleteAllInterfaces;
    }

}
