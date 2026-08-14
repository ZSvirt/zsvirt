package org.zstack.storage.primary.sharedblock;

import org.zstack.header.message.APIEvent;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageStatus;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventory")
public class APIAddSharedBlockToSharedBlockGroupEvent extends APIEvent {
    private SharedBlockGroupPrimaryStorageInventory inventory;

    public SharedBlockGroupPrimaryStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(SharedBlockGroupPrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }

    public APIAddSharedBlockToSharedBlockGroupEvent() {
    }

    public APIAddSharedBlockToSharedBlockGroupEvent(String apiId) {
        super(apiId);
    }

    public static APIAddSharedBlockToSharedBlockGroupEvent __example__() {
        APIAddSharedBlockToSharedBlockGroupEvent event = new APIAddSharedBlockToSharedBlockGroupEvent();

        SharedBlockGroupPrimaryStorageInventory inv = new SharedBlockGroupPrimaryStorageInventory();
        inv.setSharedBlockGroupType(SharedBlockGroupType.LvmVolumeGroupBasic);
        String psUuid = uuid();
        inv.setUuid(psUuid);
        inv.setName("shared block group primary storage");
        inv.setDescription("shared block group primary storage description");
        inv.setType(SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE);
        inv.setAvailablePhysicalCapacity(1073741824L);
        inv.setAvailableCapacity(1073741824);
        inv.setState(PrimaryStorageState.Enabled.toString());
        inv.setStatus(PrimaryStorageStatus.Connected.toString());
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        SharedBlockInventory blockInventory = new SharedBlockInventory();
        blockInventory.setUuid(uuid());
        blockInventory.setDiskUuid(uuid());
        blockInventory.setSharedBlockGroupUuid(psUuid);
        blockInventory.setType(SharedBlockType.LvmLogicalVolumeBasic);
        blockInventory.setName("test shared block");
        blockInventory.setDescription("description");
        blockInventory.setState(SharedBlockState.Enabled);
        blockInventory.setStatus(SharedBlockStatus.Connected);
        blockInventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        blockInventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        inv.setSharedBlocks(asList(blockInventory));

        event.setInventory(inv);
        return event;
    }

}
