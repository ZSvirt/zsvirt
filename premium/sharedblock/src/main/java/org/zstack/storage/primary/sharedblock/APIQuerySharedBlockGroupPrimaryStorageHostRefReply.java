package org.zstack.storage.primary.sharedblock;

import org.zstack.header.message.DocUtils;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.primary.PrimaryStorageHostStatus;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQuerySharedBlockGroupPrimaryStorageHostRefReply extends APIQueryReply {
    private List<SharedBlockGroupPrimaryStorageHostRefInventory> inventories;

    public List<SharedBlockGroupPrimaryStorageHostRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SharedBlockGroupPrimaryStorageHostRefInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQuerySharedBlockGroupPrimaryStorageHostRefReply __example__() {
        APIQuerySharedBlockGroupPrimaryStorageHostRefReply reply = new APIQuerySharedBlockGroupPrimaryStorageHostRefReply();

        SharedBlockGroupPrimaryStorageHostRefInventory inv = new SharedBlockGroupPrimaryStorageHostRefInventory();
        inv.setHostUuid(DocUtils.uuidForAPIDoc());
        inv.setPrimaryStorageUuid(DocUtils.uuidForAPIDoc());
        inv.setHostId(100);
        inv.setStatus(PrimaryStorageHostStatus.Connected);
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setInventories(asList(inv));
        reply.setSuccess(true);
        return reply;
    }
}
