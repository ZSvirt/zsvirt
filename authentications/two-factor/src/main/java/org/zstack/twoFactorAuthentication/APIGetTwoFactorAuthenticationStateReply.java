package org.zstack.twoFactorAuthentication;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "state")
public class APIGetTwoFactorAuthenticationStateReply extends APIReply {
    private String state;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public static APIGetTwoFactorAuthenticationStateReply __example__() {
        APIGetTwoFactorAuthenticationStateReply reply = new APIGetTwoFactorAuthenticationStateReply();
        reply.setState(TwoFactorAuthenticationState.Disable.toString());
        return reply;
    }

}
