package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/vm-instances/{uuid}/uptime",
        method = HttpMethod.GET,
        responseClass = APIGetVmUptimeReply.class
)
public class APIGetVmUptimeMsg extends APISyncCallMessage implements VmInstanceMessage {
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
        return getUuid();
    }

    public static APIGetVmUptimeMsg __example__() {
        APIGetVmUptimeMsg msg = new APIGetVmUptimeMsg();
        msg.setUuid(uuid());
        return msg;
    }

}

