package org.zstack.allocator.domain.api;

import org.springframework.http.HttpMethod;
import org.zstack.allocator.domain.entity.AllocatorDomainInventory;
import org.zstack.allocator.domain.entity.AllocatorDomainVO;
import org.zstack.header.message.DocUtils;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryAllocatorDomainReply.class, inventoryClass = AllocatorDomainInventory.class)
@RestRequest(
        path = "/allocator-domains",
        optionalPaths = {"/allocator-domains/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryAllocatorDomainReply.class
)
public class APIQueryAllocatorDomainMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("uuid=" + DocUtils.createFixedUuid(AllocatorDomainVO.class));
    }
}
