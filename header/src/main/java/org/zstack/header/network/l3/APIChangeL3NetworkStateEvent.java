package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for :ref:`APIChangeL3NetworkStateMsg`
 * @category l3network
 * @example {
 * "org.zstack.header.network.l3.APIChangeL3NetworkStateEvent": {
 * "inventory": {
 * "uuid": "3424b7a643c348c795aaa8df59c5044f",
 * "name": "Test-L3Network",
 * "description": "Test",
 * "type": "L3BasicNetwork",
 * "trafficType": "NotSpecified",
 * "zoneUuid": "8ee8fd26a2ee43a19a27f0166009d247",
 * "l2NetworkUuid": "7c4ad3812a9a44c3a395c326699b1a4e",
 * "state": "Disabled",
 * "createDate": "May 3, 2014 9:40:07 PM",
 * "lastOpDate": "May 3, 2014 9:40:07 PM",
 * "dns": [],
 * "ipRanges": [],
 * "networkServices": []
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse(allTo = "inventory")
public class APIChangeL3NetworkStateEvent extends APIEvent {
    /**
     * @desc see :ref:`L3NetworkInventory`
     */
    private L3NetworkInventory inventory;

    public APIChangeL3NetworkStateEvent(String apiId) {
        super(apiId);
    }

    public APIChangeL3NetworkStateEvent() {
        super(null);
    }

    public L3NetworkInventory getInventory() {
        return inventory;
    }

    public void setInventory(L3NetworkInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIChangeL3NetworkStateEvent __example__() {
        APIChangeL3NetworkStateEvent event = new APIChangeL3NetworkStateEvent();
        L3NetworkInventory l3 = new L3NetworkInventory();

        l3.setName("Test-L3Network");
        l3.setL2NetworkUuid(uuid());

        event.setInventory(l3);
        return event;
    }

}
