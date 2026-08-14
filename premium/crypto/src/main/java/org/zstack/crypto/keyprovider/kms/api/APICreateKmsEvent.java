package org.zstack.crypto.keyprovider.kms.api;

import org.zstack.crypto.keyprovider.api.APICreateKeyProviderEvent;
import org.zstack.header.keyprovider.KmsInventory;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateKmsEvent extends APICreateKeyProviderEvent {
    public APICreateKmsEvent() {
        super(null);
    }

    public APICreateKmsEvent(String apiId) {
        super(apiId);
    }

    public static APICreateKmsEvent __example__() {
        APICreateKmsEvent event = new APICreateKmsEvent();
        event.setInventory(KmsInventory.__example__());
        return event;
    }
}
