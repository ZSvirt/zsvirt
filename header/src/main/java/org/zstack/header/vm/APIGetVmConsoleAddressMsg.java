package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 1/25/2016.
 */
@RestRequest(
        path = "/vm-instances/{uuid}/console-addresses",
        method = HttpMethod.GET,
        responseClass = APIGetVmConsoleAddressReply.class
)
public class APIGetVmConsoleAddressMsg extends APISyncCallMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
 
    public static APIGetVmConsoleAddressMsg __example__() {
        APIGetVmConsoleAddressMsg msg = new APIGetVmConsoleAddressMsg();
        msg.uuid = uuid();
        return msg;
    }

}
