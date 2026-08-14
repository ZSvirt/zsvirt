package org.zstack.header.network.service;

import org.zstack.header.message.APIEvent;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @apiResult api event for message :ref:`APIAttachNetworkServiceToL3NetworkMsg`
 * @category network service
 * @example {
 * "org.zstack.header.network.service.APIAttachNetworkServiceToL3NetworkEvent": {
 * "inventory": {
 * "uuid": "fba7bf08a590444c9e21eee394b61135",
 * "name": "GuestNetwork",
 * "description": "Test",
 * "type": "L3BasicNetwork",
 * "trafficType": "NotSpecified",
 * "zoneUuid": "48c5febd96024e33809cc98035d79277",
 * "l2NetworkUuid": "a766f7dec6e5477f9842289950b51e63",
 * "state": "Enabled",
 * "createDate": "May 3, 2014 9:19:08 PM",
 * "lastOpDate": "May 3, 2014 9:19:08 PM",
 * "dns": [],
 * "ipRanges": [
 * {
 * "uuid": "34ba62fd8b0246e5a23763a917467934",
 * "l3NetworkUuid": "fba7bf08a590444c9e21eee394b61135",
 * "name": "TestIpRange",
 * "description": "Test",
 * "startIp": "10.10.2.100",
 * "endIp": "10.20.2.200",
 * "netmask": "255.0.0.0",
 * "gateway": "10.10.2.1",
 * "createDate": "May 3, 2014 9:19:08 PM",
 * "lastOpDate": "May 3, 2014 9:19:08 PM"
 * }
 * ],
 * "networkServices": [
 * {
 * "l3NetworkUuid": "fba7bf08a590444c9e21eee394b61135",
 * "networkServiceProviderUuid": "1d1d5ff248b24906a39f96aa3c6411dd",
 * "networkServiceType": "PortForwarding"
 * }
 * ]
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */

@RestResponse(allTo = "inventory")
public class APIAttachNetworkServiceToL3NetworkEvent extends APIEvent {
    /**
     * @desc see :ref:`L3NetworkInventory`
     */
    private L3NetworkInventory inventory;

    public APIAttachNetworkServiceToL3NetworkEvent() {
        super(null);
    }

    public APIAttachNetworkServiceToL3NetworkEvent(String apiId) {
        super(apiId);
    }

    public L3NetworkInventory getInventory() {
        return inventory;
    }

    public void setInventory(L3NetworkInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIAttachNetworkServiceToL3NetworkEvent __example__() {
        APIAttachNetworkServiceToL3NetworkEvent event = new APIAttachNetworkServiceToL3NetworkEvent();
        L3NetworkInventory l3 = new L3NetworkInventory();
        NetworkServiceL3NetworkRefInventory ns = new NetworkServiceL3NetworkRefInventory();

        String l3Uuid = uuid();
        ns.setL3NetworkUuid(l3Uuid);
        ns.setNetworkServiceProviderUuid(uuid());
        ns.setNetworkServiceType("PortForwarding");

        l3.setUuid(l3Uuid);
        l3.setNetworkServices(Arrays.asList(ns));

        return event;
    }

}
