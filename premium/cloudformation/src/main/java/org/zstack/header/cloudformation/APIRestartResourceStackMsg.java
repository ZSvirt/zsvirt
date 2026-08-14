package org.zstack.header.cloudformation;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/6/26.
 */
@RestRequest(
        path = "/cloudformation/stack/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIRestartResourceStackEvent.class,
        isAction = true
)
public class APIRestartResourceStackMsg extends APIMessage {
    @APIParam(resourceType = ResourceStackVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIRestartResourceStackMsg __example__() {
        APIRestartResourceStackMsg msg = new APIRestartResourceStackMsg();

        msg.setUuid(uuid());
        return msg;
    }
}
