package org.zstack.ipsec;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.eip.EipState;

/**
 */
@RestResponse(allTo = "inventory")
public class APIChangeIPSecConnectionStateEvent extends APIEvent {
    private IPsecConnectionInventory inventory;

    public APIChangeIPSecConnectionStateEvent() {
        super(null);
    }

    public APIChangeIPSecConnectionStateEvent(String apiId) {
        super(apiId);
    }

    public IPsecConnectionInventory getInventory() {
        return inventory;
    }

    public void setInventory(IPsecConnectionInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIChangeIPSecConnectionStateEvent __example__() {
        APIChangeIPSecConnectionStateEvent event = new APIChangeIPSecConnectionStateEvent();
        IPsecConnectionInventory ipsec = new IPsecConnectionInventory();

        ipsec.setName("IPSec-1");
        ipsec.setUuid(uuid());
        ipsec.setState(IPsecState.Enabled.toString());

        event.setInventory(ipsec);
        return event;
    }

}
