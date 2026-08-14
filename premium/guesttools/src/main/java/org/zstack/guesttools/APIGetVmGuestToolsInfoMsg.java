package org.zstack.guesttools;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.message.Message;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.header.vm.VmInstanceVO;

import java.util.Set;

/**
 * Created by GuoYi on 2019-09-17.
 */
@RestRequest(
        path = "/vm-instances/{uuid}/guest-tools-infos",
        method = HttpMethod.GET,
        responseClass = APIGetVmGuestToolsInfoReply.class
)
public class APIGetVmGuestToolsInfoMsg extends APISyncCallMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;

    @APIParam(required = false)
    private Set<String> debug;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Set<String> getDebug() {
        return debug;
    }

    public void setDebug(Set<String> debug) {
        this.debug = debug;
    }

    @Override
    public String getVmInstanceUuid() {
        return getUuid();
    }

    public static APIGetVmGuestToolsInfoMsg __example__() {
        APIGetVmGuestToolsInfoMsg msg = new APIGetVmGuestToolsInfoMsg();
        msg.setUuid(Message.uuid());
        return msg;
    }
}
