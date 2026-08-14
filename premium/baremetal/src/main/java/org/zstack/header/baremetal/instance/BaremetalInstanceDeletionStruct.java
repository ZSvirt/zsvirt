package org.zstack.header.baremetal.instance;

/**
 * Created by GuoYi on 7/8/18.
 */
public class BaremetalInstanceDeletionStruct {
    private BaremetalInstanceInventory inventory;
    private BaremetalInstanceDeletionPolicyManager.BaremetalInstanceDeletionPolicy deletionPolicy;

    public BaremetalInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(BaremetalInstanceInventory inventory) {
        this.inventory = inventory;
    }

    public BaremetalInstanceDeletionPolicyManager.BaremetalInstanceDeletionPolicy getDeletionPolicy() {
        return deletionPolicy;
    }

    public void setDeletionPolicy(BaremetalInstanceDeletionPolicyManager.BaremetalInstanceDeletionPolicy deletionPolicy) {
        this.deletionPolicy = deletionPolicy;
    }
}
