package org.zstack.sns.platform.microsoftteams;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.APICreateSNSApplicationEndpointMsg;
import org.zstack.sns.SNSApplicationPlatformMessage;
import org.zstack.sns.SNSConstants;

@RestRequest(path = "/sns/application-endpoints/microsoft-teams",
        method = HttpMethod.POST,
        responseClass = APICreateSNSMicrosoftTeamsEndpointEvent.class,
        parameterName = "params")

public class APICreateSNSMicrosoftTeamsEndpointMsg extends APICreateSNSApplicationEndpointMsg implements SNSApplicationPlatformMessage {
    @APIParam(maxLength = 2048)
    private String url;


    public static APICreateSNSMicrosoftTeamsEndpointMsg __example__() {
        APICreateSNSMicrosoftTeamsEndpointMsg msg = new APICreateSNSMicrosoftTeamsEndpointMsg();
        msg.setName("Microsofo teams");
        msg.setUrl("http://teams-robot-url");
        return msg;
    }

    @Override
    public String getPlatformUuid() {
        return SNSConstants.SYSTEM_PLATFORM_UUID;
    }

    @Override
    public String getApplicationPlatformUuid() {
        return getPlatformUuid();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String getApplicationEndpointType() {
        return SNSMicrosoftTeamsEndpointFactory.type.toString();
    }
}
