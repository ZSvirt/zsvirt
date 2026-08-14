package org.zstack.softwarePackage.header;


import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIUploadAndExecuteSoftwareUpgradePackageEvent extends APIEvent {
    private SoftwarePackageInventory inventory;

    public SoftwarePackageInventory getInventory() {
        return inventory;
    }

    public void setInventory(SoftwarePackageInventory inventory) {
        this.inventory = inventory;
    }

    public APIUploadAndExecuteSoftwareUpgradePackageEvent() {
    }

    public APIUploadAndExecuteSoftwareUpgradePackageEvent(String apiId) {
        super(apiId);
    }

    public static APIUploadAndExecuteSoftwareUpgradePackageEvent __example__() {
        return new APIUploadAndExecuteSoftwareUpgradePackageEvent();
    }
}
