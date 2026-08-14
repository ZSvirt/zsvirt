package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetVmCleanTrafficEvent.class
)
public class APISetVmCleanTrafficMsg extends APIMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;

    @APIParam
    private boolean enable;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public static APISetVmCleanTrafficMsg __example__() {
        APISetVmCleanTrafficMsg msg = new APISetVmCleanTrafficMsg();
        msg.setUuid(uuid(VmInstanceVO.class));
        msg.setEnable(true);
        return msg;
    }
}
