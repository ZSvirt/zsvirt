package org.zstack.header.tag;

import org.springframework.http.HttpMethod;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

/**
 */
@RestRequest(
        path = "/system-tags",
        method = HttpMethod.POST,
        responseClass = APICreateSystemTagEvent.class,
        parameterName = "params"
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "ResourceUuidToVmUuidResolver", field = "resourceUuid")
public class APICreateSystemTagMsg extends APIAbstractCreateTagMsg {
 
    public static APICreateSystemTagMsg __example__() {
        APICreateSystemTagMsg msg = new APICreateSystemTagMsg();
        msg.setResourceType("HostVO");
        msg.setResourceUuid(uuid());
        msg.setTag("reservedMemory::1G");
        return msg;
    }

}
