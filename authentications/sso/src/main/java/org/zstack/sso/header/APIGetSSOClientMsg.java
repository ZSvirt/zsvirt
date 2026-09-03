package org.zstack.sso.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * @Author: DaoDao
 * @Date: 2023/2/3
 */
@RestRequest(
        path = "/get/sso/client",
        method = HttpMethod.GET,
        responseClass = APIGetSSOClientReply.class
)
@SuppressCredentialCheck
public class APIGetSSOClientMsg extends APISyncCallMessage {
    public static APIGetSSOClientMsg __example__() {
        APIGetSSOClientMsg msg = new APIGetSSOClientMsg();
        return msg;
    }
}
