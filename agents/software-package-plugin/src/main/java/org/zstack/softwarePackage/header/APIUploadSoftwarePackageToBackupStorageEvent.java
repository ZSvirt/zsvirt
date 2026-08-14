package org.zstack.softwarePackage.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIUploadSoftwarePackageToBackupStorageEvent extends APIEvent {
    private SoftwarePackageInventory inventory;

    public SoftwarePackageInventory getInventory() {
        return inventory;
    }

    public void setInventory(SoftwarePackageInventory inventory) {
        this.inventory = inventory;
    }

    public APIUploadSoftwarePackageToBackupStorageEvent() {
    }

    public APIUploadSoftwarePackageToBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public static APIUploadSoftwarePackageToBackupStorageEvent __example__() {
        APIUploadSoftwarePackageToBackupStorageEvent event = new APIUploadSoftwarePackageToBackupStorageEvent();
        SoftwarePackageInventory inventory = new SoftwarePackageInventory();
        event.setInventory(inventory);
        return event;
    }
}
