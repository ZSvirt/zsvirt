package org.zstack.sso.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/oauth2/clients/{uuid}/client-secret",
        method = HttpMethod.GET,
        responseClass = APIGetOAuthClientSecretReply.class
)
public class APIGetOAuthClientSecretMsg extends APISyncCallMessage {
    @APIParam(resourceType = OAuth2ClientVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIGetOAuthClientSecretMsg __example__() {
        APIGetOAuthClientSecretMsg msg = new APIGetOAuthClientSecretMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
