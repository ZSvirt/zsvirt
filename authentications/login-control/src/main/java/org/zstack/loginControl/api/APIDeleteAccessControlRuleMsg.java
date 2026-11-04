package org.zstack.loginControl.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.loginControl.entity.AccessControlRuleVO;

@RestRequest(
        path = "/login-control/access-control/rules/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteAccessControlRuleEvent.class
)
public class APIDeleteAccessControlRuleMsg extends APIDeleteMessage {
    @APIParam(resourceType = AccessControlRuleVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteAccessControlRuleMsg __example__() {
        APIDeleteAccessControlRuleMsg msg = new APIDeleteAccessControlRuleMsg();
        msg.setUuid(uuid(AccessControlRuleVO.class));
        return msg;
    }
}
