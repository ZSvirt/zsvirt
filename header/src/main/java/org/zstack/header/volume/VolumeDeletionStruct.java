package org.zstack.header.volume;

/**
 * Created by frank on 4/20/2015.
 */
public class VolumeDeletionStruct {
    private boolean detachBeforeDeleting;
    private String deletionPolicy;
    private VolumeInventory inventory;

    public VolumeDeletionStruct() {
    }

    public String getDeletionPolicy() {
        return deletionPolicy;
    }

    public void setDeletionPolicy(String deletionPolicy) {
        this.deletionPolicy = deletionPolicy;
    }

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }

    public boolean isDetachBeforeDeleting() {
        return detachBeforeDeleting;
    }

    public void setDetachBeforeDeleting(boolean detachBeforeDeleting) {
        this.detachBeforeDeleting = detachBeforeDeleting;
    }
}
