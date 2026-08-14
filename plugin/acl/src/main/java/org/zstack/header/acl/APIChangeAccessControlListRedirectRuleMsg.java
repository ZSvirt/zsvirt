package org.zstack.header.acl;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/access-control-lists/redirectRules/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeAccessControlListRedirectRuleEvent.class,
        isAction = true
)
public class APIChangeAccessControlListRedirectRuleMsg extends APIMessage implements APIAuditor {
    @APIParam(resourceType = AccessControlListEntryVO.class)
    private String uuid;
    @APIParam(required = false, maxLength = 255)
    private String name;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return null;
    }

    public static APIChangeAccessControlListRedirectRuleMsg __example__() {
        APIChangeAccessControlListRedirectRuleMsg msg = new APIChangeAccessControlListRedirectRuleMsg();
        msg.setUuid(uuid(AccessControlListEntryVO.class));
        msg.setName("test-access-control");
        return msg;
    }
}
