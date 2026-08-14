package org.zstack.directory;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @author shenjin
 * @date 2022/11/29 14:34
 */
@RestResponse(allTo = "inventory")
public class APIUpdateDirectoryEvent extends APIEvent {
    private DirectoryInventory inventory;

    public DirectoryInventory getInventory() {
        return inventory;
    }

    public void setInventory(DirectoryInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateDirectoryEvent() {
    }

    public APIUpdateDirectoryEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateDirectoryEvent __example__() {
        APIUpdateDirectoryEvent event = new APIUpdateDirectoryEvent();
        DirectoryInventory inventory = new DirectoryInventory();
        inventory.setUuid(uuid());
        inventory.setName("test");
        inventory.setParentUuid(uuid());
        inventory.setRootDirectoryUuid(uuid());
        inventory.setZoneUuid(uuid());
        inventory.setType("vminstance");
        inventory.setGroupName("admin/first/second");
        event.setInventory(inventory);
        return event;
    }
}
