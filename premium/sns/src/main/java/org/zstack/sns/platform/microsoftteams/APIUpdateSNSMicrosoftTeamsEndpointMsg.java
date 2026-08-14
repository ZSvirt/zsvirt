package org.zstack.sns.platform.microsoftteams;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.APIUpdateSNSApplicationEndpointEvent;
import org.zstack.sns.APIUpdateSNSApplicationEndpointMsg;

@RestRequest(
        path = "/sns/application-endpoints/microsoft-teams/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateSNSApplicationEndpointEvent.class,
        isAction = true
)
public class APIUpdateSNSMicrosoftTeamsEndpointMsg extends APIUpdateSNSApplicationEndpointMsg {

    @APIParam(maxLength = 256, required = false)
    private String url;

    public static APIUpdateSNSApplicationEndpointMsg __example__() {
        APIUpdateSNSMicrosoftTeamsEndpointMsg msg = new APIUpdateSNSMicrosoftTeamsEndpointMsg();
        msg.setUuid(uuid());
        msg.setName("new name");
        msg.setDescription("desc");
        msg.setUrl("https://open.microsoft-teams.cn/open-apis/bot/v2/hook/006879a3-0898-4428-aad4-3221db3daf81");
        return msg;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
