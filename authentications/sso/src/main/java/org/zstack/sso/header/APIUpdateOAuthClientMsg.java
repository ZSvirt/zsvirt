package org.zstack.sso.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.identity.imports.message.AccountSourceMessage;

/**
 * @Author: DaoDao
 * @Date: 2022/8/23
 */
@RestRequest(
        path = "/update/oauth2/client",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIUpdateOAuthClientEvent.class
)
public class APIUpdateOAuthClientMsg extends APIMessage implements AccountSourceMessage {
    @APIParam(resourceType = OAuth2ClientVO.class)
    private String uuid ;
    @APIParam(required = false)
    private String name;
    @APIParam(required = false)
    private String description;
    @APIParam(required = false)
    private String clientId;
    @APIParam(required = false)
    private String clientSecret;
    @APIParam(required = false)
    private String authorizationUrl;
    @APIParam(required = false)
    private String tokenUrl;
    @APIParam(required = false)
    private String redirectUrl;
    @APIParam(required = false)
    private String userinfoUrl;
    @APIParam(required = false)
    private String logoutUrl;
    @APIParam(required = false)
    private String usernameProperty;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
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

    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    public void setAuthorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getUserinfoUrl() {
        return userinfoUrl;
    }

    public void setUserinfoUrl(String userinfoUrl) {
        this.userinfoUrl = userinfoUrl;
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public void setLogoutUrl(String logoutUrl) {
        this.logoutUrl = logoutUrl;
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

    public static APIUpdateOAuthClientMsg __example__() {
        APIUpdateOAuthClientMsg msg = new APIUpdateOAuthClientMsg();
        msg.setClientId(uuid());
        msg.setClientSecret(uuid());
        msg.setAuthorizationUrl("http://zstack.com/code");
        msg.setTokenUrl("http://zstack.com/token");
        msg.setDescription("test");
        msg.setName("test");
        msg.setRedirectUrl("http://zstack.com/redirectUrl");
        msg.setUserinfoUrl("http://zstack.com/userinfoUrl");
        msg.setLogoutUrl("http://zstack.com/logouturl");
        return msg;
    }
}

