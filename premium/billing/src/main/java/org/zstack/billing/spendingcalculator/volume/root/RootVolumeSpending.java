package org.zstack.billing.spendingcalculator.volume.root;

import org.zstack.billing.SpendingDetails;

import java.util.List;

/**
 * Created by xing5 on 2016/6/11.
 */
public class RootVolumeSpending extends SpendingDetails {
    public List<RootVolumeSpendingInventory> sizeInventory;

    public List<RootVolumeSpendingInventory> getSizeInventory() {
        return sizeInventory;
    }

    public void setSizeInventory(List<RootVolumeSpendingInventory> sizeInventory) {
        this.sizeInventory = sizeInventory;
    }
}
