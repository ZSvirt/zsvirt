package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/spice/certificates",
        method = HttpMethod.GET,
        responseClass = APIGetSpiceCertificatesReply.class
)
public class APIGetSpiceCertificatesMsg extends APISyncCallMessage {

    public static APIGetSpiceCertificatesMsg __example__() {
        APIGetSpiceCertificatesMsg msg = new APIGetSpiceCertificatesMsg();
        return msg;
    }
}
