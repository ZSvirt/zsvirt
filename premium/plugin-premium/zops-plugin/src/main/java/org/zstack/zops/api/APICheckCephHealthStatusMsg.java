package org.zstack.zops.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/zops/check-ceph-health",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICheckCephHealthStatusReply.class
)
public class APICheckCephHealthStatusMsg extends APISyncCallMessage {
    public static APICheckCephHealthStatusMsg __example__() {
        return new APICheckCephHealthStatusMsg();
    }
}
