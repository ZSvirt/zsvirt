package org.zstack.drs.api;

import org.springframework.http.HttpMethod;
import org.zstack.drs.DRSMessage;
import org.zstack.drs.entity.ClusterDRSVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by lining on 2019/12/12.
 */
@RestRequest(
        path = "/clusters/drs/status",
        responseClass = APIGetClusterDRSStatusReply.class,
        method = HttpMethod.GET
)
public class APIGetClusterDRSStatusMsg extends APISyncCallMessage implements DRSMessage {
    @APIParam(resourceType = ClusterDRSVO.class)
    private String drsUuid;

    public String getDrsUuid() {
        return drsUuid;
    }

    public void setDrsUuid(String drsUuid) {
        this.drsUuid = drsUuid;
    }

    @Override
    public String getDRSUuid() {
        return drsUuid;
    }

    public static APIGetClusterDRSStatusMsg __example__() {
        APIGetClusterDRSStatusMsg msg = new APIGetClusterDRSStatusMsg();
        msg.setDrsUuid(uuid(ClusterDRSVO.class));
        return msg;
    }
}
