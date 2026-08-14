package org.zstack.network.service.eip;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 *@apiResult
 *
 * api event for message :ref:`APIAttachEipMsg`
 *
 *@category eip
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.service.eip.APIAttachEipEvent": {
"inventory": {
"uuid": "69198105fd7a40778fba1759b923545c",
"name": "eip",
"description": "eip",
"vmNicUuid": "0bdb4fb64baa4c4d85d39c9f43773342",
"vipUuid": "715b7942abc93c959e331d4582ede1e2",
"createDate": "May 5, 2014 5:44:49 PM",
"lastOpDate": "May 5, 2014 5:44:49 PM"
},
"success": true
}
}
 */
@RestResponse(allTo = "inventory")
public class APIAttachEipEvent extends APIEvent {
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

    public APIAttachEipEvent(String apiId) {
        super(apiId);
    }
    public APIAttachEipEvent() {
        super(null);
    }
 
    public static APIAttachEipEvent __example__() {
        APIAttachEipEvent event = new APIAttachEipEvent();
        EipInventory eip = new EipInventory();
        eip.setVipUuid(uuid());
        eip.setVmNicUuid(uuid());

        return event;
    }

}
