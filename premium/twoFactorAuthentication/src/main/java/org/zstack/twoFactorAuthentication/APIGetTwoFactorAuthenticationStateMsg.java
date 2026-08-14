package org.zstack.twoFactorAuthentication;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/twofactorauthentication/state",
        method = HttpMethod.GET,
        responseClass = APIGetTwoFactorAuthenticationStateReply.class
)
@SuppressCredentialCheck
public class APIGetTwoFactorAuthenticationStateMsg extends APISyncCallMessage {

    public static APIGetTwoFactorAuthenticationStateMsg __example__() {
        APIGetTwoFactorAuthenticationStateMsg msg = new APIGetTwoFactorAuthenticationStateMsg();

        return msg;
    }
}