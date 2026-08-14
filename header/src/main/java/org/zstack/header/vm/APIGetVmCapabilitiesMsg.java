package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2016/5/17.
 */
@RestRequest(
        path = "/vm-instances/{uuid}/capabilities",
        responseClass = APIGetVmCapabilitiesReply.class,
        method = HttpMethod.GET
)
public class APIGetVmCapabilitiesMsg extends APISyncCallMessage implements VmInstanceMessage {
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
 
    public static APIGetVmCapabilitiesMsg __example__() {
        APIGetVmCapabilitiesMsg msg = new APIGetVmCapabilitiesMsg();
        msg.uuid = uuid();
        return msg;
    }

}
