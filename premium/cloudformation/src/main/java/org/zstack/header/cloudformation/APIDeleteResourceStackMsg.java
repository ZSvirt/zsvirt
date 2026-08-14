package org.zstack.header.cloudformation;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/6/11.
 */
@RestRequest(
        path = "/cloudformation/stack/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteResourceStackEvent.class
)
public class APIDeleteResourceStackMsg extends APIDeleteMessage {
    @APIParam(resourceType = ResourceStackVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteResourceStackMsg __example__() {
        APIDeleteResourceStackMsg msg = new APIDeleteResourceStackMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
