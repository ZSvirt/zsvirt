package org.zstack.header.vm;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by david on 8/3/16.
 */
public class CloneVmInstanceResults {
    private int numberOfClonedVm;
    private List<CloneVmInstanceInventory> inventories;

    public int getNumberOfClonedVm() {
        return numberOfClonedVm;
    }

    public void setNumberOfClonedVm(int numberOfClonedVm) {
        this.numberOfClonedVm = numberOfClonedVm;
    }

    public List<CloneVmInstanceInventory> getInventories() {
        return inventories;
    }

    public List<CloneVmInstanceInventory> getInventoriesWithoutError() {
        return inventories.stream().filter(it -> it.getError() == null).collect(Collectors.toList());
    }

    public void setInventories(List<CloneVmInstanceInventory> inventories) {
        this.inventories = inventories;
    }

    public void resetNumberOfCloneVm() {
        numberOfClonedVm = inventories == null ? 0 :
                (int) inventories.stream().filter(it -> it.getError() == null).count();
    }

    public synchronized void addVmInstanceInventory(final CloneVmInstanceInventory inv) {
        if (inventories == null) {
            inventories = new ArrayList<>();
        }

        inventories.add(inv);
    }
}
