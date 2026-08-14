package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 16/12/9.
 */
@RestRequest(
        path = "/vm-instances/{uuid}/nic-qos",
        method = HttpMethod.GET,
        responseClass = APIGetNicQosReply.class
)
public class APIGetNicQosMsg extends APISyncCallMessage {
    @APIParam
    private String uuid;
    @APIParam(required = false)
    private Boolean forceSync = false;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Boolean getForceSync() {
        return forceSync;
    }

    public void setForceSync(Boolean forceSync) {
        this.forceSync = forceSync;
    }

    public static APIGetNicQosMsg __example__() {
        APIGetNicQosMsg msg = new APIGetNicQosMsg();
        msg.setUuid(uuid());

        return msg;
    }

}
