package org.zstack.header.managementnode;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by Mei Lei <meilei007@gmail.com> on 11/1/16.
 */
@SuppressCredentialCheck(supportBacthQuery = true)
@RestRequest(
        path = "/management-nodes/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIGetCurrentTimeReply.class
)
public class APIGetCurrentTimeMsg extends APISyncCallMessage implements APIManagementNodeMessage {
 
    public static APIGetCurrentTimeMsg __example__() {
        APIGetCurrentTimeMsg msg = new APIGetCurrentTimeMsg();


        return msg;
    }

}
