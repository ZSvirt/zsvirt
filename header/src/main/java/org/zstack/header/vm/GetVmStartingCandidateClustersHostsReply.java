package org.zstack.header.vm;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 * Created by weiwang on 04/09/2017
 */
public class GetVmStartingCandidateClustersHostsReply extends MessageReply {
    private List<HostInventory> hostInventories;
    private List<ClusterInventory> clusterInventories;

    public List<HostInventory> getHostInventories() {
        return hostInventories;
    }

    public void setHostInventories(List<HostInventory> hostInventories) {
        this.hostInventories = hostInventories;
    }

    public List<ClusterInventory> getClusterInventories() {
        return clusterInventories;
    }

    public void setClusterInventories(List<ClusterInventory> clusterInventories) {
        this.clusterInventories = clusterInventories;
    }
}
