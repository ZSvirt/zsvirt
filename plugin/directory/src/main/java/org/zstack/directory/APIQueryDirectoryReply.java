package org.zstack.directory;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * @author shenjin
 * @date 2023/1/12 10:25
 */
@RestResponse(allTo = "inventories")
public class APIQueryDirectoryReply extends APIQueryReply {
    private List<DirectoryInventory> inventories;

    public List<DirectoryInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<DirectoryInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryDirectoryReply __example__() {
        APIQueryDirectoryReply reply = new APIQueryDirectoryReply();
        DirectoryInventory inventory = new DirectoryInventory();
        inventory.setName("test");
        inventory.setUuid(uuid());
        inventory.setGroupName("admin/test");
        inventory.setParentUuid(uuid());
        inventory.setRootDirectoryUuid(uuid());
        inventory.setZoneUuid(uuid());
        inventory.setType("vminstance");
        reply.setInventories(list(inventory));
        return reply;
    }
}
