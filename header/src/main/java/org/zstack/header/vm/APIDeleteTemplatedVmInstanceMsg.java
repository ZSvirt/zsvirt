package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/vm-instances/templatedVmInstance/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteTemplatedVmInstanceEvent.class
)
public class APIDeleteTemplatedVmInstanceMsg extends APIDeleteMessage implements VmInstanceMessage {
    @APIParam(resourceType = TemplatedVmInstanceVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return getUuid();
    }

    public static APIDeleteTemplatedVmInstanceMsg __example__() {
        APIDeleteTemplatedVmInstanceMsg msg = new APIDeleteTemplatedVmInstanceMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
