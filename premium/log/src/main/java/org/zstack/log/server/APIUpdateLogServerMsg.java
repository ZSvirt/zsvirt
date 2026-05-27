package org.zstack.log.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/log/servers",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateLogServerEvent.class
)
public class APIUpdateLogServerMsg extends APIMessage implements APIAuditor, LogServerMessage {
    @APIParam(resourceType = LogServerVO.class)
    private String uuid;

    @APIParam(required = false, maxLength = 255)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static APIUpdateLogServerMsg __example__() {
        APIUpdateLogServerMsg msg = new APIUpdateLogServerMsg();
        msg.setUuid(uuid());
        msg.setName("example");
        return msg;
    }

    @Override
    public String getLogServerUuid() {
        return uuid;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APIUpdateLogServerMsg) msg).getUuid(), LogServerVO.class);
    }
}
