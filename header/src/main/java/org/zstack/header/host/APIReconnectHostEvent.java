package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult
 * @example {
 * "org.zstack.header.host.APIReconnectHostEvent": {
 * "inventory": {
 * "zoneUuid": "f9c6e213e08c4f8e9b706f0a3033ceca",
 * "name": "host1-192.168.0.203",
 * "uuid": "5ea9605b1d754077b2c9dfca05fc904b",
 * "clusterUuid": "19e518f0101a4639be9c7e8c1b681936",
 * "description": "Test",
 * "managementIp": "192.168.0.203",
 * "hypervisorType": "KVM",
 * "state": "Enabled",
 * "status": "Connected",
 * "createDate": "Apr 30, 2014 4:35:10 PM",
 * "lastOpDate": "Apr 30, 2014 4:35:10 PM"
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse(allTo = "inventory")
public class APIReconnectHostEvent extends APIEvent {
    /**
     * @desc see :ref:`HostInventory`
     */
    private HostInventory inventory;

    public APIReconnectHostEvent() {
        super(null);
    }

    public APIReconnectHostEvent(String apiId) {
        super(apiId);
    }

    public HostInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostInventory inventory) {
        this.inventory = inventory;
    }


 
    public static APIReconnectHostEvent __example__() {

        APIReconnectHostEvent event = new APIReconnectHostEvent();
        HostInventory hi = new HostInventory();
        hi.setName("example");
        hi.setClusterUuid(uuid());
        hi.setManagementIp("192.168.0.1");
        hi.setAvailableCpuCapacity(100000L);
        hi.setAvailableMemoryCapacity(100000L);
        return event;
    }

}
