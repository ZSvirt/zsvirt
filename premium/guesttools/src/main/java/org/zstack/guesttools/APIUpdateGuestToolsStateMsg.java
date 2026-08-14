package org.zstack.guesttools;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.header.vm.VmInstanceVO;

@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/guesttools-state",
        isAction = true,
        responseClass = APIUpdateGuestToolsStateReply.class,
        method = HttpMethod.PUT
)
public class APIUpdateGuestToolsStateMsg extends APISyncCallMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    public static APIUpdateGuestToolsStateMsg __example__() {
        APIUpdateGuestToolsStateMsg msg = new APIUpdateGuestToolsStateMsg();
        msg.setVmInstanceUuid(uuid());

        return msg;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
