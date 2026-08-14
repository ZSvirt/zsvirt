package org.zstack.crypto.keyprovider.kms.api;

import org.zstack.header.keyprovider.KmsIdentityInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUploadKmsClientSignedCertEvent extends APIEvent {
    private KmsIdentityInventory inventory;

    public APIUploadKmsClientSignedCertEvent() {
        super(null);
    }

    public APIUploadKmsClientSignedCertEvent(String apiId) {
        super(apiId);
    }

    public KmsIdentityInventory getInventory() {
        return inventory;
    }

    public void setInventory(KmsIdentityInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUploadKmsClientSignedCertEvent __example__() {
        APIUploadKmsClientSignedCertEvent event = new APIUploadKmsClientSignedCertEvent();
        event.setInventory(KmsIdentityInventory.__example__());
        return event;
    }
}
