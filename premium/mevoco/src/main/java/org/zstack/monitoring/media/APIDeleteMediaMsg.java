package org.zstack.monitoring.media;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2017/6/11.
 */
@RestRequest(
        path = "/media/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteMediaEvent.class
)
@Deprecated
public class APIDeleteMediaMsg extends APIDeleteMessage implements MediaMessage {
    @APIParam(resourceType = MediaVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getMediaMessageUuid() {
        return uuid;
    }

    public static APIDeleteMediaMsg __example__() {
        APIDeleteMediaMsg msg = new APIDeleteMediaMsg();
        msg.uuid = uuid();
        return msg;
    }
}
