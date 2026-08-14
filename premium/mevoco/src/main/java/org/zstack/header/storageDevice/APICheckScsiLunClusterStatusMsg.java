package org.zstack.header.storageDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APIMultiAuditor;
import org.zstack.header.rest.RestRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Create by weiwang at 2018/10/26
 */
@RestRequest(
        path = "/storage-devices/scsi-lun/{uuid}/cluster/{clusterUuid}",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APICheckScsiLunClusterStatusReply.class
)
public class APICheckScsiLunClusterStatusMsg extends APISyncCallMessage implements APIMultiAuditor {
    @APIParam(resourceType = ScsiLunVO.class)
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

    public static APICheckScsiLunClusterStatusMsg __example__() {
        APICheckScsiLunClusterStatusMsg msg = new APICheckScsiLunClusterStatusMsg();
        msg.setUuid(uuid());
        msg.setClusterUuid(uuid());
        return msg;
    }

    @Override
    public List<APIAuditor.Result> multiAudit(APIMessage msg, APIEvent rsp) {
        APICheckScsiLunClusterStatusMsg amsg = (APICheckScsiLunClusterStatusMsg) msg;
        List<APIAuditor.Result> res = new ArrayList<>();
        res.add(new APIAuditor.Result(amsg.getClusterUuid(), ClusterVO.class));
        res.add(new APIAuditor.Result(amsg.getUuid(), ScsiLunVO.class));

        return res;
    }
}
