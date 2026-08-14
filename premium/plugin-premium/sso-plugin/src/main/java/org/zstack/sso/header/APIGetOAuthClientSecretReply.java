package org.zstack.sso.header;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.io.Serializable;

@RestResponse(fieldsTo = {"clientSecret=clientSecret"})
public class APIGetOAuthClientSecretReply extends APIReply implements Serializable {
    private String clientSecret;

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public static APIGetOAuthClientSecretReply __example__() {
        APIGetOAuthClientSecretReply reply = new APIGetOAuthClientSecretReply();
        reply.clientSecret = "exampleClientSecret";
        return reply;
    }
}
