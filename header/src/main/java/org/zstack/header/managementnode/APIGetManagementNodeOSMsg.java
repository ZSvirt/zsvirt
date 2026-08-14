package org.zstack.header.managementnode;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by Jialong on 2021/03/15.
 */

@RestRequest(
        path = "/management/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIGetManagementNodeOSReply.class
)
@SuppressCredentialCheck
public class APIGetManagementNodeOSMsg extends APISyncCallMessage implements APIManagementNodeMessage {

    public static APIGetManagementNodeOSMsg __example__() {
        APIGetManagementNodeOSMsg msg = new APIGetManagementNodeOSMsg();
        return msg;
    }
}
