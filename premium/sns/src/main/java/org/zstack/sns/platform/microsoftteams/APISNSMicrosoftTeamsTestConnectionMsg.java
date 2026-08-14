package org.zstack.sns.platform.microsoftteams;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.SNSApplicationEndpointTestConnectionMessage;
import org.zstack.sns.SNSApplicationEndpointType;

@RestRequest(path = "/sns/application-endpoints/microsoft-teams/test-connection",
        method = HttpMethod.POST,
        responseClass = APISNSMicrosoftTeamsTestConnectionEvent.class,
        parameterName = "params")
public class APISNSMicrosoftTeamsTestConnectionMsg extends APIMessage implements SNSApplicationEndpointTestConnectionMessage {
    @APIParam(maxLength = 2048, required = false)
    private String url;
    @APIParam
    private String testMsg;
    @APIParam(resourceType = SNSMicrosoftTeamsEndpointVO.class,required = false)
    private String endpointUuid;

    public static APISNSMicrosoftTeamsTestConnectionMsg __example__() {
        APISNSMicrosoftTeamsTestConnectionMsg msg = new APISNSMicrosoftTeamsTestConnectionMsg();
        msg.setUrl("https://microsoft.com/cgi-bin/webhook/send?key=f8b9014a-207a-44d5-ae26-3501bf01dbc4");
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
        return SNSMicrosoftTeamsEndpointFactory.type;
    }
}
