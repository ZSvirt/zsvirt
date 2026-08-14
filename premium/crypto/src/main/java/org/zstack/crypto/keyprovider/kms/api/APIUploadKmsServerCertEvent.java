package org.zstack.crypto.keyprovider.kms.api;

import org.zstack.header.keyprovider.KmsInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUploadKmsServerCertEvent extends APIEvent {
    private KmsInventory inventory;

    public APIUploadKmsServerCertEvent() {
        super(null);
    }

    public APIUploadKmsServerCertEvent(String apiId) {
        super(apiId);
    }

    public KmsInventory getInventory() {
        return inventory;
    }

    public void setInventory(KmsInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUploadKmsServerCertEvent __example__() {
        APIUploadKmsServerCertEvent event = new APIUploadKmsServerCertEvent();
        event.setInventory(KmsInventory.__example__());
        return event;
    }
}
