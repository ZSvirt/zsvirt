package org.zstack.header.vm;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2016/5/14.
 */
@RestResponse(fieldsTo = {"hosts=hostInventories", "clusters=clusterInventories"})
public class APIGetVmStartingCandidateClustersHostsReply extends APIReply {
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
 
    public static APIGetVmStartingCandidateClustersHostsReply __example__() {
        APIGetVmStartingCandidateClustersHostsReply reply = new APIGetVmStartingCandidateClustersHostsReply();

        String clusterUuid = uuid();

        HostInventory hi = new HostInventory ();
        hi.setAvailableCpuCapacity(2L);
        hi.setAvailableMemoryCapacity(4L);
        hi.setClusterUuid(clusterUuid);
        hi.setManagementIp("192.168.0.1");
        hi.setName("example");
        hi.setState(HostState.Enabled.toString());
        hi.setStatus(HostStatus.Connected.toString());
        hi.setClusterUuid(uuid());
        hi.setZoneUuid(uuid());
        hi.setUuid(uuid());
        hi.setTotalCpuCapacity(4L);
        hi.setTotalMemoryCapacity(4L);
        hi.setHypervisorType("KVM");
        hi.setDescription("example");
        hi.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        hi.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setHostInventories(asList(hi));

        ClusterInventory cl = new ClusterInventory();
        cl.setName("cluster1");
        cl.setUuid(clusterUuid);
        cl.setZoneUuid(uuid());
        cl.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        cl.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        cl.setHypervisorType("KVM");

        reply.setClusterInventories(asList(cl));


        return reply;
    }

}
