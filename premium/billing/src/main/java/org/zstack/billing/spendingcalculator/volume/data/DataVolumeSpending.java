package org.zstack.billing.spendingcalculator.volume.data;

import org.zstack.billing.SpendingDetails;

import java.util.List;

/**
 * Created by xing5 on 2016/6/11.
 */
public class DataVolumeSpending extends SpendingDetails {
    public List<DataVolumeSpendingInventory> sizeInventory;

    public List<DataVolumeSpendingInventory> getSizeInventory() {
        return sizeInventory;
    }

    public void setSizeInventory(List<DataVolumeSpendingInventory> sizeInventory) {
        this.sizeInventory = sizeInventory;
    }
}
