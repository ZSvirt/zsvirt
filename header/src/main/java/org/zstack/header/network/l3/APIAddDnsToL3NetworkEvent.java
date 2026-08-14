package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;

/**
 * @apiResult api event for message :ref:`APIAddDnsToL3NetworkMsg`
 * @category l3network
 * @example {
 * "org.zstack.header.network.l3.APIAddDnsToL3NetworkEvent": {
 * "inventory": {
 * "uuid": "22c3277f6b7540c8995bee842cf112d4",
 * "name": "basic",
 * "description": "Basic L3 Network without Vlan and no special services",
 * "type": "L3BasicNetwork",
 * "trafficType": "NotSpecified",
 * "zoneUuid": "01a9929069134e0a8d8687876bafeed4",
 * "l2NetworkUuid": "589ae22c2253423c9014c69411a6975e",
 * "state": "Enabled",
 * "createDate": "Apr 30, 2014 7:48:40 PM",
 * "lastOpDate": "Apr 30, 2014 7:48:40 PM",
 * "dns": [
 * "8.8.8.8"
 * ],
 * "ipRanges": [],
 * "networkServices": []
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse(allTo = "inventory")
public class APIAddDnsToL3NetworkEvent extends APIEvent {
    /**
     * @desc see :ref:`L3NetworkInventory`
     */
    private L3NetworkInventory inventory;

    public APIAddDnsToL3NetworkEvent(String apiId) {
        super(apiId);
    }

    public APIAddDnsToL3NetworkEvent() {
        super(null);
    }

    public L3NetworkInventory getInventory() {
        return inventory;
    }

    public void setInventory(L3NetworkInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIAddDnsToL3NetworkEvent __example__() {
        APIAddDnsToL3NetworkEvent event = new APIAddDnsToL3NetworkEvent();
        L3NetworkInventory l3 = new L3NetworkInventory();

        l3.setName("Test-L3Network");
        l3.setL2NetworkUuid(uuid());
        l3.setDns(Arrays.asList("8.8.8.8"));

        event.setInventory(l3);
        return event;
    }

}
