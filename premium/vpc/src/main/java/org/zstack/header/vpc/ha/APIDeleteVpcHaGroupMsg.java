package org.zstack.header.vpc.ha;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/vpc/hagroups/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVpcHaGroupEvent.class
)
public class APIDeleteVpcHaGroupMsg extends APIDeleteMessage {
    @APIParam(successIfResourceNotExisting = true, resourceType = VpcHaGroupVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

 
    public static APIDeleteVpcHaGroupMsg __example__() {
        APIDeleteVpcHaGroupMsg msg = new APIDeleteVpcHaGroupMsg();

        msg.setUuid(uuid());

        return msg;
    }
}
