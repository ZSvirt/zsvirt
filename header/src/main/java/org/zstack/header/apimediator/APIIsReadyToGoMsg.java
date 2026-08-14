package org.zstack.header.apimediator;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.managementnode.APIManagementNodeMessage;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@SuppressCredentialCheck
@RestRequest(
        path = "/management-nodes/ready",
        method = HttpMethod.GET,
        responseClass = APIIsReadyToGoReply.class,
        category = "other"
)
public class APIIsReadyToGoMsg extends APISyncCallMessage implements APIManagementNodeMessage {
    private String managementNodeId;

    public String getManagementNodeId() {
        return managementNodeId;
    }

    public void setManagementNodeId(String managementNodeId) {
        this.managementNodeId = managementNodeId;
    }

    public APIIsReadyToGoMsg() {
    }
 
    public static APIIsReadyToGoMsg __example__() {
        APIIsReadyToGoMsg msg = new APIIsReadyToGoMsg();


        return msg;
    }

}
