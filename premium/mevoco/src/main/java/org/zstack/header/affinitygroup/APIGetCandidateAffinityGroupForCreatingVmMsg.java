package org.zstack.header.affinitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;

/**
 * @author qiuyu.zhang
 * @Package org.zstack.header.affinitygroup
 * @date 2021/2/24 10:38 AM
 */

@RestRequest(
        path = "/vm-instances/candidate-affinityGroup",
        method = HttpMethod.GET,
        responseClass = APIGetCandidateAffinityGroupForCreatingVmReply.class
)
public class APIGetCandidateAffinityGroupForCreatingVmMsg extends APISyncCallMessage {
    @APIParam(resourceType = ZoneVO.class)
    private String zoneUuid;
    @APIParam(required = false, resourceType = ClusterVO.class)
    private String clusterUuid;
    @APIParam(resourceType = HostVO.class, required = false)
    private String hostUuid;

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public static APIGetCandidateAffinityGroupForCreatingVmMsg __example__() {
        APIGetCandidateAffinityGroupForCreatingVmMsg msg = new APIGetCandidateAffinityGroupForCreatingVmMsg();
        msg.setZoneUuid(uuid());
        msg.setClusterUuid(uuid());
        msg.setHostUuid(uuid());
        return msg;
    }
}
