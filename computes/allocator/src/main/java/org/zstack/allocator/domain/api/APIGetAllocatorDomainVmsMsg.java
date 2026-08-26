package org.zstack.allocator.domain.api;

import org.springframework.http.HttpMethod;
import org.zstack.allocator.domain.entity.AllocatorDomainVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/allocator-domains/{uuid}/vms",
        method = HttpMethod.GET,
        responseClass = APIGetAllocatorDomainVmsReply.class
)
public class APIGetAllocatorDomainVmsMsg extends APISyncCallMessage implements AllocatorDomainMessage {
    @APIParam(resourceType = AllocatorDomainVO.class)
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

    public static APIGetAllocatorDomainVmsMsg __example__() {
        APIGetAllocatorDomainVmsMsg msg = new APIGetAllocatorDomainVmsMsg();
        msg.setUuid(DocUtils.createFixedUuid(AllocatorDomainVO.class));
        return msg;
    }
}
