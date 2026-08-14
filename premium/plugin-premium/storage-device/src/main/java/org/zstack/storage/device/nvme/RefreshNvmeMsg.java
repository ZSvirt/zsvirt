package org.zstack.storage.device.nvme;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class RefreshNvmeMsg extends NeedReplyMessage {
    private String hostUuid;
    private List<String> nvmeLunUuids;

    private boolean rescan = false;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<String> getNvmeLunUuids() {
        return nvmeLunUuids;
    }

    public void setNvmeLunUuids(List<String> nvmeLunUuids) {
        this.nvmeLunUuids = nvmeLunUuids;
    }

    public boolean isRescan() {
        return rescan;
    }

    public void setRescan(boolean rescan) {
        this.rescan = rescan;
    }
}
