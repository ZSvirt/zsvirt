package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for APIAddIpRangeMsg
 * @category l3Network
 * @example {
 * "org.zstack.header.network.l3.APIAddIpRangeEvent": {
 * "inventory": {
 * "uuid": "ba84b1e3e75a495bb06c67e3d5a95a28",
 * "l3NetworkUuid": "22c3277f6b7540c8995bee842cf112d4",
 * "name": "public ip range",
 * "description": "Test",
 * "startIp": "192.168.0.10",
 * "endIp": "192.168.0.90",
 * "netmask": "255.255.255.0",
 * "gateway": "192.168.0.1",
 * "createDate": "Apr 30, 2014 7:48:47 PM",
 * "lastOpDate": "Apr 30, 2014 7:48:47 PM"
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse(allTo = "inventory")
public class APIAddIpRangeEvent extends APIEvent {
    /**
     * @desc see :ref:`IpRangeInventory`
     */
    private IpRangeInventory inventory;

    public APIAddIpRangeEvent(String apiId) {
        super(apiId);
    }

    public APIAddIpRangeEvent() {
        super(null);
    }

    public IpRangeInventory getInventory() {
        return inventory;
    }

    public void setInventory(IpRangeInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIAddIpRangeEvent __example__() {
        APIAddIpRangeEvent event = new APIAddIpRangeEvent();
        IpRangeInventory ipRange = new IpRangeInventory();

        ipRange.setL3NetworkUuid(uuid());
        ipRange.setName("Test-IP-Range");
        ipRange.setStartIp("192.168.100.10");
        ipRange.setEndIp("192.168.100.250");
        ipRange.setNetmask("255.255.255.0");
        ipRange.setGateway("192.168.100.1");

        event.setInventory(ipRange);
        return event;
    }

}
