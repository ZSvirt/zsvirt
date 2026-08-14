package org.zstack.crypto.keyprovider.nkp.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/key-providers/nkp/actions",
        method = HttpMethod.PUT,
        responseClass = APIParseNkpRestoreEvent.class,
        isAction = true
)
public class APIParseNkpRestoreMsg extends APINkpRestoreMsg {
    public static APIParseNkpRestoreMsg __example__() {
        APIParseNkpRestoreMsg msg = new APIParseNkpRestoreMsg();
        msg.setContentBase64("BASE64_ENCODED_NKP_BACKUP");
        msg.setPassword("password");
        return msg;
    }
}
