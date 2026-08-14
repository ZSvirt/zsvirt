package org.zstack.core.errorcode;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/12/1.
 */
@RestRequest(
        path = "/errorcode/actions",
        isAction = true,
        responseClass = APIReloadElaborationEvent.class,
        method = HttpMethod.PUT
)
public class APIReloadElaborationMsg extends APIMessage {

    public static APIReloadElaborationMsg __example__() {
        return new APIReloadElaborationMsg();
    }
}
