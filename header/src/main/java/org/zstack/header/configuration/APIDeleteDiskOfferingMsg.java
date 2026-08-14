package org.zstack.header.configuration;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/disk-offerings/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteDiskOfferingEvent.class
)
public class APIDeleteDiskOfferingMsg extends APIDeleteMessage implements DiskOfferingMessage {
    @APIParam(resourceType = DiskOfferingVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public APIDeleteDiskOfferingMsg() {
    }

    public APIDeleteDiskOfferingMsg(String uuid) {
        super();
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getDiskOfferingUuid() {
        return uuid;
    }
 
    public static APIDeleteDiskOfferingMsg __example__() {
        APIDeleteDiskOfferingMsg msg = new APIDeleteDiskOfferingMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
