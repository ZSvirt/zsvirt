package org.zstack.header.cbt;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

@RestRequest(
        path = "/cbt-task/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteCbtTaskEvent.class
)
@TagResourceType(CbtTaskVO.class)
public class APIDeleteCbtTaskMsg extends APIDeleteMessage implements CbtTaskMessage {
    @APIParam(required = true)
    private String uuid;

    @APIParam(required = false)
    private boolean force = false;

    public APIDeleteCbtTaskMsg() {
    }

    public APIDeleteCbtTaskMsg(String uuid) {
        this.uuid = uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public static APIDeleteCbtTaskMsg __example__() {
        APIDeleteCbtTaskMsg msg = new APIDeleteCbtTaskMsg();

        msg.setUuid(uuid());

        return msg;
    }

    @Override
    public String getCbtTaskUuid() {
        return uuid;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }
}
