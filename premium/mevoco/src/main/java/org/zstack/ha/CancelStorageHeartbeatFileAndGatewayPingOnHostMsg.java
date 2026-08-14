package org.zstack.ha;

import org.zstack.header.host.HostInventory;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageInventory;

/**
 * Create by weiwang at 2018/11/29
 */
public class CancelStorageHeartbeatFileAndGatewayPingOnHostMsg extends NeedReplyMessage {
    private PrimaryStorageInventory primaryStorageInventory;
    private HostInventory hostInventory;

    public PrimaryStorageInventory getPrimaryStorageInventory() {
        return primaryStorageInventory;
    }

    public void setPrimaryStorageInventory(PrimaryStorageInventory primaryStorageInventory) {
        this.primaryStorageInventory = primaryStorageInventory;
    }

    public HostInventory getHostInventory() {
        return hostInventory;
    }

    public void setHostInventory(HostInventory hostInventory) {
        this.hostInventory = hostInventory;
    }
}
