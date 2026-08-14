package org.zstack.header.storage.volume.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.backup.VolumeBackupInventory;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;


@RestResponse(allTo = "inventories")
public class APISyncVmBackupFromImageStoreBackupStorageEvent extends APIEvent {
    private List<VolumeBackupInventory> inventories;

    public APISyncVmBackupFromImageStoreBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public APISyncVmBackupFromImageStoreBackupStorageEvent() {
        super(null);
    }

    public List<VolumeBackupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeBackupInventory> inventories) {
        this.inventories = inventories;
    }

    public static APISyncVmBackupFromImageStoreBackupStorageEvent __example__() {
        APISyncVmBackupFromImageStoreBackupStorageEvent event = new APISyncVmBackupFromImageStoreBackupStorageEvent();

        VolumeBackupInventory rinv = new VolumeBackupInventory();
        String groupUuid = uuid();
        String rootVolumeUuid= uuid();
        rinv.setUuid(uuid());
        rinv.setName("Root-Volume-Backup-1");
        rinv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        rinv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        rinv.setDescription("volume backup");
        rinv.setVolumeUuid(rootVolumeUuid);
        rinv.setSize(SizeUnit.GIGABYTE.toByte(1));
        rinv.setGroupUuid(groupUuid);

        VolumeBackupInventory dinv = new VolumeBackupInventory();
        String dataVolumeUuid = uuid();
        dinv.setUuid(uuid());
        dinv.setName("Data-Volume-Backup-1");
        dinv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        dinv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        dinv.setDescription("volume backup");
        dinv.setVolumeUuid(dataVolumeUuid);
        dinv.setSize(SizeUnit.GIGABYTE.toByte(2));
        rinv.setGroupUuid(groupUuid);

        event.setInventories(Arrays.asList(rinv, dinv));

        return event;
    }
}
