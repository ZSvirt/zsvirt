package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 7/19/2015.
 */
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/l3-networks-candidates",
        method = HttpMethod.GET,
        responseClass = APIGetVmAttachableL3NetworkReply.class
)
public class APIGetVmAttachableL3NetworkMsg extends APISyncCallMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
 
    public static APIGetVmAttachableL3NetworkMsg __example__() {
        APIGetVmAttachableL3NetworkMsg msg = new APIGetVmAttachableL3NetworkMsg();
        msg.vmInstanceUuid = uuid();
        return msg;
    }

}
