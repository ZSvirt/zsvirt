package org.zstack.sdk;

import org.zstack.sdk.QuotaInventory;

public class UpdateQuotaResult {
    public QuotaInventory inventory;
    public void setInventory(QuotaInventory inventory) {
        this.inventory = inventory;
    }
    public QuotaInventory getInventory() {
        return this.inventory;
    }

}
