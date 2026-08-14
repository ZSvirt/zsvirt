package org.zstack.sso.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.identity.imports.message.AccountSourceMessage;

/**
 * @Author: DaoDao
 * @Date: 2023/7/14
 */
@RestRequest(
        path = "/update/sso/redirectTemplate",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIUpdateSSORedirectTemplateEvent.class
)
public class APIUpdateSSORedirectTemplateMsg extends APIMessage implements AccountSourceMessage {
    @APIParam(resourceType = SSORedirectTemplateVO.class)
    private String uuid;

    @APIParam
    private String redirectTemplate;

    @APINoSee
    private String accountSourceUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getRedirectTemplate() {
        return redirectTemplate;
    }

    public void setRedirectTemplate(String redirectTemplate) {
        this.redirectTemplate = redirectTemplate;
    }

    public String getAccountSourceUuid() {
        return accountSourceUuid;
    }

    public void setAccountSourceUuid(String accountSourceUuid) {
        this.accountSourceUuid = accountSourceUuid;
    }

    @Override
    public String getSourceUuid() {
        return getAccountSourceUuid();
    }

    public static APIUpdateSSORedirectTemplateMsg __example__() {
        APIUpdateSSORedirectTemplateMsg msg = new APIUpdateSSORedirectTemplateMsg();
        msg.setUuid(uuid());
        msg.setRedirectTemplate("http://zstack.com/userinfoUrl");
        return msg;
    }
}
