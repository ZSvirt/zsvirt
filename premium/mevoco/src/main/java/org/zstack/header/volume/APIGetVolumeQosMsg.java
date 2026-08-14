package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 16/12/9.
 */
@RestRequest(
        path = "/volumes/{uuid}/qos",
        method = HttpMethod.GET,
        responseClass = APIGetVolumeQosReply.class
)
public class APIGetVolumeQosMsg extends APISyncCallMessage {
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

    public static APIGetVolumeQosMsg __example__() {
        APIGetVolumeQosMsg msg = new APIGetVolumeQosMsg();
        msg.setUuid(uuid());
        return  msg;
    }

}
