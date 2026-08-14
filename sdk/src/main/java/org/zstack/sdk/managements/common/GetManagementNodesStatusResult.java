package org.zstack.sdk.managements.common;

import org.zstack.sdk.managements.common.ManagementsStatusView;

public class GetManagementNodesStatusResult {
    public ManagementsStatusView inventory;
    public void setInventory(ManagementsStatusView inventory) {
        this.inventory = inventory;
    }
    public ManagementsStatusView getInventory() {
        return this.inventory;
    }

}
