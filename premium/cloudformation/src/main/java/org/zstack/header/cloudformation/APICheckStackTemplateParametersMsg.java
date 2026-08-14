package org.zstack.header.cloudformation;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/6/20.
 */
@RestRequest(
        path = "/cloudformation/stack/check",
        method = HttpMethod.POST,
        responseClass = APICheckStackTemplateParametersReply.class,
        parameterName = "params"
)
public class APICheckStackTemplateParametersMsg extends APISyncCallMessage {
    @APIParam(required = false, validValues = {"zstack"})
    private String type = "zstack";
    @APIParam(required = false, maxLength = 4194304)
    private String templateContent;
    @APIParam(required = false, resourceType = StackTemplateVO.class)
    private String uuid;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public void setTemplateContent(String templateContent) {
        this.templateContent = templateContent;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APICheckStackTemplateParametersMsg __example__() {
        APICheckStackTemplateParametersMsg msg = new APICheckStackTemplateParametersMsg();

        msg.setUuid(uuid());

        return msg;
    }
}
