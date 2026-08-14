package org.zstack.header.network.l2;

import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for :ref:`APICreateL2VlanNetworkMsg`
 * @category l2network
 * @example {
 * "org.zstack.header.network.l2.APICreateL2VlanNetworkEvent": {
 * "inventory": {
 * "vlan": 10,
 * "uuid": "9186dff66cd94485bc6adb0b360cd7e0",
 * "name": "TestL2VlanNetwork",
 * "description": "Test",
 * "zoneUuid": "d81c3d3d008e46038b8a38fee595fe41",
 * "physicalInterface": "eth0",
 * "type": "L2VlanNetwork",
 * "createDate": "May 3, 2014 10:57:12 PM",
 * "lastOpDate": "May 3, 2014 10:57:12 PM",
 * "attachedClusterUuids": []
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse(allTo = "inventory")
public class APICreateL2VlanNetworkEvent extends APICreateL2NetworkEvent {
    public APICreateL2VlanNetworkEvent(String apiId) {
        super(apiId);
    }

    public APICreateL2VlanNetworkEvent() {
        super(null);
    }

    public static APICreateL2VlanNetworkEvent __example__() {
        APICreateL2VlanNetworkEvent event = new APICreateL2VlanNetworkEvent();
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
