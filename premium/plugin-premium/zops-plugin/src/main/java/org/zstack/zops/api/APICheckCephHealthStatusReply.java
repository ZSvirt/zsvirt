package org.zstack.zops.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APICheckCephHealthStatusReply extends APIReply {

    public static APICheckCephHealthStatusReply __example__() {
        APICheckCephHealthStatusReply reply = new APICheckCephHealthStatusReply();
        reply.setSuccess(true);
        return reply;
    }
}
