package org.zstack.storage.primary.local;

import org.zstack.header.storage.primary.PrimaryStorageInventory;

/**
 * Created by frank on 10/16/2015.
 */
public class LocalStorageHostCapacityStruct {
    private PrimaryStorageInventory localStorage;
    private long size;
    private long sizeBeforeOverProvisioning;
    private String hostUuid;

    public long getSizeBeforeOverProvisioning() {
        return sizeBeforeOverProvisioning;
    }

    public void setSizeBeforeOverProvisioning(long sizeBeforeOverProvisioning) {
        this.sizeBeforeOverProvisioning = sizeBeforeOverProvisioning;
    }

    public PrimaryStorageInventory getLocalStorage() {
        return localStorage;
    }

    public void setLocalStorage(PrimaryStorageInventory localStorage) {
        this.localStorage = localStorage;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
