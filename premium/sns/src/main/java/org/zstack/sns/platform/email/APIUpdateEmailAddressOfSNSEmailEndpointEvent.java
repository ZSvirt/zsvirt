package org.zstack.sns.platform.email;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateEmailAddressOfSNSEmailEndpointEvent extends APIEvent {
    private SNSEmailAddressInventory inventory;

    public SNSEmailAddressInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSEmailAddressInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateEmailAddressOfSNSEmailEndpointEvent() {}

    public APIUpdateEmailAddressOfSNSEmailEndpointEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateEmailAddressOfSNSEmailEndpointEvent __example__() {
        APIUpdateEmailAddressOfSNSEmailEndpointEvent evt = new APIUpdateEmailAddressOfSNSEmailEndpointEvent();
        evt.setInventory(SNSEmailAddressInventory.__example__());
        return evt;
    }
}
