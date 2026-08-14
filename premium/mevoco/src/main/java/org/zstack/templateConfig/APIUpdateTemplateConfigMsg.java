package org.zstack.templateConfig;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/template-configurations/{templateUuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateTemplateConfigEvent.class
)
public class APIUpdateTemplateConfigMsg extends APIMessage {
    @APIParam
    private String templateUuid;
    @APIParam
    private String category;
    @APIParam
    private String name;
    @APIParam
    private String value;

    public String getIdentity() {
        return TemplateConfig.produceIdentity(templateUuid, category, name);
    }
    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static APIUpdateTemplateConfigMsg __example__() {
        APIUpdateTemplateConfigMsg msg = new APIUpdateTemplateConfigMsg();
        msg.setTemplateUuid("0000");
        msg.setCategory("quota");
        msg.setName("scheduler.num");
        msg.setValue("90");
        return msg;
    }
}

