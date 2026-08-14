package org.zstack.header.longjob;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by wushan on 8/23/21
 **/
@RestRequest(
        path = "/longjobs/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APICleanLongJobEvent.class
)
public class APICleanLongJobMsg extends APIMessage implements LongJobMessage {
    @APIParam(resourceType = LongJobVO.class, scope = APIParam.SCOPE_MUST_OWNER)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getLongJobUuid() {
        return uuid;
    }

    public static APICleanLongJobMsg __example__() {
        APICleanLongJobMsg msg = new APICleanLongJobMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
