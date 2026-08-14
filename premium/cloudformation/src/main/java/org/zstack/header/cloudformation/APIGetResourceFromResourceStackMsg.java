package org.zstack.header.cloudformation;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/6/12.
 */
@RestRequest(
        path = "/cloudformation/stack/resources",
        method = HttpMethod.GET,
        responseClass = APIGetResourceFromResourceStackReply.class
)
public class APIGetResourceFromResourceStackMsg extends APISyncCallMessage {
    @APIParam(resourceType = ResourceStackVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIGetResourceFromResourceStackMsg __example__() {
        APIGetResourceFromResourceStackMsg msg = new APIGetResourceFromResourceStackMsg();

        msg.setUuid(uuid());

        return msg;
    }
}
