package org.zstack.storage.primary.block.message;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.storage.primary.block.BlockPrimaryStorageConstants;
import org.zstack.storage.primary.block.BlockPrimaryStorageInventory;

import java.sql.Timestamp;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2023/9/3 23:01
 */
@RestResponse(allTo = "inventory")
public class APIUpdateBlockPrimaryStorageEvent extends APIEvent{
    private BlockPrimaryStorageInventory inventory;

    public BlockPrimaryStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(BlockPrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateBlockPrimaryStorageEvent() {
    }

    public APIUpdateBlockPrimaryStorageEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateBlockPrimaryStorageEvent __example__() {
        APIUpdateBlockPrimaryStorageEvent event = new APIUpdateBlockPrimaryStorageEvent();

        BlockPrimaryStorageInventory inv = new BlockPrimaryStorageInventory();
        inv.setVendorName("XStor");
        inv.setMetadata("{'ip':'127.0.0.1', 'port':8443, 'user':'test', 'password':'password'}");
        String psUuid = uuid();
        inv.setUuid(psUuid);
        inv.setName("block primary storage");
        inv.setDescription("block primary storage description");
        inv.setType(BlockPrimaryStorageConstants.BLOCK_PRIMARY_STORAGE_TYPE);
        inv.setAvailablePhysicalCapacity(1073741824L);
        inv.setAvailableCapacity(1073741824);
        inv.setState(PrimaryStorageState.Enabled.toString());
        inv.setStatus(PrimaryStorageStatus.Connected.toString());
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        event.setInventory(inv);
        return event;
    }
}
