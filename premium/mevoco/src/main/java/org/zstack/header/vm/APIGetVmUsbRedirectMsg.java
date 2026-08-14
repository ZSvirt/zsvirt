package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by meilei007@gmail.com on 17/7/7
 */
@RestRequest(
        path = "/vm-instances/{uuid}/usbredirect",
        method = HttpMethod.GET,
        responseClass = APIGetVmUsbRedirectReply.class
)
public class APIGetVmUsbRedirectMsg extends APISyncCallMessage implements VmInstanceMessage {
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

    public static APIGetVmUsbRedirectMsg __example__() {
        APIGetVmUsbRedirectMsg msg = new APIGetVmUsbRedirectMsg();
        msg.uuid = uuid();
        return msg;
    }

}
