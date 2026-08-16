package org.zstack.header.storage.primary;

import java.util.ArrayList;
import java.util.List;

public class PrimaryStorageDiscoveryResult {
    private List<PrimaryStorageInventory> inventories = new ArrayList<>();
    private List<String> resetVgUuidRequiredUuids = new ArrayList<>();

    public List<PrimaryStorageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PrimaryStorageInventory> inventories) {
        this.inventories = inventories;
    }

    public List<String> getResetVgUuidRequiredUuids() {
        return resetVgUuidRequiredUuids;
    }

    public void setResetVgUuidRequiredUuids(List<String> resetVgUuidRequiredUuids) {
        this.resetVgUuidRequiredUuids = resetVgUuidRequiredUuids;
    }
}
