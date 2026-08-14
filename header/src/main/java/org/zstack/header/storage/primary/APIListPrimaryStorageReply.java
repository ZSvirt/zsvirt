package org.zstack.header.storage.primary;

import org.zstack.header.message.APIReply;

import java.util.List;

public class APIListPrimaryStorageReply extends APIReply {
    private List<PrimaryStorageInventory> inventories;

    public List<PrimaryStorageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PrimaryStorageInventory> inventories) {
        this.inventories = inventories;
    }


    public static APIListPrimaryStorageReply __example__() {
        APIListPrimaryStorageReply msg = new APIListPrimaryStorageReply();
        return msg;
    }
    
}