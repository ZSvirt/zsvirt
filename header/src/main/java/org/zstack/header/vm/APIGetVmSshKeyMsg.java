package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by luchukun on 8/4/16.
 */
@RestRequest(
        path = "/vm-instances/{uuid}/ssh-keys",
        method = HttpMethod.GET,
        responseClass = APIGetVmSshKeyReply.class
)
public class APIGetVmSshKeyMsg extends APISyncCallMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }
 
    public static APIGetVmSshKeyMsg __example__() {
        APIGetVmSshKeyMsg msg = new APIGetVmSshKeyMsg();
        msg.uuid = uuid();
        return msg;
    }

}
