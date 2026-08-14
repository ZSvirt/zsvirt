package org.zstack.sso.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;
import org.zstack.identity.imports.message.AccountSourceMessage;

/**
 * @Author: DaoDao
 * @Date: 2022/9/6
 */
@RestRequest(
        path = "/create/sso/redirect/template/",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateSSORedirectTemplateEvent.class
)
public class APICreateSSORedirectTemplateMsg extends APICreateMessage implements APIAuditor, AccountSourceMessage {
    @APIParam
    private String name;
    @APIParam
    private String description;
    @APIParam(resourceType = ThirdPartyAccountSourceVO.class)
    private String clientUuid;
    @APIParam
    private String redirectTemplate;

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

    public String getClientUuid() {
        return clientUuid;
    }

    public void setClientUuid(String clientUuid) {
        this.clientUuid = clientUuid;
    }

    public String getRedirectTemplate() {
        return redirectTemplate;
    }

    public void setRedirectTemplate(String redirectTemplate) {
        this.redirectTemplate = redirectTemplate;
    }

    @Override
    public String getSourceUuid() {
        return getClientUuid();
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateSSORedirectTemplateEvent)rsp).getInventory().getUuid() : "", SSORedirectTemplateVO.class);
    }

    public static APICreateSSORedirectTemplateMsg __example__() {
        APICreateSSORedirectTemplateMsg msg = new APICreateSSORedirectTemplateMsg();
        msg.setName("test");
        msg.setDescription("desc");
        msg.setClientUuid(uuid());
        msg.setRedirectTemplate("http://172.24.194.28:5000/oauth1/verify/?username=${username}&sessionId=${sessionId}&userUuid=${userUuid}&accountUuid=${accountUuid}&loginType=${loginType}&userType=${userType}'");
        return msg;
    }
}
