package org.zstack.billing.spendingcalculator.vm;

import org.zstack.billing.SpendingDetails;

import java.util.List;

/**
 * Created by xing5 on 2016/6/8.
 */
public class VmSpending extends SpendingDetails {
    public List<VmSpendingDetails> cpuInventory;
    public List<VmSpendingDetails> memoryInventory;
    public List<VmSpendingDetails> rootVolumeInventory;

    public List<VmSpendingDetails> getCpuInventory() {
        return cpuInventory;
    }

    public void setCpuInventory(List<VmSpendingDetails> cpuInventory) {
        this.cpuInventory = cpuInventory;
    }

    public List<VmSpendingDetails> getMemoryInventory() {
        return memoryInventory;
    }

    public void setMemoryInventory(List<VmSpendingDetails> memoryInventory) {
        this.memoryInventory = memoryInventory;
    }

    public List<VmSpendingDetails> getRootVolumeInventory() {
        return rootVolumeInventory;
    }

    public void setRootVolumeInventory(List<VmSpendingDetails> rootVolumeInventory) {
        this.rootVolumeInventory = rootVolumeInventory;
    }
}
