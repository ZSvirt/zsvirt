package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by longtao.wu@zstack.io on 21/12/01
 */
@RestRequest(
        path = "/vm-instances/{uuid}/vnuma",
        method = HttpMethod.GET,
        responseClass = APIGetVmNumaReply.class
)
public class APIGetVmNumaMsg extends APISyncCallMessage implements VmInstanceMessage {
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

    public static APIGetVmNumaMsg __example__() {
        APIGetVmNumaMsg msg = new APIGetVmNumaMsg();
        msg.uuid = uuid();
        return msg;
    }

}
