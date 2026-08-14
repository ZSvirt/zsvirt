package org.zstack.header.vm;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryTemplatedVmInstanceReply extends APIQueryReply {
    private List<TemplatedVmInstanceInventory> inventories;

    public List<TemplatedVmInstanceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<TemplatedVmInstanceInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryTemplatedVmInstanceReply __example__() {
        APIQueryTemplatedVmInstanceReply reply = new APIQueryTemplatedVmInstanceReply();
        TemplatedVmInstanceInventory inventory = new TemplatedVmInstanceInventory();
        inventory.setUuid(uuid());
        inventory.setName("templatedVmInstance");
        inventory.setZoneUuid(uuid());
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        reply.setInventories(list(inventory));
        return reply;
    }
}
