package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.DocUtils;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.backup.BackupStorageState;
import org.zstack.header.storage.backup.BackupStorageStatus;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryImageStoreBackupStorageReply extends APIQueryReply {
    private List<ImageStoreBackupStorageInventory> inventories;

    public List<ImageStoreBackupStorageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ImageStoreBackupStorageInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryImageStoreBackupStorageReply __example__() {
        APIQueryImageStoreBackupStorageReply reply = new APIQueryImageStoreBackupStorageReply();
        ImageStoreBackupStorageInventory inventory = new ImageStoreBackupStorageInventory();
        reply.setInventories(Collections.singletonList(inventory));
        inventory.setCreateDate(new Timestamp(DocUtils.date));
        inventory.setLastOpDate(new Timestamp(DocUtils.date));
        inventory.setAttachedZoneUuids(Collections.singletonList(uuid()));
        inventory.setDescription("My ImageStore Backup Storage.");
        inventory.setHostname("127.0.0.1");
        inventory.setName("ImageStoreBS");
        inventory.setSshPort(22);
        inventory.setState(BackupStorageState.Enabled.toString());
        inventory.setStatus(BackupStorageStatus.Connected.toString());
        inventory.setAvailableCapacity(SizeUnit.GIGABYTE.toByte(512));
        inventory.setTotalCapacity(SizeUnit.GIGABYTE.toByte(1024));
        inventory.setType(ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE);
        inventory.setUrl("/zstack_bs");
        inventory.setUsername("zstack");
        inventory.setUuid(uuid());
        return reply;
    }

}
