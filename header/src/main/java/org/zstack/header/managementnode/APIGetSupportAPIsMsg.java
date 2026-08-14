package org.zstack.header.managementnode;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@SuppressCredentialCheck
@RestRequest(
        path = "/management-nodes/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIGetSupportAPIsReply.class
)
public class APIGetSupportAPIsMsg extends APISyncCallMessage implements APIManagementNodeMessage {

    public static APIGetSupportAPIsMsg __example__() {
        return new APIGetSupportAPIsMsg();
    }
}
