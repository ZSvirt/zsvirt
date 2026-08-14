package org.zstack.sso.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.identity.imports.message.AccountSourceMessage;

/**
 * @Author: DaoDao
 * @Date: 2022/8/24
 */
@RestRequest(
        path = "/update/cas/client",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIUpdateCasClientEvent.class
)
public class APIUpdateCasClientMsg extends APIMessage implements AccountSourceMessage {
    @APIParam(resourceType = CasClientVO.class)
    private String uuid ;
    @APIParam(required = false)
    private String description;
    @APIParam(required = false)
    private String name;
    @APIParam(required = false)
    private String casServerLoginUrl;
    @APIParam(required = false)
    private String casServerUrlPrefix;
    @APIParam(required = false)
    private String serverName;
    @APIParam(required = false)
    private String usernameProperty;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    @Override
    public String getSourceUuid() {
        return getUuid();
    }

    public static APIUpdateCasClientMsg __example__() {
        APIUpdateCasClientMsg msg = new APIUpdateCasClientMsg();
        msg.setUuid(uuid());
        msg.setCasServerLoginUrl("http://zstack.com/login");
        msg.setCasServerUrlPrefix("http://zstack.com");
        msg.setServerName("http://127.0.0.1:8080");
        msg.setDescription("test");
        msg.setName("test");
        return msg;
    }
}
