package org.zstack.header.configuration;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@AutoQuery(replyClass = APIQueryInstanceOfferingReply.class, inventoryClass = InstanceOfferingInventory.class)
@RestRequest(
        path = "/instance-offerings",
        optionalPaths = "/instance-offerings/{uuid}",
        responseClass = APIQueryInstanceOfferingReply.class,
        method = HttpMethod.GET
)
public class APIQueryInstanceOfferingMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return list("uuid=" + uuid(InstanceOfferingVO.class));
    }
}
