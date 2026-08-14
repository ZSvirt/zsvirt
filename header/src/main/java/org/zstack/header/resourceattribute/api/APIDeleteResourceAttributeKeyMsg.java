package org.zstack.header.resourceattribute.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.resourceattribute.ResourceAttributeMessage;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestRequest(
        path = "/resource-attributes/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteResourceAttributeKeyEvent.class
)
public class APIDeleteResourceAttributeKeyMsg extends APIDeleteMessage implements ResourceAttributeMessage {
    @APIParam(resourceType = ResourceAttributeKeyVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getKeyUuid() {
        return getUuid();
    }

    @Override
    public List<String> getDeletedResourceUuidList() {
        return list(getUuid());
    }

    public static APIDeleteResourceAttributeKeyMsg __example__() {
        APIDeleteResourceAttributeKeyMsg msg = new APIDeleteResourceAttributeKeyMsg();
        msg.setUuid(uuid(ResourceAttributeKeyVO.class));
        return msg;
    }
}
