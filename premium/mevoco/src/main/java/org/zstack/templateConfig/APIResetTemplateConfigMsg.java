package org.zstack.templateConfig;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/template-configurations/{templateUuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIResetTemplateConfigEvent.class
)
public class APIResetTemplateConfigMsg extends APIMessage {
    @APIParam
    private String templateUuid;

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }

    public static APIResetTemplateConfigMsg __example__() {
        APIResetTemplateConfigMsg msg = new APIResetTemplateConfigMsg();
        return msg;
    }
}
