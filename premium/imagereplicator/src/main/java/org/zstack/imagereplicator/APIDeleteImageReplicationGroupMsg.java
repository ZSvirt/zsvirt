package org.zstack.imagereplicator;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/image-replication-groups/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteImageReplicationGroupEvent.class
)
public class APIDeleteImageReplicationGroupMsg extends APIDeleteMessage {
    @APIParam(resourceType = ImageReplicationGroupVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public APIDeleteImageReplicationGroupMsg() {
        super();
    }

    public APIDeleteImageReplicationGroupMsg(String uuid) {
        super();
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteImageReplicationGroupMsg __example__() {
        APIDeleteImageReplicationGroupMsg msg = new APIDeleteImageReplicationGroupMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
