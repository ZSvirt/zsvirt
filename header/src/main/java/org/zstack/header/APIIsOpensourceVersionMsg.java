package org.zstack.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2017/5/17.
 */
@RestRequest(
        path = "/meta-data/opensource",
        method = HttpMethod.GET,
        responseClass = APIIsOpensourceVersionReply.class
)
@SuppressCredentialCheck
public class APIIsOpensourceVersionMsg extends APISyncCallMessage {
    public static APIIsOpensourceVersionMsg __example__() {
        return new APIIsOpensourceVersionMsg();
    }
}
