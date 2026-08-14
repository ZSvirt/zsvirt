package org.zstack.storage.primary.sharedblock;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQuerySharedBlockReply extends APIQueryReply {
    private List<SharedBlockInventory> inventories;

    public List<SharedBlockInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SharedBlockInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQuerySharedBlockReply __example__() {
        APIQuerySharedBlockReply reply = new APIQuerySharedBlockReply();

        SharedBlockInventory blockInventory = new SharedBlockInventory();
        blockInventory.setUuid(uuid());
        blockInventory.setSharedBlockGroupUuid(uuid());
        blockInventory.setType(SharedBlockType.LvmLogicalVolumeBasic);
        blockInventory.setName("test shared block");
        blockInventory.setDescription("description");
        blockInventory.setState(SharedBlockState.Enabled);
        blockInventory.setStatus(SharedBlockStatus.Connected);
        blockInventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        blockInventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setInventories(asList(blockInventory));
        reply.setSuccess(true);
        return reply;
    }
}
