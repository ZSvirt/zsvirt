package org.zstack.header.core.progress;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2017/3/21.
 */
@RestRequest(
        path = "/task-progresses/{apiId}",
        method = HttpMethod.GET,
        responseClass = APIGetTaskProgressReply.class
)
public class APIGetTaskProgressMsg extends APISyncCallMessage {
    private String apiId;

    @Deprecated
    private boolean all;

    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public static APIGetTaskProgressMsg __example__() {
        APIGetTaskProgressMsg msg = new APIGetTaskProgressMsg();
        msg.setApiId(uuid());
        return msg;
    }
}
