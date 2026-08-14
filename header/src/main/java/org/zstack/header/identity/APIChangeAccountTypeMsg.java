package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(category = AccountConstant.ACTION_CATEGORY, adminOnly = true)
@RestRequest(
        path = "/accounts/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIChangeAccountTypeEvent.class
)
public class APIChangeAccountTypeMsg extends APIMessage implements AccountMessage {
    @APIParam(resourceType = AccountVO.class)
    private String uuid;

    @APIParam
    private String type;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }


    @Override
    public String getAccountUuid() {
        return this.getSession().getAccountUuid();
    }

    public static APIChangeAccountTypeMsg __example__() {
        APIChangeAccountTypeMsg msg = new APIChangeAccountTypeMsg();
        msg.setUuid(uuid());
        msg.setType(AccountType.SystemAdmin.toString());
        return msg;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
