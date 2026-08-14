package org.zstack.sso.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * @Author: DaoDao
 * @Date: 2022/8/24
 */
@RestRequest(
        path = "/create/cas/client",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateCasClientEvent.class
)
public class APICreateCasClientMsg extends APICreateMessage implements APIAuditor {
    @APIParam
    private String name;
    @APIParam(required = false)
    private String description;
    @APIParam
    private String casServerLoginUrl;
    @APIParam
    private String casServerUrlPrefix;
    @APIParam
    private String serverName;
    @APIParam(required = false)
    private String usernameProperty = "username";
    @APIParam(required = false)
    private String urlTemplate;

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

    public String getCasServerLoginUrl() {
        return casServerLoginUrl;
    }

    public void setCasServerLoginUrl(String casServerLoginUrl) {
        this.casServerLoginUrl = casServerLoginUrl;
    }

    public String getCasServerUrlPrefix() {
        return casServerUrlPrefix;
    }

    public void setCasServerUrlPrefix(String casServerUrlPrefix) {
        this.casServerUrlPrefix = casServerUrlPrefix;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getUsernameProperty() {
        return usernameProperty;
    }

    public void setUsernameProperty(String usernameProperty) {
        this.usernameProperty = usernameProperty;
    }

    public String getUrlTemplate() {
        return urlTemplate;
    }

    public void setUrlTemplate(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateCasClientEvent)rsp).getInventory().getUuid() : "", CasClientVO.class);
    }

    public static APICreateCasClientMsg __example__() {
        APICreateCasClientMsg msg = new APICreateCasClientMsg();
        msg.setCasServerLoginUrl("http://zstack.com/login");
        msg.setCasServerUrlPrefix("http://zstack.com");
        msg.setServerName("http://127.0.0.1:8080");
        msg.setDescription("test");
        msg.setName("test");
        msg.setUrlTemplate("http://172.24.194.28:5000/oauth1/verify/?username=${username}&sessionId=${sessionId}&userUuid=${userUuid}&accountUuid=${accountUuid}&loginType=${loginType}&userType=${userType}'");
        return msg;
    }
}
