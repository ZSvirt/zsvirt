package org.zstack.network.service.eip;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestResponse;

/**
 *@apiResult
 *
 * api event for :ref:`APICreateEipMsg`
 *
 *@category eip
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.service.eip.APICreateEipEvent": {
"inventory": {
"uuid": "419f99b39acf4d468f901372f6d06c93",
"name": "eip",
"description": "eip",
"vmNicUuid": "82d038288d724a04bc6c57ad67e5528a",
"vipUuid": "779aecf223eb3855bc8bb6056342ae57",
"createDate": "May 5, 2014 5:42:47 PM",
"lastOpDate": "May 5, 2014 5:42:47 PM"
},
"success": true
}
}
 */
@RestResponse(allTo = "inventory")
public class APICreateEipEvent extends APIEvent {
    /**
     * @desc see :ref:`EipInventory`
     */
    private EipInventory inventory;

    public APICreateEipEvent(String apiId) {
        super(apiId);
    }
    public APICreateEipEvent() {
        super(null);
    }

    public EipInventory getInventory() {
        return inventory;
    }

    public void setInventory(EipInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreateEipEvent __example__() {
        APICreateEipEvent event = new APICreateEipEvent();
        EipInventory eip = new EipInventory();

        eip.setName("Test-EIP");
        eip.setVipUuid(uuid());
        eip.setVmNicUuid(uuid());

        event.setInventory(eip);
        return event;
    }

}
