package org.zstack.sso.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * @Author: DaoDao
 * @Date: 2023/2/3
 */
@RestRequest(
        path = "/get/oauth2/token",
        method = HttpMethod.GET,
        responseClass = APIGetOAuth2TokenReply.class
)
public class APIGetOAuth2TokenMsg extends APISyncCallMessage {

    public static APIGetOAuth2TokenMsg __example__() {
        APIGetOAuth2TokenMsg msg = new APIGetOAuth2TokenMsg();
        return msg;
    }
}
