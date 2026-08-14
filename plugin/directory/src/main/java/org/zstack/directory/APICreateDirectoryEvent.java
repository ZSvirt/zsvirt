package org.zstack.directory;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @author shenjin
 * @date 2022/11/29 14:32
 */
@RestResponse(allTo = "inventory")
public class APICreateDirectoryEvent extends APIEvent {
    private DirectoryInventory inventory;

    public static APICreateDirectoryEvent __example__() {
        APICreateDirectoryEvent ret = new APICreateDirectoryEvent();
        DirectoryInventory inventory = new DirectoryInventory();
        inventory.setUuid(uuid());
        inventory.setName("test");
        inventory.setGroupName("admin/first/second");
        inventory.setParentUuid(uuid());
        inventory.setRootDirectoryUuid(uuid());
        inventory.setZoneUuid(uuid());
        inventory.setType("vminstance");
        ret.setInventory(inventory);
        return ret;
    }

    public APICreateDirectoryEvent() {
    }

    public APICreateDirectoryEvent(String apiId) {
        super(apiId);
    }

    public DirectoryInventory getInventory() {
        return inventory;
    }

    public void setInventory(DirectoryInventory inventory) {
        this.inventory = inventory;
    }
}
