package org.zstack.header.cluster;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * @apiResult api event for message :ref:`APIChangeClusterStateMsg`
 * @example {
 * "org.zstack.header.cluster.APIChangeClusterStateEvent": {
 * "inventory": {
 * "name": "cluster1",
 * "uuid": "44e981a73c7d414a995d5894b086670a",
 * "description": "Test",
 * "state": "Enabled",
 * "hypervisorType": "KVM",
 * "createDate": "Apr 28, 2014 6:48:15 PM",
 * "lastOpDate": "Apr 28, 2014 6:48:15 PM",
 * "zoneUuid": "def3040c73404a6f9abeba7f720748cc",
 * "type": "zstack"
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse(allTo = "inventory")
public class APIChangeClusterStateEvent extends APIEvent {
    /**
     * @desc cluster inventory (see :ref:`ClusterInventory`)
     */
    private ClusterInventory inventory;

    public APIChangeClusterStateEvent() {
        super(null);
    }

    public APIChangeClusterStateEvent(String apiId) {
        super(apiId);
    }

    public ClusterInventory getInventory() {
        return inventory;
    }

    public void setInventory(ClusterInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIChangeClusterStateEvent __example__() {
        APIChangeClusterStateEvent event = new APIChangeClusterStateEvent();
        ClusterInventory cluster = new ClusterInventory();
        cluster.setHypervisorType("KVM");
        cluster.setName("cluster1");
        cluster.setDescription("test");
        cluster.setState(ClusterState.Enabled.toString());
        cluster.setZoneUuid(uuid());
        cluster.setUuid(uuid());
        cluster.setType("zstack");
        cluster.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        cluster.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(cluster);
        return event;
    }

}
