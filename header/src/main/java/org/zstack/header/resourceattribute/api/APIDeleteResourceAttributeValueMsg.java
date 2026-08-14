package org.zstack.header.resourceattribute.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.resourceattribute.ResourceAttributeMessage;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestRequest(
        path = "/resource-attributes/{keyUuid}/resources",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteResourceAttributeValueEvent.class
)
public class APIDeleteResourceAttributeValueMsg extends APIMessage implements ResourceAttributeMessage {
    @APIParam(resourceType = ResourceAttributeKeyVO.class)
    private String keyUuid;

    @APIParam(nonempty = true)
    private List<String> resourceUuids;

    @Override
    public String getKeyUuid() {
        return keyUuid;
    }

    public void setKeyUuid(String keyUuid) {
        this.keyUuid = keyUuid;
    }

    public List<String> getResourceUuids() {
        return resourceUuids;
    }

    public void setResourceUuids(List<String> resourceUuids) {
        this.resourceUuids = resourceUuids;
    }

    public static APIDeleteResourceAttributeValueMsg __example__() {
        APIDeleteResourceAttributeValueMsg msg = new APIDeleteResourceAttributeValueMsg();
        msg.setKeyUuid(uuid(ResourceAttributeKeyVO.class));
        msg.setResourceUuids(list(uuid(VmInstanceVO.class)));
        return msg;
    }
}
