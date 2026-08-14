package org.zstack.storage.device.localRaid;

import org.zstack.header.message.NeedReplyMessage;

/**
 * author:kaicai.hu
 * Date:2021/9/13
 */
public class LocateLocalRaidPhysicalDriveMsg extends NeedReplyMessage {
    private Boolean locate = true;
    private RaidControllerInventory controllerInventory;
    private RaidPhysicalDriveInventory physicalDriveInventory;

    public Boolean getLocate() {
        return locate;
    }

    public void setLocate(Boolean locate) {
        this.locate = locate;
    }

    public RaidControllerInventory getControllerInventory() {
        return controllerInventory;
    }

    public void setControllerInventory(RaidControllerInventory controllerInventory) {
        this.controllerInventory = controllerInventory;
    }

    public RaidPhysicalDriveInventory getPhysicalDriveInventory() {
        return physicalDriveInventory;
    }

    public void setPhysicalDriveInventory(RaidPhysicalDriveInventory physicalDriveInventory) {
        this.physicalDriveInventory = physicalDriveInventory;
    }
}
