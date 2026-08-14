package org.zstack.guesttools.advanced;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/vm-custom-specifications/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVmCustomSpecificationEvent.class
)
public class APIDeleteVmCustomSpecificationMsg extends APIDeleteMessage {
    @APIParam(resourceType = VmCustomSpecificationVO.class, scope = APIParam.SCOPE_MUST_OWNER, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteVmCustomSpecificationMsg __example__() {
        APIDeleteVmCustomSpecificationMsg msg = new APIDeleteVmCustomSpecificationMsg();
        msg.setUuid(uuid(VmCustomSpecificationVO.class));
        return msg;
    }
}
