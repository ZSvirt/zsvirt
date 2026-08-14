package org.zstack.billing.spendingcalculator.pcidevice;

import org.zstack.billing.SpendingDetails;

import java.util.List;

/**
 * Created by shixin.ruan on 2018/05/05.
 */
public class PciDeviceSpending extends SpendingDetails {
    public List<PciDeviceSpendingInventory> sizeInventory;

    public List<PciDeviceSpendingInventory> getSizeInventory() {
        return sizeInventory;
    }

    public void setSizeInventory(List<PciDeviceSpendingInventory> sizeInventory) {
        this.sizeInventory = sizeInventory;
    }
}
