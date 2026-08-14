package org.zstack.header.cloudformation;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2020/3/26.
 */
@RestRequest(
        path = "/cloudformation/resources/stack",
        method = HttpMethod.GET,
        responseClass = APIGetResourceStackFromResourceReply.class
)
public class APIGetResourceStackFromResourceMsg extends APISyncCallMessage {
    @APIParam()
    private String resourceUuid;

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public static APIGetResourceStackFromResourceMsg __example__() {
        APIGetResourceStackFromResourceMsg msg = new APIGetResourceStackFromResourceMsg();

        msg.setResourceUuid(uuid());

        return msg;
    }
}
