package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by meilei007@gmail.com on 17/7/7
 */
@RestRequest(
        path = "/vm-instances/{uuid}/monitorNumber",
        method = HttpMethod.GET,
        responseClass = APIGetVmMonitorNumberReply.class
)
public class APIGetVmMonitorNumberMsg extends APISyncCallMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public static APIGetVmMonitorNumberMsg __example__() {
        APIGetVmMonitorNumberMsg msg = new APIGetVmMonitorNumberMsg();
        msg.uuid = uuid();
        return msg;
    }

}
