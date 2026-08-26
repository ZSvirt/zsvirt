package org.zstack.allocator.domain.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;

@RestRequest(
        path = "/allocator-domains/candidates",
        method = HttpMethod.GET,
        responseClass = APIGetAllocatorDomainCandidatesReply.class
)
public class APIGetAllocatorDomainCandidatesMsg extends APISyncCallMessage {
    @APIParam(resourceType = ZoneVO.class)
    private String zoneUuid;

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public static APIGetAllocatorDomainCandidatesMsg __example__() {
        APIGetAllocatorDomainCandidatesMsg msg = new APIGetAllocatorDomainCandidatesMsg();
        msg.setZoneUuid(DocUtils.createFixedUuid(ZoneVO.class));
        return msg;
    }
}
