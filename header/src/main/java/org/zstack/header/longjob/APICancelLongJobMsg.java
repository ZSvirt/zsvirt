package org.zstack.header.longjob;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 11/13/17.
 */
@RestRequest(
        path = "/longjobs/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APICancelLongJobEvent.class
)
public class APICancelLongJobMsg extends APIMessage implements LongJobMessage {
    @APIParam(resourceType = LongJobVO.class, scope = APIParam.SCOPE_MUST_OWNER)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APICancelLongJobMsg __example__() {
        APICancelLongJobMsg msg = new APICancelLongJobMsg();
        msg.setUuid(uuid());
        return msg;
    }

    @Override
    public String getLongJobUuid() {
        return uuid;
    }
}
