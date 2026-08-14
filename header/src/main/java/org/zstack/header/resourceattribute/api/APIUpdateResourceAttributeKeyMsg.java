package org.zstack.header.resourceattribute.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.resourceattribute.ResourceAttributeMessage;
import org.zstack.header.resourceattribute.entity.ResourceAttributeConstraintParam;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

@RestRequest(
        path = "/resource-attributes/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateResourceAttributeKeyEvent.class
)
public class APIUpdateResourceAttributeKeyMsg extends APIMessage implements ResourceAttributeMessage {
    @APIParam(required = true, resourceType = ResourceAttributeKeyVO.class)
    private String uuid;

    @APIParam(required = false, emptyString = false)
    private String name;

    @APIParam(required = false)
    private String description;

    @APIParam(required = false)
    private List<String> resourceTypes;

    @APIParam(required = false)
    private List<ResourceAttributeConstraintParam> createConstraints;

    @APIParam(required = false)
    private List<ResourceAttributeConstraintParam> updateConstraints;

    /**
     * list of ResourceAttributeConstraintVO.id
     */
    @APIParam(required = false)
    private List<Long> deleteConstraintIds;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

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

    public List<ResourceAttributeConstraintParam> getCreateConstraints() {
        return createConstraints;
    }

    public void setCreateConstraints(List<ResourceAttributeConstraintParam> createConstraints) {
        this.createConstraints = createConstraints;
    }

    public List<ResourceAttributeConstraintParam> getUpdateConstraints() {
        return updateConstraints;
    }

    public void setUpdateConstraints(List<ResourceAttributeConstraintParam> updateConstraints) {
        this.updateConstraints = updateConstraints;
    }

    public List<Long> getDeleteConstraintIds() {
        return deleteConstraintIds;
    }

    public void setDeleteConstraintIds(List<Long> deleteConstraintIds) {
        this.deleteConstraintIds = deleteConstraintIds;
    }

    @Override
    public String getKeyUuid() {
        return getUuid();
    }

    public static APIUpdateResourceAttributeKeyMsg __example__() {
        APIUpdateResourceAttributeKeyMsg msg = new APIUpdateResourceAttributeKeyMsg();
        msg.setUuid(uuid(ResourceAttributeKeyVO.class));
        msg.setDescription("test");
        return msg;
    }
}
