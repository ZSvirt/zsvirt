package org.zstack.crypto.keyprovider.kms.api;

import org.zstack.header.keyprovider.KmsIdentityInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUploadKmsClientIdentityEvent extends APIEvent {
    private KmsIdentityInventory inventory;

    public APIUploadKmsClientIdentityEvent() {
        super(null);
    }

    public APIUploadKmsClientIdentityEvent(String apiId) {
        super(apiId);
    }

    public KmsIdentityInventory getInventory() {
        return inventory;
    }

    public void setInventory(KmsIdentityInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUploadKmsClientIdentityEvent __example__() {
        APIUploadKmsClientIdentityEvent event = new APIUploadKmsClientIdentityEvent();
        event.setInventory(KmsIdentityInventory.__example__());
        return event;
    }
}
