package org.zstack.sns.platform.dingtalk;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.SNSApplicationEndpointTestConnectionMessage;
import org.zstack.sns.SNSApplicationEndpointType;

import java.util.Arrays;
import java.util.List;

@RestRequest(path = "/sns/application-endpoints/ding-talk/test-connection",
        method = HttpMethod.POST,
        responseClass = APISNSDingTalkTestConnectionEvent.class,
        parameterName = "params")
public class APISNSDingTalkTestConnectionMsg extends APIMessage implements SNSApplicationEndpointTestConnectionMessage {
    @APIParam(maxLength = 2048, required = false)
    private String url;
    @APIParam(required = false)
    private Boolean atAll;
    @APIParam(required = false)
    private List<String> atPersonPhoneNumbers;
    @APIParam(required = false)
    private String secret;
    @APIParam
    private String testMsg;
    @APIParam(required = false, resourceType = SNSDingTalkEndpointVO.class)
    private String endpointUuid;
    public static APISNSDingTalkTestConnectionMsg __example__() {
        APISNSDingTalkTestConnectionMsg msg = new APISNSDingTalkTestConnectionMsg();
        msg.setUrl("https://open.feishu.cn/open-apis/bot/v2/hook/006879a3-0898-4428-aad4-3221db3daf81");
        msg.setAtAll(true);
        msg.setAtPersonPhoneNumbers(Arrays.asList("13062689903", "13062689901"));
        msg.setSecret("SECf8310c22767fe6f7cc5c00c6c4b1343605c445e9adaa28e39138749127a1c962");
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

    public List<String> getAtPersonPhoneNumbers() {
        return atPersonPhoneNumbers;
    }

    public void setAtPersonPhoneNumbers(List<String> atPersonPhoneNumbers) {
        this.atPersonPhoneNumbers = atPersonPhoneNumbers;
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
        return SNSDingTalkEndpointFactory.type;
    }
}
