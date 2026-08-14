package org.zstack.drs.api;

import org.springframework.http.HttpMethod;
import org.zstack.drs.DRSMessage;
import org.zstack.drs.entity.ClusterDRSVO;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by lining on 2019/12/12.
 */
@RestRequest(
        path = "/clusters/drs/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteClusterDRSEvent.class
)
public class APIDeleteClusterDRSMsg extends APIDeleteMessage implements DRSMessage {
    @APIParam(resourceType = ClusterDRSVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getDRSUuid() {
        return uuid;
    }

    public static APIDeleteClusterDRSMsg __example__() {
        APIDeleteClusterDRSMsg msg = new APIDeleteClusterDRSMsg();
        msg.setUuid(uuid(ClusterDRSVO.class));
        return msg;
    }
}
