package org.zstack.header.allocator;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestRequest(
        path = "/hosts/capacities/cpu-memory",
        method = HttpMethod.GET,
        responseClass = APIGetCpuMemoryCapacityReply.class
)
public class APIGetCpuMemoryCapacityMsg extends APISyncCallMessage {
    @APIParam(required = false, resourceType = ZoneVO.class)
    private List<String> zoneUuids;
    @APIParam(required = false, resourceType = ClusterVO.class)
    private List<String> clusterUuids;
    @APIParam(required = false, resourceType = HostVO.class)
    private List<String> hostUuids;
    @APIParam(required = false, validValues = {"KVM", "ESX"})
    private String hypervisorType = null;

    private boolean all;

    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }

    public List<String> getZoneUuids() {
        return zoneUuids;
    }

    public void setZoneUuids(List<String> zoneUuids) {
        this.zoneUuids = zoneUuids;
    }

    public List<String> getClusterUuids() {
        return clusterUuids;
    }

    public void setClusterUuids(List<String> clusterUuids) {
        this.clusterUuids = clusterUuids;
    }

    public List<String> getHostUuids() {
        return hostUuids;
    }

    public void setHostUuids(List<String> hostUuids) {
        this.hostUuids = hostUuids;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public static APIGetCpuMemoryCapacityMsg __example__() {
        APIGetCpuMemoryCapacityMsg msg = new APIGetCpuMemoryCapacityMsg();
        msg.setAll(true);
        msg.setZoneUuids(list(uuid(),uuid()));
        msg.setClusterUuids(list(uuid(),uuid()));
        msg.setHostUuids(list(uuid(),uuid()));
        msg.setHypervisorType("KVM");
        return msg;
    }

}
