package org.zstack.guesttools;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.message.Message;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Created by GuoYi on 2019-09-19.
 */
@RestRequest(
        path = "/vm-instances/{uuid}/latest-guest-tools",
        method = HttpMethod.GET,
        responseClass = APIGetLatestGuestToolsForVmReply.class
)
public class APIGetLatestGuestToolsForVmMsg extends APISyncCallMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
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

    public static APIGetLatestGuestToolsForVmMsg __example__() {
        APIGetLatestGuestToolsForVmMsg msg = new APIGetLatestGuestToolsForVmMsg();
        msg.setUuid(Message.uuid());
        return msg;
    }
}
