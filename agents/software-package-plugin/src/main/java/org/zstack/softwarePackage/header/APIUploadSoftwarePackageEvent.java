package org.zstack.softwarePackage.header;


import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"inventory"})
public class APIUploadSoftwarePackageEvent extends APIEvent {
    private SoftwarePackageInventory inventory;

    public SoftwarePackageInventory getInventory() {
        return inventory;
    }

    public void setInventory(SoftwarePackageInventory inventory) {
        this.inventory = inventory;
    }

    public APIUploadSoftwarePackageEvent() {
    }

    public APIUploadSoftwarePackageEvent(String apiId) {
        super(apiId);
    }

    public static APIUploadSoftwarePackageEvent __example__() {
        APIUploadSoftwarePackageEvent event = new APIUploadSoftwarePackageEvent();
        SoftwarePackageInventory inventory = new SoftwarePackageInventory();
        event.setInventory(inventory);
        return event;
    }
}
