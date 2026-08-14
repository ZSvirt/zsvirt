package org.zstack.header.resourceattribute.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.resourceattribute.ResourceAttributeMessage;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.utils.StringDSL;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestRequest(
        path = "/resource-attributes/{keyUuid}/resources",
        method = HttpMethod.POST,
        responseClass = APICreateResourceAttributeValueEvent.class,
        parameterName = "params"
)
public class APICreateResourceAttributeValueMsg extends APIMessage implements ResourceAttributeMessage {
    @APIParam(resourceType = ResourceAttributeKeyVO.class)
    private String keyUuid;

    @APIParam(nonempty = true, maxLength = 2048)
    private String value;

    @APIParam(nonempty = true)
    private List<String> resourceUuids;

    @Override
    public String getKeyUuid() {
        return keyUuid;
    }

    public void setKeyUuid(String keyUuid) {
        this.keyUuid = keyUuid;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<String> getResourceUuids() {
        return resourceUuids;
    }

    public void setResourceUuids(List<String> resourceUuids) {
        this.resourceUuids = resourceUuids;
    }

    public static APICreateResourceAttributeValueMsg __example__() {
        APICreateResourceAttributeValueMsg msg = new APICreateResourceAttributeValueMsg();
        msg.setKeyUuid(StringDSL.createFixedUuid(ResourceAttributeKeyVO.class));
        msg.setValue("Kinny");
        msg.setResourceUuids(list(StringDSL.createFixedUuid(VmInstanceVO.class)));
        return msg;
    }
}
