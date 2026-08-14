package org.zstack.header.baremetal.pxeserver;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 2018-10-11.
 */
@RestRequest(
        path = "/clusters/{clusterUuid}/pxeservers/{pxeServerUuid}",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAttachBaremetalPxeServerToClusterEvent.class
)
public class APIAttachBaremetalPxeServerToClusterMsg extends APIMessage implements APIAuditor {
    @APIParam(resourceType = BaremetalPxeServerVO.class)
    private String pxeServerUuid;

    @APIParam(resourceType = ClusterVO.class)
    private String clusterUuid;

    public String getPxeServerUuid() {
        return pxeServerUuid;
    }

    public void setPxeServerUuid(String pxeServerUuid) {
        this.pxeServerUuid = pxeServerUuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIAttachBaremetalPxeServerToClusterEvent)rsp).getInventory().getUuid() : "", BaremetalPxeServerVO.class);
    }

    public static APIAttachBaremetalPxeServerToClusterMsg __example__() {
        APIAttachBaremetalPxeServerToClusterMsg msg = new APIAttachBaremetalPxeServerToClusterMsg();
        msg.setPxeServerUuid(uuid());
        msg.setClusterUuid(uuid());
        return msg;
    }
}
