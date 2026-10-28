package org.zstack.header.cluster;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for message :ref:`APICreateClusterMsg`
 * @example {
 * "org.zstack.header.cluster.APICreateClusterEvent": {
 * "inventory": {
 * "name": "cluster1",
 * "uuid": "c1bd173d5cd84f0e9e7c47195ae27ec6",
 * "description": "test",
 * "state": "Enabled",
 * "hypervisorType": "KVM",
 * "createDate": "Apr 28, 2014 5:54:09 PM",
 * "lastOpDate": "Apr 28, 2014 5:54:09 PM",
 * "zoneUuid": "1b830f5bd1cb469b821b4b77babfdd6f",
 * "type": "zstack"
 * },
 * "success": true
 * }
 * }
 * @since zsv_0.1.0
 */
@RestResponse(allTo = "inventory")
public class APICreateClusterEvent extends APIEvent {
    /**
     * @desc cluster inventory (see :ref:`ClusterInventory`)
     */
    private ClusterInventory inventory;

    public APICreateClusterEvent() {
        super(null);
    }

    public APICreateClusterEvent(String apiId) {
        super(apiId);
    }

    public ClusterInventory getInventory() {
        return inventory;
    }

    public void setInventory(ClusterInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreateClusterEvent __example__() {
        APICreateClusterEvent event = new APICreateClusterEvent();
        event.setInventory(ClusterInventory.__example__());
        return event;
    }
}
