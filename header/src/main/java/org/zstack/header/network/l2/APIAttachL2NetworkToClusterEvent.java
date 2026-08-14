package org.zstack.header.network.l2;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for message :ref:`APIAttachL2NetworkToClusterMsg`
 * @category l2network
 * @example {
 * "org.zstack.header.network.l2.APIAttachL2NetworkToClusterEvent": {
 * "inventory": {
 * "uuid": "a766f7dec6e5477f9842289950b51e63",
 * "name": "TestL2Network",
 * "description": "Test",
 * "zoneUuid": "48c5febd96024e33809cc98035d79277",
 * "physicalInterface": "eth0",
 * "type": "L2NoVlanNetwork",
 * "createDate": "May 3, 2014 9:19:08 PM",
 * "lastOpDate": "May 3, 2014 9:19:08 PM",
 * "attachedClusterUuids": [
 * "cb97e076b2e7497d9d4018fb4b4cfcea"
 * ]
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */

@RestResponse(allTo = "inventory")
public class APIAttachL2NetworkToClusterEvent extends APIEvent {
    /**
     * @desc see :ref:`L2NetworkInventory`
     */
    private L2NetworkInventory inventory;

    public APIAttachL2NetworkToClusterEvent(String apiId) {
        super(apiId);
    }

    public APIAttachL2NetworkToClusterEvent() {
        super(null);
    }

    public L2NetworkInventory getInventory() {
        return inventory;
    }

    public void setInventory(L2NetworkInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIAttachL2NetworkToClusterEvent __example__() {
        APIAttachL2NetworkToClusterEvent event = new APIAttachL2NetworkToClusterEvent();
        L2VlanNetworkInventory net = new L2VlanNetworkInventory();

        net.setName("Test-Net");
        net.setVlan(10);
        net.setDescription("Test");
        net.setZoneUuid(uuid());
        net.setPhysicalInterface("eth0");
        net.setType("L2VlanNetwork");

        event.setInventory(net);
        return event;
    }

}
