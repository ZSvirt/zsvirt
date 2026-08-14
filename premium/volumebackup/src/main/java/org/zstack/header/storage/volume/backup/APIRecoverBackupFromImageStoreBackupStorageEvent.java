package org.zstack.header.storage.volume.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.backup.VolumeBackupInventory;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APIRecoverBackupFromImageStoreBackupStorageEvent extends APIEvent {
    private VolumeBackupInventory inventory;

    public APIRecoverBackupFromImageStoreBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public APIRecoverBackupFromImageStoreBackupStorageEvent() {
        super(null);
    }

    public VolumeBackupInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeBackupInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateVolumeBackupEvent __example__() {
        APICreateVolumeBackupEvent event = new APICreateVolumeBackupEvent();
        String volumeUuid= uuid();
        VolumeBackupInventory inv = new VolumeBackupInventory();
        inv.setUuid(uuid());
        inv.setName("Backup-1");
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setDescription("volume backup");
        inv.setVolumeUuid(volumeUuid);
        inv.setSize(SizeUnit.GIGABYTE.toByte(1));

        event.setInventory(inv);

        return event;
    }
}
