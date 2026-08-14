package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 */
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/data-volume-candidates",
        method = HttpMethod.GET,
        responseClass = APIGetVmAttachableDataVolumeReply.class
)
public class APIGetVmAttachableDataVolumeMsg extends APISyncCallMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
 
    public static APIGetVmAttachableDataVolumeMsg __example__() {
        APIGetVmAttachableDataVolumeMsg msg = new APIGetVmAttachableDataVolumeMsg();
        msg.vmInstanceUuid = uuid();
        return msg;
    }

}
