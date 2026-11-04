package org.zstack.header.network.l2;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for :ref:`APICreateL2NetworkMsg`
 * @category l2Network
 * @example {
 * "org.zstack.header.network.l2.APICreateL2NetworkMsg": {
 * "name": "TestL2Network",
 * "description": "Test",
 * "zoneUuid": "48c5febd96024e33809cc98035d79277",
 * "physicalInterface": "eth0",
 * "type": "L2NoVlanNetwork",
 * "session": {
 * "uuid": "d93f354c4339450e8c2a4c31de89da15"
 * },
 * "timeout": 1800000,
 * "id": "7b58a8e291e54d41bc3fe643bb1c76b4",
 * "serviceId": "api.portal"
 * }
 * }
 * @since zsv_0.1.0
 */

@RestResponse(allTo = "inventory")
public class APICreateL2NetworkEvent extends APIEvent {
    /**
     * @desc see :ref:`L2NetworkInventory`
     */
    private L2NetworkInventory inventory;

    public APICreateL2NetworkEvent() {
        super(null);
    }

    public APICreateL2NetworkEvent(String apiId) {
        super(apiId);
    }

    public L2NetworkInventory getInventory() {
        return inventory;
    }

    public void setInventory(L2NetworkInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateL2NetworkEvent __example__() {
        APICreateL2NetworkEvent event = new APICreateL2NetworkEvent();
        event.setInventory(L2NetworkInventory.__example__());
        return event;
    }
}
