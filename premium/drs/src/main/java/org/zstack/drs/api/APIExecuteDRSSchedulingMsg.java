package org.zstack.drs.api;

import org.springframework.http.HttpMethod;
import org.zstack.drs.DRSMessage;
import org.zstack.drs.entity.ClusterDRSVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Create by lining at 2019/12/12
 */
@RestRequest(
        path = "/clusters/drs/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIExecuteDRSSchedulingEvent.class,
        isAction = true
)
public class APIExecuteDRSSchedulingMsg extends APIMessage implements DRSMessage {
    @APIParam(resourceType = ClusterDRSVO.class)
    private String uuid;

    @Override
    public String getDRSUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIExecuteDRSSchedulingMsg __example__() {
        APIExecuteDRSSchedulingMsg msg = new APIExecuteDRSSchedulingMsg();
        msg.setUuid(uuid(ClusterDRSVO.class));
        return msg;
    }
}
