package org.zstack.network.service.eip;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 */
@RestResponse(allTo = "inventory")
public class APIChangeEipStateEvent extends APIEvent {
    private EipInventory inventory;

    public APIChangeEipStateEvent() {
        super(null);
    }

    public APIChangeEipStateEvent(String apiId) {
        super(apiId);
    }

    public EipInventory getInventory() {
        return inventory;
    }

    public void setInventory(EipInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIChangeEipStateEvent __example__() {
        APIChangeEipStateEvent event = new APIChangeEipStateEvent();
        EipInventory eip = new EipInventory();

        eip.setName("Test-EIP");
        eip.setVipUuid(uuid());
        eip.setVmNicUuid(uuid());
        eip.setState(EipState.Enabled.toString());

        event.setInventory(eip);
        return event;
    }

}
