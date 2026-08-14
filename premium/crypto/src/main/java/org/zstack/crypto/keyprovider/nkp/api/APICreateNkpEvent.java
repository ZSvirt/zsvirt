package org.zstack.crypto.keyprovider.nkp.api;

import org.zstack.crypto.keyprovider.api.APICreateKeyProviderEvent;
import org.zstack.header.keyprovider.NkpInventory;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateNkpEvent extends APICreateKeyProviderEvent {
    public APICreateNkpEvent() {
        super(null);
    }

    public APICreateNkpEvent(String apiId) {
        super(apiId);
    }

    public static APICreateNkpEvent __example__() {
        APICreateNkpEvent event = new APICreateNkpEvent();
        event.setInventory(NkpInventory.__example__());
        return event;
    }
}
