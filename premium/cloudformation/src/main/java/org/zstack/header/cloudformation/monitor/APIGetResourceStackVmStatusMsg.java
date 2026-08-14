package org.zstack.header.cloudformation.monitor;

import org.springframework.http.HttpMethod;
import org.zstack.header.cloudformation.ResourceStackVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2019/11/22.
 */
@RestRequest(
        path = "/cloudformation/stack/monitor/vmstatus",
        method = HttpMethod.GET,
        responseClass = APIGetResourceStackVmStatusReply.class
)
public class APIGetResourceStackVmStatusMsg extends APISyncCallMessage {
    @APIParam(resourceType = ResourceStackVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIGetResourceStackVmStatusMsg __example__() {
        APIGetResourceStackVmStatusMsg msg = new APIGetResourceStackVmStatusMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
