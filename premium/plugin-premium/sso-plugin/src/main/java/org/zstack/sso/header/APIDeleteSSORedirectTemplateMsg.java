package org.zstack.sso.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.identity.imports.message.AccountSourceMessage;

/**
 * @Author: DaoDao
 * @Date: 2022/9/6
 */
@RestRequest(
        path = "/delete/sso/redirect/template",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIDeleteSSORedirectTemplateEvent.class
)
public class APIDeleteSSORedirectTemplateMsg extends APIDeleteMessage implements AccountSourceMessage {
    @APIParam(resourceType = SSORedirectTemplateVO.class)
    private String uuid;

    @APINoSee
    private String accountSourceUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public static APIDeleteSSORedirectTemplateMsg __example__() {
        APIDeleteSSORedirectTemplateMsg msg = new APIDeleteSSORedirectTemplateMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
