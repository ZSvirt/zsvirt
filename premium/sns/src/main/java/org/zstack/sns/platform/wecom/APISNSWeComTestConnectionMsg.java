package org.zstack.sns.platform.wecom;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.SNSApplicationEndpointTestConnectionMessage;
import org.zstack.sns.SNSApplicationEndpointType;

import java.util.Arrays;
import java.util.List;

@RestRequest(path = "/sns/application-endpoints/we-com/test-connection",
        method = HttpMethod.POST,
        responseClass = APISNSWeComTestConnectionEvent.class,
        parameterName = "params")
public class APISNSWeComTestConnectionMsg extends APIMessage implements SNSApplicationEndpointTestConnectionMessage {
    @APIParam(maxLength = 2048, required = false)
    private String url;
    @APIParam(required = false)
    private Boolean atAll;
    @APIParam(required = false)
    private List<String> atPersonUserIds;
    @APIParam
    private String testMsg;
    @APIParam(required = false, resourceType = SNSWeComEndpointVO.class)
    private String endpointUuid;

    public static APISNSWeComTestConnectionMsg __example__() {
        APISNSWeComTestConnectionMsg msg = new APISNSWeComTestConnectionMsg();
        msg.setUrl("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=f8b9014a-207a-44d5-ae26-3501bf01dbc4");
        msg.setAtAll(false);
        msg.setAtPersonUserIds(Arrays.asList("zhangsan", "lisi"));
        msg.setTestMsg("hello");
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
        return SNSWeComEndpointFactory.type;
    }
}
