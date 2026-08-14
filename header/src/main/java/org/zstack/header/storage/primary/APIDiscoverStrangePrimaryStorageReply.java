package org.zstack.header.storage.primary;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIDiscoverStrangePrimaryStorageReply extends APIReply {
    private List<PrimaryStorageInventory> inventories = new ArrayList<>();

    public List<PrimaryStorageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PrimaryStorageInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIDiscoverStrangePrimaryStorageReply __example__() {
        APIDiscoverStrangePrimaryStorageReply reply = new APIDiscoverStrangePrimaryStorageReply();

        PrimaryStorageInventory ps = new PrimaryStorageInventory();
        ps.setName("SharedBlockGroup-1");
        ps.setUrl("/dev/vg_uuid");
        ps.setType("SharedBlock");
        ps.setAttachedClusterUuids(Collections.singletonList(uuid()));
        reply.setInventories(Collections.singletonList(ps));

        return reply;
    }
}
