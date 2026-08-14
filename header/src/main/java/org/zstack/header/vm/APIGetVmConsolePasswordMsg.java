package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by root on 7/29/16.
 */
@RestRequest(
        path = "/vm-instances/{uuid}/console-passwords",
        method = HttpMethod.GET,
        responseClass = APIGetVmConsolePasswordReply.class
)
public class APIGetVmConsolePasswordMsg extends APISyncCallMessage implements VmInstanceMessage {
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

    public static APIGetVmConsolePasswordMsg __example__() {
        APIGetVmConsolePasswordMsg msg = new APIGetVmConsolePasswordMsg();
        msg.uuid = uuid();
        return msg;
    }
}

