package org.zstack.sns.platform.feishu;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.SNSApplicationEndpointTestConnectionMessage;
import org.zstack.sns.SNSApplicationEndpointType;

import java.util.Arrays;
import java.util.List;

@RestRequest(path = "/sns/application-endpoints/feishu/test-connection",
        method = HttpMethod.POST,
        responseClass = APISNSFeiShuTestConnectionEvent.class,
        parameterName = "params")
public class APISNSFeiShuTestConnectionMsg extends APIMessage implements SNSApplicationEndpointTestConnectionMessage {
    @APIParam(maxLength = 2048, required = false)
    private String url;
    @APIParam(required = false)
    private Boolean atAll;
    @APIParam(required = false)
    private List<String> atPersonUserIds;
    @APIParam(required = false)
    private String secret;
    @APIParam
    private String testMsg;
    @APIParam(required = false, resourceType = SNSFeiShuEndpointVO.class)
    private String endpointUuid;
    public static APISNSFeiShuTestConnectionMsg __example__() {
        APISNSFeiShuTestConnectionMsg msg = new APISNSFeiShuTestConnectionMsg();
        msg.setUrl("https://open.feishu.cn/open-apis/bot/v2/hook/006879a3-0898-4428-aad4-3221db3daf81");
        msg.setAtAll(true);
        msg.setAtPersonUserIds(Arrays.asList("13062689903", "13062689901"));
        msg.setSecret("fiSmveXkeD2jIjrENHYjQd");
        msg.setTestMsg("hello world");
        msg.setEndpointUuid("1c201c27a81740ddadbc5d2f3f38a5e4");
        return msg;
    }
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Boolean getAtAll() {
        return atAll;
    }

    public void setAtAll(Boolean atAll) {
        this.atAll = atAll;
    }

    public List<String> getAtPersonUserIds() {
        return atPersonUserIds;
    }

    public void setAtPersonUserIds(List<String> atPersonUserIds) {
        this.atPersonUserIds = atPersonUserIds;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getTestMsg() {
        return testMsg;
    }

    public void setTestMsg(String testMsg) {
        this.testMsg = testMsg;
    }

    public String getEndpointUuid() {
        return endpointUuid;
    }

    public void setEndpointUuid(String endpointUuid) {
        this.endpointUuid = endpointUuid;
    }

    @Override
    public SNSApplicationEndpointType getEndpointType() {
        return SNSFeiShuEndpointFactory.type;
    }
}
