package org.zstack.header.managementnode;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;


/**
 * Created by Jialong on 2021/03/15.
 */

@RestRequest(
        path = "/management-nodes/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIGetManagementNodeArchReply.class
)
@SuppressCredentialCheck
public class APIGetManagementNodeArchMsg extends APISyncCallMessage implements APIManagementNodeMessage {

    public static APIGetManagementNodeArchMsg __example__() {
        APIGetManagementNodeArchMsg msg = new APIGetManagementNodeArchMsg();
        return msg;
    }
}
