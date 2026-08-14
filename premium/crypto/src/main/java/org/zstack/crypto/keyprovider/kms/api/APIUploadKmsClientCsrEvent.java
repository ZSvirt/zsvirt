package org.zstack.crypto.keyprovider.kms.api;

import org.zstack.header.keyprovider.KmsIdentityInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUploadKmsClientCsrEvent extends APIEvent {
    private KmsIdentityInventory inventory;

    public APIUploadKmsClientCsrEvent() {
        super(null);
    }

    public APIUploadKmsClientCsrEvent(String apiId) {
        super(apiId);
    }

    public KmsIdentityInventory getInventory() {
        return inventory;
    }

    public void setInventory(KmsIdentityInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUploadKmsClientCsrEvent __example__() {
        APIUploadKmsClientCsrEvent event = new APIUploadKmsClientCsrEvent();
        event.setInventory(KmsIdentityInventory.__example__());
        return event;
    }
}
