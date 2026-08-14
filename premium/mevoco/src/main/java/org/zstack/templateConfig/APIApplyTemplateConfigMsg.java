package org.zstack.templateConfig;


import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/template-configurations/{templateUuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIApplyTemplateConfigEvent.class
)
public class APIApplyTemplateConfigMsg extends APIMessage {
    @APIParam
    private String templateUuid;

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }

    public static APIApplyTemplateConfigMsg __example__() {
        APIApplyTemplateConfigMsg msg = new APIApplyTemplateConfigMsg();
        return msg;
    }
}
