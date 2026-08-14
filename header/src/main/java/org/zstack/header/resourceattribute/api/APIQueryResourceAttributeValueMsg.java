package org.zstack.header.resourceattribute.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO;
import org.zstack.header.resourceattribute.entity.ResourceAttributeValueInventory;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@AutoQuery(replyClass = APIQueryResourceAttributeValueReply.class, inventoryClass = ResourceAttributeValueInventory.class)
@RestRequest(
        path = "/resource-attributes",
        responseClass = APIQueryResourceAttributeValueReply.class,
        method = HttpMethod.GET
)
public class APIQueryResourceAttributeValueMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return list("keyUuid=" + uuid(ResourceAttributeKeyVO.class));
    }
}
