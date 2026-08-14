package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 17/1/4.
 */
@RestRequest(
        path = "/images/{uuid}/qga",
        method = HttpMethod.GET,
        responseClass = APIGetImageQgaReply.class
)
public class APIGetImageQgaMsg extends APISyncCallMessage {
    @APIParam
    String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
 
    public static APIGetImageQgaMsg __example__() {
        APIGetImageQgaMsg msg = new APIGetImageQgaMsg();
        msg.setUuid(uuid());
        return msg;
    }

}
