package org.zstack.crypto.keyprovider.nkp.api;

import org.zstack.crypto.keyprovider.api.APIUpdateKeyProviderEvent;
import org.zstack.header.keyprovider.NkpInventory;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateNkpEvent extends APIUpdateKeyProviderEvent {
    public APIUpdateNkpEvent() {
        super(null);
    }

    public APIUpdateNkpEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateNkpEvent __example__() {
        APIUpdateNkpEvent event = new APIUpdateNkpEvent();
        event.setInventory(NkpInventory.__example__());
        return event;
    }
}
