package org.zstack.ha.message;

import org.zstack.header.host.HostInventory;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class RetryCancelSelfFencerOnHostMsg extends NeedReplyMessage {
    private HostInventory hostInventory;
    private List<String> primaryStorageUuids;

    public HostInventory getHostInventory() {
        return hostInventory;
    }

    public void setHostInventory(HostInventory hostInventory) {
        this.hostInventory = hostInventory;
    }

    public List<String> getPrimaryStorageUuids() {
        return primaryStorageUuids;
    }

    public void setPrimaryStorageUuids(List<String> primaryStorageUuids) {
        this.primaryStorageUuids = primaryStorageUuids;
    }
}
