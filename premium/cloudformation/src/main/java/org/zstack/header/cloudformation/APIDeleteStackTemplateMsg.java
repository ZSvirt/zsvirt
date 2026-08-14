package org.zstack.header.cloudformation;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/6/5.
 */
@RestRequest(
        path = "/cloudformation/template/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteStackTemplateEvent.class
)
public class APIDeleteStackTemplateMsg extends APIDeleteMessage {
    @APIParam(resourceType = StackTemplateVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteStackTemplateMsg __example__() {
        APIDeleteStackTemplateMsg msg = new APIDeleteStackTemplateMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
