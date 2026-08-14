package org.zstack.templateConfig;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/template-configurations/{templateUuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIRevertTemplateConfigEvent.class
)
public class APIRevertTemplateConfigMsg extends APIMessage {
    @APIParam
    private String templateUuid;

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }

    public static APIRevertTemplateConfigMsg __example__() {
        APIRevertTemplateConfigMsg msg = new APIRevertTemplateConfigMsg();
        msg.setTemplateUuid(uuid());
        return msg;
    }
}
