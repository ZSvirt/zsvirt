package org.zstack.sns.platform.email;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIAddEmailAddressToSNSEmailEndpointEvent extends APIEvent {
    private SNSEmailAddressInventory inventory;

    public SNSEmailAddressInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSEmailAddressInventory inventory) {
        this.inventory = inventory;
    }

    public APIAddEmailAddressToSNSEmailEndpointEvent() {}

    public APIAddEmailAddressToSNSEmailEndpointEvent(String apiId) {
        super(apiId);
    }

    public static APIAddEmailAddressToSNSEmailEndpointEvent __example__() {
        APIAddEmailAddressToSNSEmailEndpointEvent evt = new APIAddEmailAddressToSNSEmailEndpointEvent();
        evt.setInventory(SNSEmailAddressInventory.__example__());
        return evt;
    }
}
