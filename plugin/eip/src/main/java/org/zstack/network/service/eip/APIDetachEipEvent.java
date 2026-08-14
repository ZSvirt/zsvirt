package org.zstack.network.service.eip;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 *@apiResult
 * api event for message :ref`APIDetachEipMsg`
 *
 *@category eip
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.service.eip.APIDetachEipEvent": {
"inventory": {
"uuid": "69198105fd7a40778fba1759b923545c",
"name": "eip",
"description": "eip",
"vipUuid": "715b7942abc93c959e331d4582ede1e2",
"createDate": "May 5, 2014 5:44:49 PM",
"lastOpDate": "May 5, 2014 5:44:49 PM"
},
"success": true
}
}
 */
@RestResponse(allTo = "inventory")
public class APIDetachEipEvent extends APIEvent {
    /**
     * @desc see :ref:`EipInventory`
     */
    private EipInventory inventory;

    public EipInventory getInventory() {
        return inventory;
    }

    public void setInventory(EipInventory inventory) {
        this.inventory = inventory;
    }

    public APIDetachEipEvent(String apiId) {
        super(apiId);
    }

    public APIDetachEipEvent() {
        super(null);
    }
 
    public static APIDetachEipEvent __example__() {
        APIDetachEipEvent event = new APIDetachEipEvent();

        EipInventory eip = new EipInventory();
        eip.setVipUuid(uuid());
        eip.setVmNicUuid(uuid());
        eip.setName("Test-EIP");

        event.setInventory(eip);
        return event;
    }

}
