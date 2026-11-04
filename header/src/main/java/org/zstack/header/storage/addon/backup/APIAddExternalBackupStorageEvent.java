package org.zstack.header.storage.addon.backup;

import org.zstack.header.log.MaskSensitiveInfo;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.AddBackupStorageReply;
import org.zstack.header.storage.backup.BackupStorageInventory;

@RestResponse(allTo = "inventory")
@MaskSensitiveInfo
public class APIAddExternalBackupStorageEvent extends APIEvent implements AddBackupStorageReply {
    public APIAddExternalBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public APIAddExternalBackupStorageEvent() {
        super(null);
    }

    @NoLogging(behavior = NoLogging.Behavior.Auto)
    private ExternalBackupStorageInventory inventory;

    public ExternalBackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ExternalBackupStorageInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAddExternalBackupStorageEvent __example__() {
        APIAddExternalBackupStorageEvent event = new APIAddExternalBackupStorageEvent();
        ExternalBackupStorageInventory ssInventory = new ExternalBackupStorageInventory();
        ssInventory.setAvailableCapacity(1099511627776L);
        ssInventory.setTotalCapacity(1099511627776L);
        ssInventory.setUrl("zbd:pool/my-vol:/etc/foo.conf");
        ssInventory.setIdentity("zbd");
        event.setInventory(ssInventory);
        return event;
    }

    @Override
    public BackupStorageInventory backupStorageInventory() {
        return getInventory();
    }
}
