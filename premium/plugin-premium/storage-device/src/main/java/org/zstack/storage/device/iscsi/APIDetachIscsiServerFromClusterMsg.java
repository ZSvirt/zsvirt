package org.zstack.storage.device.iscsi;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * Create by weiwang at 2018/8/2
 */
@RestRequest(
        path = "/clusters/{clusterUuid}/storage-devices/iscsi/servers/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDetachIscsiServerFromClusterEvent.class
)
public class APIDetachIscsiServerFromClusterMsg extends APIMessage implements APIAuditor {
    @APIParam(resourceType = IscsiServerVO.class)
    private String uuid;

    @APIParam(resourceType = ClusterVO.class)
    private String clusterUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public static APIDetachIscsiServerFromClusterMsg __example__() {
        APIDetachIscsiServerFromClusterMsg msg = new APIDetachIscsiServerFromClusterMsg();
        msg.setUuid(uuid());
        msg.setClusterUuid(uuid());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(
                rsp.isSuccess() ? ((APIDetachIscsiServerFromClusterEvent) rsp).getInventory().getUuid() : "",
                IscsiServerVO.class
        );
    }
}
