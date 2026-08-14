package org.zstack.monitoring;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2017/6/28.
 */
@RestRequest(
        path = "/monitoring/items",
        method = HttpMethod.GET,
        responseClass = APIGetMonitorItemReply.class
)
@Deprecated
public class APIGetMonitorItemMsg extends APISyncCallMessage {
    @APIParam
    private String resourceType;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public static APIGetMonitorItemMsg __example__() {
        APIGetMonitorItemMsg msg = new APIGetMonitorItemMsg();
        msg.resourceType = HostVO.class.getSimpleName();
        return msg;
    }
}
