package org.zstack.sso.header;

import org.zstack.header.log.NoLogging;
import org.zstack.header.rest.RestRequest;
import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;

import static org.zstack.sso.oauth2.OAuth2Constants.OIDC_GET_USERNAME;

/**
 * @Author: DaoDao
 * @Date: 2022/8/23
 */
@RestRequest(
        path = "/create/oauth2/client",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateOAuthClientEvent.class
)
public class APICreateOAuthClientMsg extends APICreateMessage implements APIAuditor  {
    @APIParam
    private String name;
    @APIParam(required = false)
    private String description;
    @APIParam
    private String clientId;
    @APIParam(required = false)
    @NoLogging
    private String clientSecret;
    @APIParam(required = false)
    private String authorizationUrl;
    @APIParam
    private String tokenUrl;
    @APIParam(required = false)
    private String userinfoUrl;
    @APIParam(required = false)
    private String redirectUrl;
    @APIParam(required = false)
    private String logoutUrl;
    @APIParam
    private String grantType;
    @APIParam
    private String urlTemplate;
    @APIParam(required = false)
    private String usernameProperty = OIDC_GET_USERNAME;

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

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getUrlTemplate() {
        return urlTemplate;
    }

    public void setUrlTemplate(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    public String getUserinfoUrl() {
        return userinfoUrl;
    }

    public void setUserinfoUrl(String userinfoUrl) {
        this.userinfoUrl = userinfoUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
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
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateOAuthClientEvent)rsp).getInventory().getUuid() : "", OAuth2ClientVO.class);
    }

    public static APICreateOAuthClientMsg __example__() {
        APICreateOAuthClientMsg msg = new APICreateOAuthClientMsg();
        msg.setClientId(uuid());
        msg.setClientSecret(uuid());
        msg.setAuthorizationUrl("http://zstack.com/code");
        msg.setTokenUrl("http://zstack.com/token");
        msg.setDescription("test");
        msg.setName("test");
        msg.setUrlTemplate("http://172.24.194.28:5000/oauth1/verify/?username=${username}&sessionId=${sessionId}&userUuid=${userUuid}&accountUuid=${accountUuid}&loginType=${loginType}&userType=${userType}'");
        msg.setRedirectUrl("http://zstack.com/redirectUrl");
        msg.setUserinfoUrl("http://zstack.com/userinfoUrl");
        msg.setLogoutUrl("http://zstack.com/logoutUrl");
        return msg;
    }
}
