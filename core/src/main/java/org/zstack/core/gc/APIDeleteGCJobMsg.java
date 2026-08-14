package org.zstack.core.gc;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2017/3/5.
 */
@RestRequest(
        path = "/gc-jobs/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteGCJobEvent.class
)
public class APIDeleteGCJobMsg extends APIMessage implements GarbageCollectorMessage {
    @APIParam
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getGCJobUuid() {
        return uuid;
    }

    public static APIDeleteGCJobMsg __example__() {
        APIDeleteGCJobMsg msg = new APIDeleteGCJobMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
