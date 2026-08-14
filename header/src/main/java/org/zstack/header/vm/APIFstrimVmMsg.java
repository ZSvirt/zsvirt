package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        responseClass = APIFstrimVmEvent.class,
        method = HttpMethod.POST,
        parameterName = "params"
)
public class APIFstrimVmMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;

    @APINoSee
    private String hostUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public static APIFstrimVmMsg __example__() {
        APIFstrimVmMsg msg = new APIFstrimVmMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
