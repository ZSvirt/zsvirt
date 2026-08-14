package org.zstack.header.identity;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpMethod;
import org.zstack.header.MapField;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APIReply;
import org.zstack.header.other.APILoginAuditor;
import org.zstack.header.rest.RestRequest;

import java.util.Map;

@SuppressCredentialCheck
@RestRequest(
        path = "/accounts/sessions/{sessionUuid}",
        method = HttpMethod.DELETE,
        responseClass = APILogOutReply.class
)
public class APILogOutMsg extends APISessionMessage implements APILoginAuditor {
    private String sessionUuid;

    @APIParam(required = false)
    @MapField(keyType = String.class, valueType = String.class)
    private Map<String, String> clientInfo;

    public String getSessionUuid() {
        return sessionUuid;
    }

    public void setSessionUuid(String sessionUuid) {
        this.sessionUuid = sessionUuid;
    }

    public Map<String, String> getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(Map<String, String> clientInfo) {
        this.clientInfo = clientInfo;
    }
 
    public static APILogOutMsg __example__() {
        APILogOutMsg msg = new APILogOutMsg();

        msg.setSessionUuid(uuid());
        return msg;
    }

    @Override
    public LoginResult loginAudit(APIMessage msg, APIReply reply) {
        String clientIp = "";
        String clientBrowser = "";
        APILogOutMsg amsg = (APILogOutMsg) msg;
        Map<String, String> clientInfo = amsg.getClientInfo();
        if (clientInfo != null && !clientInfo.isEmpty()) {
            clientIp = StringUtils.isNotEmpty(clientInfo.get("clientIp")) ? clientInfo.get("clientIp") : "";
            clientBrowser = StringUtils.isNotEmpty(clientInfo.get("clientBrowser")) ? clientInfo.get("clientBrowser") : "";
        } else {
            clientIp = StringUtils.isNotBlank(msg.getClientIp()) ? msg.getClientIp() : "";
            clientBrowser = StringUtils.isNotBlank(msg.getClientBrowser()) ? msg.getClientBrowser() : "";
        }
        return new LoginResult(clientIp, clientBrowser, amsg.getSessionUuid(), SessionVO.class);
    }

    @Override
    public String getUsername() {
        return null;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getLoginType() {
        return null;
    }
}
