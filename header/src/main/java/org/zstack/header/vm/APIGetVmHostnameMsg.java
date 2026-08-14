package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2016/4/8.
 */
@RestRequest(
        path = "/vm-instances/{uuid}/hostnames",
        method = HttpMethod.GET,
        responseClass = APIGetVmHostnameReply.class
)
public class APIGetVmHostnameMsg extends APISyncCallMessage implements VmInstanceMessage {
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
 
    public static APIGetVmHostnameMsg __example__() {
        APIGetVmHostnameMsg msg = new APIGetVmHostnameMsg();
        msg.uuid = uuid();
        return msg;
    }

}
