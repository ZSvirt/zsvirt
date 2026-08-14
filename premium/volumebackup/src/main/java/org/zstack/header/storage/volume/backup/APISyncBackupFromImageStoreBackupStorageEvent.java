package org.zstack.header.storage.volume.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.backup.VolumeBackupInventory;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APISyncBackupFromImageStoreBackupStorageEvent extends APIEvent {
    private VolumeBackupInventory inventory;

    public APISyncBackupFromImageStoreBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public APISyncBackupFromImageStoreBackupStorageEvent() {
        super(null);
    }

    public VolumeBackupInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeBackupInventory inventory) {
        this.inventory = inventory;
    }

    public static APISyncBackupFromImageStoreBackupStorageEvent  __example__() {
        APISyncBackupFromImageStoreBackupStorageEvent  event = new APISyncBackupFromImageStoreBackupStorageEvent ();
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
