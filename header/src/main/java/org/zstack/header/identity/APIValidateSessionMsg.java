package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:38 PM
 * To change this template use File | Settings | File Templates.
 */
@SuppressCredentialCheck
@RestRequest(
        path = "/accounts/sessions/{sessionUuid}/valid",
        responseClass = APIValidateSessionReply.class,
        method = HttpMethod.GET
)
public class APIValidateSessionMsg extends APISessionMessage {
    @APIParam
    private String sessionUuid;

    public String getSessionUuid() {
        return sessionUuid;
    }

    public void setSessionUuid(String sessionUuid) {
        this.sessionUuid = sessionUuid;
    }
 
    public static APIValidateSessionMsg __example__() {
        APIValidateSessionMsg msg = new APIValidateSessionMsg();
        msg.setSessionUuid(uuid());
        return msg;
    }

    @Override
    public String getUsername() {
        return null;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getLoginType() {
        return null;
    }
}
