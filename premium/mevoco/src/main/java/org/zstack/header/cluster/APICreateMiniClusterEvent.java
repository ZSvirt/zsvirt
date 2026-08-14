package org.zstack.header.cluster;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APICreateMiniClusterEvent extends APIEvent {
    private ClusterInventory inventory;

    public APICreateMiniClusterEvent() {
        super(null);
    }

    public APICreateMiniClusterEvent(String apiId) {
        super(apiId);
    }

    public ClusterInventory getInventory() {
        return inventory;
    }

    public void setInventory(ClusterInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateMiniClusterEvent __example__() {
        APICreateMiniClusterEvent event = new APICreateMiniClusterEvent();
        ClusterInventory cluster = new ClusterInventory();
        cluster.setHypervisorType("KVM");
        cluster.setName("mini-cluster1");
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
