package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 17/1/4.
 */
@RestRequest(
        path = "/vm-instances/{uuid}/qga",
        method = HttpMethod.GET,
        responseClass = APIGetVmQgaReply.class
)
public class APIGetVmQgaMsg extends APISyncCallMessage {
    @APIParam
    String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
 
    public static APIGetVmQgaMsg __example__() {
        APIGetVmQgaMsg msg = new APIGetVmQgaMsg();
        msg.setUuid(uuid());

        return msg;
    }

}
