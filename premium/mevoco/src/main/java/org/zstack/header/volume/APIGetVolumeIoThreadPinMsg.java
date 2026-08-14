package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/volumes/{uuid}/io-thread-pin",
        method = HttpMethod.GET,
        responseClass = APIGetVolumeIoThreadPinReply.class
)
public class APIGetVolumeIoThreadPinMsg extends APISyncCallMessage implements VolumeMessage {
    @APIParam(resourceType = VolumeVO.class)
    private String uuid;

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public String getVolumeUuid() {
        return getUuid();
    }

    public static APIGetVolumeIoThreadPinMsg __example__() {
        APIGetVolumeIoThreadPinMsg msg = new APIGetVolumeIoThreadPinMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
