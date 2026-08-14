package org.zstack.header.storage.primary;

import org.zstack.header.log.MaskSensitiveInfo;
import org.zstack.header.log.NoLogging;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
@MaskSensitiveInfo
public class APIQueryPrimaryStorageReply extends APIQueryReply {
    @NoLogging(behavior = NoLogging.Behavior.Auto)
    private List<PrimaryStorageInventory> inventories;

    public List<PrimaryStorageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PrimaryStorageInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryPrimaryStorageReply __example__() {
        APIQueryPrimaryStorageReply reply = new APIQueryPrimaryStorageReply();

        PrimaryStorageInventory ps = new PrimaryStorageInventory();
        ps.setName("PS1");
        ps.setUrl("/zstack_ps");
        ps.setType("LocalStorage");
        ps.setAttachedClusterUuids(Collections.singletonList(uuid()));
        ps.setState(PrimaryStorageState.Enabled.toString());
        ps.setStatus(PrimaryStorageStatus.Connected.toString());

        reply.setInventories(Collections.singletonList(ps));
        return reply;
    }

}
