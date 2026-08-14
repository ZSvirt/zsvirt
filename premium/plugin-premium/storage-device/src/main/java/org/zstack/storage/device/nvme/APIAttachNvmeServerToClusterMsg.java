package org.zstack.storage.device.nvme;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/clusters/{clusterUuid}/storage-devices/nvme/servers/{uuid}",
        method = HttpMethod.POST,
        responseClass = APIAttachNvmeServerToClusterEvent.class,
        parameterName = "params"
)
public class APIAttachNvmeServerToClusterMsg extends APIMessage implements APIAuditor {
    @APIParam(resourceType = NvmeServerVO.class)
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

    public static APIAttachNvmeServerToClusterMsg __example__() {
        APIAttachNvmeServerToClusterMsg msg = new APIAttachNvmeServerToClusterMsg();
        msg.setUuid(uuid());
        msg.setClusterUuid(uuid());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(
                rsp.isSuccess() ? ((APIAttachNvmeServerToClusterEvent) rsp).getInventory().getUuid() : "",
                NvmeServerVO.class
        );
    }
}
