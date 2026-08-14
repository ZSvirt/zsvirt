package org.zstack.iam1.api.accounts;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.iam1.entity.accounts.AccountGroupVO;

import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/account-groups/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateAccountGroupEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.MINUTES, value = 1)
public class APIUpdateAccountGroupMsg extends APIMessage implements AccountGroupMessage {
    @APIParam(resourceType = AccountGroupVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;

    @Override
    public String getAccountGroupUuid() {
        return getUuid();
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static APIUpdateAccountGroupMsg __example__() {
        APIUpdateAccountGroupMsg msg = new APIUpdateAccountGroupMsg();
        msg.setUuid(uuid());
        msg.setName("my-group");
        return msg;
    }
}
