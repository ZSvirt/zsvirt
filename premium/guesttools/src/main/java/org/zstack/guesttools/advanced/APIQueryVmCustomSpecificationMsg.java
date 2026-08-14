package org.zstack.guesttools.advanced;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

@AutoQuery(replyClass = APIQueryVmCustomSpecificationReply.class, inventoryClass = VmCustomSpecificationInventory.class)
@RestRequest(
        path = "/vm-custom-specifications",
        optionalPaths = "/vm-custom-specifications/{uuid}",
        responseClass = APIQueryVmCustomSpecificationReply.class,
        method = HttpMethod.GET
)
public class APIQueryVmCustomSpecificationMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid(VmCustomSpecificationVO.class));
    }
}
