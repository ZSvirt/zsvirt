package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:38 PM
 * To change this template use File | Settings | File Templates.
 */
@RestRequest(
        path = "/l3-networks/types",
        method = HttpMethod.GET,
        responseClass = APIGetL3NetworkTypesReply.class
)
public class APIGetL3NetworkTypesMsg extends APISyncCallMessage {
 
    public static APIGetL3NetworkTypesMsg __example__() {
        APIGetL3NetworkTypesMsg msg = new APIGetL3NetworkTypesMsg();

        return msg;
    }

}
