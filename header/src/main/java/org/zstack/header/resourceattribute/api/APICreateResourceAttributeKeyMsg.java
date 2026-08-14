package org.zstack.header.resourceattribute.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.resourceattribute.entity.ResourceAttributeConstraintParam;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

import java.util.List;
import java.util.concurrent.TimeUnit;

@TagResourceType(ResourceAttributeKeyVO.class)
@RestRequest(
        path = "/resource-attributes",
        method = HttpMethod.POST,
        responseClass = APICreateResourceAttributeKeyEvent.class,
        parameterName = "params"
)
@DefaultTimeout(timeunit = TimeUnit.SECONDS, value = 5)
public class APICreateResourceAttributeKeyMsg extends APICreateMessage {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(required = false)
    private String description;

    @APIParam(required = false)
    private List<String> resourceTypes;

    @APIParam(required = false)
    private List<ResourceAttributeConstraintParam> constraints;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getResourceTypes() {
        return resourceTypes;
    }

    public void setResourceTypes(List<String> resourceTypes) {
        this.resourceTypes = resourceTypes;
    }

    public List<ResourceAttributeConstraintParam> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<ResourceAttributeConstraintParam> constraints) {
        this.constraints = constraints;
    }

    public static APICreateResourceAttributeKeyMsg __example__() {
        APICreateResourceAttributeKeyMsg msg = new APICreateResourceAttributeKeyMsg();
        msg.setName("Operations personnel");
        return msg;
    }
}
