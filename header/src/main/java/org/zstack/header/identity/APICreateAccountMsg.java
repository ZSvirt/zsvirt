package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.core.encrypt.EncryptionParamAllowed;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/accounts",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateAccountEvent.class
)
@EncryptionParamAllowed
public class APICreateAccountMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 255, password = true)
    @NoLogging
    private String password;
    @APIParam(validValues = {"SystemAdmin", "Normal"}, required = false)
    private String type;
    @APIParam(validEnums = {AccountState.class}, required = false)
    private String state = AccountState.Enabled.toString();
    @APIParam(maxLength = 2048, required = false)
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static APICreateAccountMsg __example__() {
        APICreateAccountMsg msg = new APICreateAccountMsg();
        msg.setName("test");
        msg.setPassword("password");
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        String uuid = "";
        if (rsp.isSuccess()) {
            APICreateAccountEvent evt = (APICreateAccountEvent) rsp;
            uuid = evt.getInventory().getUuid();
        }
        return new Result(uuid, AccountVO.class);
    }
}
