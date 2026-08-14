package org.zstack.header.managementnode;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 11/14/2015.
 */
@SuppressCredentialCheck
@RestRequest(
        path = "/management-nodes/actions",
        isAction = true,
        responseClass = APIGetVersionReply.class,
        method = HttpMethod.PUT
)
public class APIGetVersionMsg extends APISyncCallMessage implements APIManagementNodeMessage {
 
    public static APIGetVersionMsg __example__() {
        APIGetVersionMsg msg = new APIGetVersionMsg();


        return msg;
    }

}
