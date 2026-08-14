package org.zstack.sdk.managements.ha2;

import org.zstack.sdk.managements.ha2.ZSha2StatusView;

public class GetZSha2StatusResult {
    public ZSha2StatusView inventory;
    public void setInventory(ZSha2StatusView inventory) {
        this.inventory = inventory;
    }
    public ZSha2StatusView getInventory() {
        return this.inventory;
    }

}
