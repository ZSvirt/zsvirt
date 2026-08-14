package org.zstack.header.vm;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.zone.ZoneInventory;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2016/8/17.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetCandidateZonesClustersHostsForCreatingVmReply extends APIReply {
    private List<ZoneInventory> zones;
    private List<ClusterInventory> clusters;
    private List<HostInventory> hosts;

    public List<ZoneInventory> getZones() {
        return zones;
    }

    public void setZones(List<ZoneInventory> zones) {
        this.zones = zones;
    }

    public List<ClusterInventory> getClusters() {
        return clusters;
    }

    public void setClusters(List<ClusterInventory> clusters) {
        this.clusters = clusters;
    }

    public List<HostInventory> getHosts() {
        return hosts;
    }

    public void setHosts(List<HostInventory> hosts) {
        this.hosts = hosts;
    }
 
    public static APIGetCandidateZonesClustersHostsForCreatingVmReply __example__() {
        APIGetCandidateZonesClustersHostsForCreatingVmReply reply = new APIGetCandidateZonesClustersHostsForCreatingVmReply();

        String zoneUuid = uuid();
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
        hi.setZoneUuid(zoneUuid);
        hi.setUuid(uuid());
        hi.setTotalCpuCapacity(4L);
        hi.setTotalMemoryCapacity(4L);
        hi.setHypervisorType("KVM");
        hi.setDescription("example");
        hi.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        hi.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setHosts(asList(hi));

        ClusterInventory cl = new ClusterInventory();
        cl.setName("cluster1");
        cl.setUuid(clusterUuid);
        cl.setZoneUuid(uuid());
        cl.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        cl.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        cl.setHypervisorType("KVM");

        reply.setClusters(asList(cl));

        ZoneInventory z = new ZoneInventory();
        z.setName("zone");
        z.setUuid(zoneUuid);

        reply.setZones(asList(z));

        return reply;
    }

}
