package org.zstack.header.cloudformation;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/8/3.
 * preview resources from stack
 */
@RestRequest(
        path = "/cloudformation/stack/preview/resource",
        method = HttpMethod.POST,
        responseClass = APIDecodeStackTemplateReply.class,
        parameterName = "params"
)
public class APIDecodeStackTemplateMsg extends APISyncCallMessage {
    @APIParam(required = false, validValues = {"zstack"})
    private String type = "zstack";
    @APIParam(required = false, maxLength = 4194304)
    private String templateContent;
    @APIParam(required = false, resourceType = StackTemplateVO.class)
    private String uuid;
    @APIParam(required = false, maxLength = 524288)
    private String parameters;
    @APIParam(required = false, maxLength = 524288)
    //See PublishAppVO#preParams
    private String preparameters;

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

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getPreparameters() {
        return preparameters;
    }

    public void setPreparameters(String preparameters) {
        this.preparameters = preparameters;
    }

    public static APIDecodeStackTemplateMsg __example__() {
        APIDecodeStackTemplateMsg msg = new APIDecodeStackTemplateMsg();

        msg.setType("zstack");
        msg.setUuid(uuid());
        msg.setParameters("{  \"imageUuid\": \"8fcfe758a7eb13118d7344a08ff790a5\",  \"instanceOfferingUuid\": \"751f662a32184933aff487f5c6e167a6\",  \"l3NetworkUuid\": \"1245de5c2d28454bb63e60575ec611cb\",  \"DiskOfferingUuid\": \"ad0b4ea4c747401c92a7c990f7375cf1\",  \"PrimaryStorageUuid\": \"06c35e7f42264a74abb5b828367169fe\",  \"HostUuid\": \"9b57690de23f449e99c8f0da311e568e\"}");

        return msg;
    }
}
