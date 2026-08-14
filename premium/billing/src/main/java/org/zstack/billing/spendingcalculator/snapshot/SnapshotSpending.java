package org.zstack.billing.spendingcalculator.snapshot;

import org.zstack.billing.SpendingDetails;

import java.util.List;

/**
 * Created by camile on 2017/5/19.
 */
public class SnapshotSpending extends SpendingDetails {
    public List<SnapShotSpendingInventory> sizeInventory;

    public List<SnapShotSpendingInventory> getSizeInventory() {
        return sizeInventory;
    }

    public void setSizeInventory(List<SnapShotSpendingInventory> sizeInventory) {
        this.sizeInventory = sizeInventory;
    }
}
