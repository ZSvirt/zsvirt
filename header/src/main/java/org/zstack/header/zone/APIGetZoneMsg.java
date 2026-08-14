package org.zstack.header.zone;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/zones/{uuid}/info",
        method = HttpMethod.GET,
        responseClass = APIGetZoneReply.class
)
public class APIGetZoneMsg extends APISyncCallMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIGetZoneMsg __example__() {
        APIGetZoneMsg msg = new APIGetZoneMsg();
        msg.setUuid(uuid());
        return msg;
    }

}
