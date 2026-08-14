package org.zstack.sdk;

import org.zstack.sdk.RaidPhysicalDriveInventory;

public class LocateLocalRaidPhysicalDriveResult {
    public RaidPhysicalDriveInventory inventory;
    public void setInventory(RaidPhysicalDriveInventory inventory) {
        this.inventory = inventory;
    }
    public RaidPhysicalDriveInventory getInventory() {
        return this.inventory;
    }

}
