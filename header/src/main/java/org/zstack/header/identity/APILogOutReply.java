package org.zstack.header.identity;

import org.zstack.header.rest.RestResponse;

@RestResponse
public class APILogOutReply extends APILogInReply {

    public static APILogOutReply __example__() {
        APILogOutReply reply = new APILogOutReply();
        return reply;
    }
}
