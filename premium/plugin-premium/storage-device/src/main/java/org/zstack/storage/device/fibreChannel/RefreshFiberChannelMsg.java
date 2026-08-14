package org.zstack.storage.device.fibreChannel;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class RefreshFiberChannelMsg extends NeedReplyMessage {
    private String hostUuid;
    private List<String> scsiLunUuids;

    private boolean rescan = false;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<String> getScsiLunUuids() {
        return scsiLunUuids;
    }

    public void setScsiLunUuids(List<String> scsiLunUuids) {
        this.scsiLunUuids = scsiLunUuids;
    }

    public boolean isRescan() {
        return rescan;
    }

    public void setRescan(boolean rescan) {
        this.rescan = rescan;
    }
}
