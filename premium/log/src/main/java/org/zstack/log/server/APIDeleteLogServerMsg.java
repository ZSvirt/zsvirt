package org.zstack.log.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/log/servers",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteLogServerEvent.class
)
public class APIDeleteLogServerMsg extends APIMessage implements LogServerMessage {
    @APIParam(resourceType = LogServerVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getLogServerUuid() {
        return uuid;
    }

    public static APIDeleteLogServerMsg __example__() {
        APIDeleteLogServerMsg msg = new APIDeleteLogServerMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
