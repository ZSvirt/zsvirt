package org.zstack.sdk;

import org.zstack.sdk.NasMountTargetInventory;

public class UpdateNasMountTargetResult {
    public NasMountTargetInventory inventory;
    public void setInventory(NasMountTargetInventory inventory) {
        this.inventory = inventory;
    }
    public NasMountTargetInventory getInventory() {
        return this.inventory;
    }

}
