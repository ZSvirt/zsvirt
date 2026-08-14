package org.zstack.sns.platform.http;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.SNSApplicationEndpointTestConnectionMessage;
import org.zstack.sns.SNSApplicationEndpointType;

@RestRequest(path = "/sns/application-endpoints/http/test-connection",
        method = HttpMethod.POST,
        responseClass = APISNSHttpTestConnectionEvent.class,
        parameterName = "params")
public class APISNSHttpTestConnectionMsg extends APIMessage implements SNSApplicationEndpointTestConnectionMessage {
    @APIParam(maxLength = 2048, required = false)
    @NoLogging(type = NoLogging.Type.Uri)
    private String url;
    @APIParam(required = false)
    private String username;
    @APIParam(required = false)
    @NoLogging
    private String password;
    @APIParam(required = false, resourceType = SNSHttpEndpointVO.class)
    private String endpointUuid;

    public static APISNSHttpTestConnectionMsg __example__() {
        APISNSHttpTestConnectionMsg msg = new APISNSHttpTestConnectionMsg();
        msg.setUrl("https://open.feishu.cn/open-apis/bot/v2/hook/006879a3-0898-4428-aad4-3221db3daf81");
        msg.setUsername("admin");
        msg.setPassword("password");
        msg.setEndpointUuid("1c201c27a81740ddadbc5d2f3f38a5e4");
        return msg;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEndpointUuid() {
        return endpointUuid;
    }

    public void setEndpointUuid(String endpointUuid) {
        this.endpointUuid = endpointUuid;
    }

    @Override
    public SNSApplicationEndpointType getEndpointType() {
        return SNSHttpEndpointFactory.type;
    }
}
