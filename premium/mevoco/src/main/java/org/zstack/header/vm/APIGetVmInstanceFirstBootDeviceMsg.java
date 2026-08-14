package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 2019-09-28.
 */
@RestRequest(
        path = "/vm-instances/{uuid}/first-boot-device",
        method = HttpMethod.GET,
        responseClass = APIGetVmInstanceFirstBootDeviceReply.class
)
public class APIGetVmInstanceFirstBootDeviceMsg extends APISyncCallMessage implements VmInstanceMessage {
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

    public static APIGetVmInstanceFirstBootDeviceMsg __example__() {
        APIGetVmInstanceFirstBootDeviceMsg msg = new APIGetVmInstanceFirstBootDeviceMsg();
        msg.uuid = uuid();
        return msg;
    }
}
