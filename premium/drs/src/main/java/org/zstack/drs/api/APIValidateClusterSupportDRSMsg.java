package org.zstack.drs.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by lining on 2019/12/12.
 */
@RestRequest(
        path = "/clusters/{clusterUuid}/drs/valid",
        responseClass = APIValidateClusterSupportDRSReply.class,
        method = HttpMethod.GET
)
public class APIValidateClusterSupportDRSMsg extends APISyncCallMessage {
    @APIParam(resourceType = ClusterVO.class)
    private String clusterUuid;

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public static APIValidateClusterSupportDRSMsg __example__() {
        APIValidateClusterSupportDRSMsg msg = new APIValidateClusterSupportDRSMsg();
        msg.setClusterUuid(uuid(ClusterVO.class));
        return msg;
    }
}
