package org.zstack.imagereplicator;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/image-replication-groups",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateImageReplicationGroupEvent.class
)
public class APICreateImageReplicationGroupMsg extends APICreateMessage implements APIAuditor {
    /**
     * @desc max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String name;
    /**
     * @desc max length of 2048 characters
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;

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

    public static APICreateImageReplicationGroupMsg __example__() {
        APICreateImageReplicationGroupMsg msg = new APICreateImageReplicationGroupMsg();
        msg.setName("rep-group");
        msg.setDescription("test");
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateImageReplicationGroupEvent)rsp).getInventory().getUuid() : "",
                ImageReplicationGroupVO.class);
    }
}
