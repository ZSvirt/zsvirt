package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for :ref:`APICreateL3NetworkMsg`
 * @category l3Network
 * @example {
 * "org.zstack.header.network.l3.APICreateL3NetworkEvent": {
 * "inventory": {
 * "uuid": "4ea8c07361314a058a261a9daae1a414",
 * "name": "PublicNetwork",
 * "description": "Test",
 * "type": "L3BasicNetwork",
 * "trafficType": "NotSpecified",
 * "zoneUuid": "48c5febd96024e33809cc98035d79277",
 * "l2NetworkUuid": "a766f7dec6e5477f9842289950b51e63",
 * "state": "Enabled",
 * "createDate": "May 3, 2014 9:19:08 PM",
 * "lastOpDate": "May 3, 2014 9:19:08 PM",
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
public class APICreateL3NetworkEvent extends APIEvent {
    /**
     * @desc see :ref:`L3NetworkInventory`
     */
    private L3NetworkInventory inventory;

    public APICreateL3NetworkEvent() {
        super(null);
    }

    public APICreateL3NetworkEvent(String apiId) {
        super(apiId);
    }

    public L3NetworkInventory getInventory() {
        return inventory;
    }

    public void setInventory(L3NetworkInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreateL3NetworkEvent __example__() {
        APICreateL3NetworkEvent event = new APICreateL3NetworkEvent();
        L3NetworkInventory l3 = new L3NetworkInventory();

        l3.setName("Test-L3Network");
        l3.setL2NetworkUuid(uuid());
        l3.setCategory(L3NetworkCategory.Private.toString());

        event.setInventory(l3);
        return event;
    }

}
