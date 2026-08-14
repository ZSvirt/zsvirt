package org.zstack.header.tag;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

/**
 */
@RestRequest(
        path = "/tags/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteTagEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "SystemTagUuidToVmUuidResolver", field = "uuid")
public class APIDeleteTagMsg extends APIDeleteMessage {
    @APIParam
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
 
    public static APIDeleteTagMsg __example__() {
        APIDeleteTagMsg msg = new APIDeleteTagMsg();
        msg.setUuid(uuid());
        return msg;
    }

}
