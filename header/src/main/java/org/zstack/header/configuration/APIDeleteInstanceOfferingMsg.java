package org.zstack.header.configuration;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/instance-offerings/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteInstanceOfferingEvent.class
)
public class APIDeleteInstanceOfferingMsg extends APIDeleteMessage implements InstanceOfferingMessage {
    @APIParam(resourceType = InstanceOfferingVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public APIDeleteInstanceOfferingMsg() {
    }

    public APIDeleteInstanceOfferingMsg(String uuid) {
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
    public String getInstanceOfferingUuid() {
        return uuid;
    }
 
    public static APIDeleteInstanceOfferingMsg __example__() {
        APIDeleteInstanceOfferingMsg msg = new APIDeleteInstanceOfferingMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
