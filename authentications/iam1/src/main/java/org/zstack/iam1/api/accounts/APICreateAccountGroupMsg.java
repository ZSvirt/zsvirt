package org.zstack.iam1.api.accounts;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import org.zstack.iam1.entity.accounts.AccountGroupVO;

import java.util.concurrent.TimeUnit;

@TagResourceType(AccountGroupVO.class)
@RestRequest(
        path = "/account-groups",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateAccountGroupEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.MINUTES, value = 1)
public class APICreateAccountGroupMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255, nonempty = true)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(resourceType = AccountGroupVO.class, required = false)
    private String parentUuid;

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

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public static APICreateAccountGroupMsg __example__() {
        APICreateAccountGroupMsg msg = new APICreateAccountGroupMsg();
        msg.setName("my-group");
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ?
                ((APICreateAccountGroupEvent) rsp).getInventory().getUuid() : "", AccountGroupVO.class);
    }
}
