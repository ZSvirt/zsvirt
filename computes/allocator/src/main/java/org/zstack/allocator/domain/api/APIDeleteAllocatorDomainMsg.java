package org.zstack.allocator.domain.api;

import org.springframework.http.HttpMethod;
import org.zstack.allocator.domain.entity.AllocatorDomainVO;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestRequest(
        path = "/allocator-domains/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteAllocatorDomainEvent.class
)
public class APIDeleteAllocatorDomainMsg extends APIDeleteMessage implements AllocatorDomainMessage {
    @APIParam(resourceType = AllocatorDomainVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getAllocatorDomainUuid() {
        return uuid;
    }

    @Override
    public List<String> getDeletedResourceUuidList() {
        return list(uuid);
    }

    public static APIDeleteAllocatorDomainMsg __example__() {
        APIDeleteAllocatorDomainMsg msg = new APIDeleteAllocatorDomainMsg();
        msg.setUuid(DocUtils.createFixedUuid(AllocatorDomainVO.class));
        return msg;
    }
}
