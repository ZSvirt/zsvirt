package org.zstack.crypto.keyprovider.kms.api;

import org.zstack.crypto.keyprovider.api.APIUpdateKeyProviderEvent;
import org.zstack.header.keyprovider.KmsInventory;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateKmsEvent extends APIUpdateKeyProviderEvent {
    public APIUpdateKmsEvent() {
        super(null);
    }

    public APIUpdateKmsEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateKmsEvent __example__() {
        APIUpdateKmsEvent event = new APIUpdateKmsEvent();
        event.setInventory(KmsInventory.__example__());
        return event;
    }
}
